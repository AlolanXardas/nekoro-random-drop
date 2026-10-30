package dev.xardas.nekororandomdrop;

import java.io.IOException;
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

/** Reads config/nekoro_random_drop.json. Comments are allowed in the file. */
public final class RuleConfig {
    public static final String FILE_NAME = RandomDrop.MOD_ID + ".json";

    private static Path configDir = Path.of("config");

    private static List<Rule> all = List.of();
    private static Map<Rule.Source, List<Rule>> bySource = new EnumMap<>(Rule.Source.class);

    public record Result(int loaded, int skipped) { }

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

    /** Reads the file and makes its rules active. */
    public static Result load() {
        List<Rule> loaded = new ArrayList<>();
        int skipped = read(loaded);
        Map<Rule.Source, List<Rule>> grouped = new EnumMap<>(Rule.Source.class);
        for (Rule rule : loaded) grouped.computeIfAbsent(rule.source(), s -> new ArrayList<>()).add(rule);
        all = List.copyOf(loaded);
        bySource = grouped;
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

    /** Fills {@code loaded} from the file, creating it first if missing; returns how many rules were skipped. */
    private static int read(List<Rule> loaded) {
        Path path = configDir.resolve(FILE_NAME);
        int skipped = 0;
        try {
            if (Files.notExists(path)) Files.writeString(path, DEFAULT_CONFIG, StandardCharsets.UTF_8);
            JsonArray rules;
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonReader json = new JsonReader(reader);
                json.setLenient(true);
                rules = JsonParser.parseReader(json).getAsJsonObject().getAsJsonArray("rules");
            }
            if (rules != null) {
                for (JsonElement element : rules) {
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
        return skipped;
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

        return new Rule(index, source, ids, tags, types, item,
                o.has("count") ? Math.max(1, o.get("count").getAsInt()) : 1,
                o.has("chance") ? o.get("chance").getAsDouble() : 1.0,
                xp,
                Rule.Bonus.valueOf(string(o, "bonus", "multiply").toUpperCase(Locale.ROOT)),
                o.has("silk_touch") && o.get("silk_touch").getAsBoolean(),
                !o.has("player_only") || o.get("player_only").getAsBoolean(),
                o.has("to_inventory") && o.get("to_inventory").getAsBoolean(),
                o.has("cooldown") ? Math.max(0, o.get("cooldown").getAsInt()) : 0,
                minY, maxY, dimensions, biomeIds, biomeTags);
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

    private static final String DEFAULT_CONFIG = """
            // Nekoro Random Drop: extra drops from blocks, mobs and (with Cobblemon) Pokemon.
            // Every rule is rolled on its own. After editing, use /randomdrop reload in game.
            //
            // "source"       "block", "mob", "pokemon_kill" (killed or defeated in battle) or "pokemon_catch"
            // "match"        block or entity id, or a tag starting with #. A list works too: ["minecraft:stone", "#c:ores"]
            //                Pokemon rules: species ids, optional (for example "cobblemon:pikachu")
            // "type"         Pokemon rules only, optional: "fire" or ["fire", "water"]
            // "item"         item to give (can be left out if the rule only gives xp)
            // "count"        how many items, default 1
            // "chance"       0.0 to 1.0, default 1.0 (always). 0.005 is 0.5%
            // "xp"           experience points, default 0
            // "bonus"        how Fortune (blocks) or Looting (mobs) helps, default "multiply":
            //                "multiply" like vanilla ores, works with any enchantment level
            //                "chance"   chance x (level + 1)
            //                "none"
            // "silk_touch"   blocks only: still drop when mined with Silk Touch, default false
            // "player_only"  only when a player mines the block or kills the mob, default true
            // "to_inventory" put the reward straight into the player's inventory, default false (dropped on the ground)
            // "cooldown"     ticks before the same player can get this rule again, default 0 (20 ticks = 1 second)
            //
            // Where it works (all optional, checked at the mined block, the dead mob or the player who caught the Pokemon):
            // "min_y"        lowest Y level, included. "max_y": highest Y level, included
            // "dimension"    dimension id or a list: "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"
            // "biome"        biome id or a biome tag starting with #, a list works too: ["minecraft:lush_caves", "#c:is_ocean"]
            //
            // The list below is empty, so nothing extra drops until you add your own rules.
            // Remove the // in front of a rule to use it.
            {
              "rules": [
                // Simple examples:
                // { "source": "block", "match": "#c:stones", "item": "minecraft:flint", "chance": 0.02 },
                // { "source": "mob", "match": "minecraft:zombie", "item": "minecraft:emerald", "chance": 0.05, "bonus": "chance" },
                // { "source": "pokemon_kill", "type": "fire", "item": "minecraft:blaze_powder", "chance": 0.1 },
                // { "source": "block", "match": "minecraft:deepslate", "item": "minecraft:diamond", "chance": 0.002, "max_y": -40 },
                // { "source": "block", "match": "#c:stones", "item": "minecraft:emerald", "chance": 0.01, "biome": "#minecraft:is_mountain" },
                // { "source": "block", "match": "minecraft:netherrack", "item": "minecraft:quartz", "chance": 0.05, "dimension": "minecraft:the_nether", "min_y": 100 },
                //
                // What the Nekoro Pokemon modpack uses (needs the Nekoro Coins mod):
                // mined stone has an 11% chance to give a coin and xp straight to the inventory
                // { "source": "block", "match": "minecraft:stone", "item": "nekorocoins:coin116", "xp": 11, "chance": 0.11, "to_inventory": true },
                // { "source": "block", "match": "minecraft:deepslate", "item": "nekorocoins:coin116", "count": 2, "xp": 22, "chance": 0.11, "to_inventory": true },
                // a 0.5% chance of a type coin for every defeated or caught Pokemon, one roll per type,
                // so a Fire/Flying Pokemon rolls for both coins (the pack has a pair like this for all 18 types)
                // { "source": "pokemon_kill", "type": "fire", "item": "nekorocoins:coin041", "chance": 0.005 },
                // { "source": "pokemon_catch", "type": "fire", "item": "nekorocoins:coin041", "chance": 0.005, "to_inventory": true }
              ]
            }
            """;
}
