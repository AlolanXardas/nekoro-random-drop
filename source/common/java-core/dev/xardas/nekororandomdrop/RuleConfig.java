package dev.xardas.nekororandomdrop;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import dev.xardas.nekororandomdrop.compat.VersionCompat;

/** Reads config/Nekoro/random_drop.json (config/Nekoro holds the configs of all Nekoro mods). Comments are allowed. */
public final class RuleConfig {
    public static final String FILE_NAME = "random_drop.json";
    /** Where release 1 and 2 kept the file; moved to the new place on first start. */
    private static final String OLD_FILE_NAME = "nekoro_random_drop.json";

    private static Path configDir = Path.of("config");

    private static List<Rule> all = List.of();
    private static Map<Rule.Source, List<Rule>> bySource = new EnumMap<>(Rule.Source.class);
    private static double silkTouchBonusPerLevel = 1;

    public record Result(int loaded, int skipped) { }

    private record Read(int skipped, double silkTouchBonusPerLevel) { }

    private RuleConfig() { }

    /** Called by the loader entry point with its config folder. */
    public static void init(Path dir) {
        configDir = dir;
    }

    public static List<Rule> all() {
        return all;
    }

    public static List<Rule> rules(Rule.Source source) {
        return bySource.getOrDefault(source, List.of());
    }

    /** Chance factor of a "silk_touch": "bonus" rule: level I is 1, every level above adds silk_touch_bonus_per_level. */
    public static double silkTouchFactor(int level) {
        return level <= 1 ? 1 : 1 + silkTouchBonusPerLevel * (level - 1);
    }

    /** Reads the file and makes its rules active. */
    public static Result load() {
        List<Rule> loaded = new ArrayList<>();
        Read read = read(loaded);
        int skipped = read.skipped();
        Map<Rule.Source, List<Rule>> grouped = new EnumMap<>(Rule.Source.class);
        for (Rule rule : loaded) grouped.computeIfAbsent(rule.source(), s -> new ArrayList<>()).add(rule);
        all = List.copyOf(loaded);
        bySource = grouped;
        silkTouchBonusPerLevel = read.silkTouchBonusPerLevel();
        Rewards.clearCooldowns();
        RandomDrop.LOGGER.info("Loaded {} random drop rules ({} skipped)", loaded.size(), skipped);
        return new Result(loaded.size(), skipped);
    }

    /** Reads the file without touching the active rules (used by the JEI tab). */
    public static List<Rule> readOnly() {
        List<Rule> rules = new ArrayList<>();
        read(rules);
        return rules;
    }

    /** Fills {@code loaded} from the file, creating it first if missing. */
    private static Read read(List<Rule> loaded) {
        Path path = configDir.resolve("Nekoro").resolve(FILE_NAME);
        int skipped = 0;
        double silkBonus = 1;
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Path old = configDir.resolve(OLD_FILE_NAME);
                if (Files.exists(old)) {
                    Files.move(old, path);
                    RandomDrop.LOGGER.info("Moved config/{} to config/Nekoro/{}", OLD_FILE_NAME, FILE_NAME);
                } else {
                    Files.writeString(path, defaultConfig(), StandardCharsets.UTF_8);
                }
            }
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonReader json = new JsonReader(reader);
                json.setLenient(true);
                root = JsonParser.parseReader(json).getAsJsonObject();
            }
            if (root.has("silk_touch_bonus_per_level")) silkBonus = Math.max(0, root.get("silk_touch_bonus_per_level").getAsDouble());
            JsonArray rules = root.getAsJsonArray("rules");
            if (rules != null) {
                for (JsonElement element : rules) {
                    if (element.isJsonNull()) continue; // a comma after the last rule
                    try {
                        loaded.add(parse(loaded.size(), element.getAsJsonObject()));
                    } catch (RuntimeException e) {
                        skipped++;
                        RandomDrop.LOGGER.warn("Skipping rule {} in {}: {}", loaded.size() + skipped, FILE_NAME, e.getMessage());
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            RandomDrop.LOGGER.error("Could not read {}, no random drops are active", FILE_NAME, e);
        }
        return new Read(skipped, silkBonus);
    }

    /** The file written on first start, kept in the jar so it is easy to read and edit. */
    private static String defaultConfig() throws IOException {
        try (InputStream in = RuleConfig.class.getResourceAsStream("/" + RandomDrop.MOD_ID + "/default_config.json")) {
            if (in == null) throw new IOException("default_config.json is missing from the jar");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Rule parse(int index, JsonObject o) {
        Rule.Source source = Rule.Source.valueOf(string(o, "source", null).toUpperCase(Locale.ROOT));
        Set<ResourceLocation> ids = new HashSet<>();
        List<ResourceLocation> tags = new ArrayList<>();
        for (String m : strings(o, "match")) {
            if (m.startsWith("#")) tags.add(VersionCompat.id(m.substring(1)));
            else ids.add(VersionCompat.id(m));
        }
        boolean pokemon = source == Rule.Source.POKEMON_KILL || source == Rule.Source.POKEMON_CATCH;
        if (!pokemon && ids.isEmpty() && tags.isEmpty()) throw new IllegalArgumentException("\"match\" is required for " + source);
        if (pokemon && !tags.isEmpty()) throw new IllegalArgumentException("pokemon rules match species ids, not tags");
        Set<String> types = new HashSet<>();
        for (String t : strings(o, "type")) types.add(t.toLowerCase(Locale.ROOT));

        Item item = null;
        String itemId = string(o, "item", "");
        if (!itemId.isEmpty()) {
            ResourceLocation id = VersionCompat.id(itemId);
            item = BuiltInRegistries.ITEM.getOptional(id).orElseThrow(() -> new IllegalArgumentException("unknown item " + id));
        }
        int xp = o.has("xp") ? o.get("xp").getAsInt() : 0;
        if (item == null && xp <= 0) throw new IllegalArgumentException("rule gives nothing: set \"item\" or \"xp\"");

        int minY = o.has("min_y") ? o.get("min_y").getAsInt() : Integer.MIN_VALUE;
        int maxY = o.has("max_y") ? o.get("max_y").getAsInt() : Integer.MAX_VALUE;
        if (minY > maxY) throw new IllegalArgumentException("\"min_y\" is higher than \"max_y\"");
        Set<ResourceLocation> dimensions = new HashSet<>();
        for (String d : strings(o, "dimension")) dimensions.add(VersionCompat.id(d));
        Set<ResourceLocation> biomeIds = new HashSet<>();
        List<ResourceLocation> biomeTags = new ArrayList<>();
        for (String b : strings(o, "biome")) {
            if (b.startsWith("#")) biomeTags.add(VersionCompat.id(b.substring(1)));
            else biomeIds.add(VersionCompat.id(b));
        }

        String minTool = o.has("min_tool") ? o.get("min_tool").getAsString().toLowerCase(Locale.ROOT) : null;
        if ("gold".equals(minTool)) minTool = "wood";
        if (minTool != null && !Rule.TOOL_TIERS.contains(minTool)) throw new IllegalArgumentException("unknown min_tool " + minTool);

        return new Rule(index, source, ids, tags, types, item,
                o.has("count") ? Math.max(1, o.get("count").getAsInt()) : 1,
                o.has("chance") ? o.get("chance").getAsDouble() : 1.0,
                xp,
                Rule.Bonus.valueOf(string(o, "bonus", "multiply").toUpperCase(Locale.ROOT)),
                silkTouch(o),
                !o.has("player_only") || o.get("player_only").getAsBoolean(),
                o.has("to_inventory") && o.get("to_inventory").getAsBoolean(),
                o.has("cooldown") ? Math.max(0, o.get("cooldown").getAsInt()) : 0,
                minY, maxY, dimensions, biomeIds, biomeTags, minTool);
    }

    /** true, false or "bonus". */
    private static Rule.SilkTouch silkTouch(JsonObject o) {
        if (!o.has("silk_touch")) return Rule.SilkTouch.NO;
        String value = o.get("silk_touch").getAsString().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "true" -> Rule.SilkTouch.YES;
            case "false" -> Rule.SilkTouch.NO;
            case "bonus" -> Rule.SilkTouch.BONUS;
            default -> throw new IllegalArgumentException("\"silk_touch\" must be true, false or \"bonus\", not " + value);
        };
    }

    private static String string(JsonObject o, String key, String fallback) {
        if (!o.has(key)) {
            if (fallback == null) throw new IllegalArgumentException("\"" + key + "\" is required");
            return fallback;
        }
        return o.get(key).getAsString();
    }

    /** A single string or a list of strings. */
    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (!o.has(key)) return out;
        JsonElement e = o.get(key);
        if (e.isJsonArray()) e.getAsJsonArray().forEach(x -> out.add(x.getAsString()));
        else out.add(e.getAsString());
        return out;
    }
}
