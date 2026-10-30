package dev.xardas.nekororandomdrop;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import dev.xardas.nekororandomdrop.compat.VersionCompat;

/** One rule, prepared for the JEI tab: what it comes from, what it gives and the text under it. */
public record DropDisplay(List<ItemStack> sources, ItemStack sourceIcon, ItemStack output,
                          Component chance, List<Component> lines) {

    private static final String KEY = "jei.nekoro_random_drop.";

    public static DropDisplay of(Rule rule) {
        List<ItemStack> sources = new ArrayList<>();
        ItemStack icon = null;
        List<Component> lines = new ArrayList<>();
        switch (rule.source()) {
            case BLOCK -> {
                for (Block block : blocks(rule)) addItem(sources, block.asItem());
                lines.add(Component.translatable(KEY + "source.block", where(rule)));
            }
            case MOB -> {
                for (EntityType<?> type : entities(rule)) addItem(sources, spawnEgg(type));
                lines.add(Component.translatable(KEY + "source.mob", where(rule)));
            }
            case POKEMON_KILL, POKEMON_CATCH -> {
                icon = BuiltInRegistries.ITEM.getOptional(VersionCompat.id("cobblemon:poke_ball")).map(ItemStack::new).orElse(null);
                String kind = rule.source() == Rule.Source.POKEMON_KILL ? "defeated" : "caught";
                lines.add(Component.translatable(KEY + "source.pokemon_" + kind, pokemon(rule)));
            }
        }

        Component place = place(rule);
        if (place != null) lines.add(place);

        List<String> notes = new ArrayList<>();
        boolean block = rule.source() == Rule.Source.BLOCK, mob = rule.source() == Rule.Source.MOB;
        if ((block || mob) && rule.bonus() != Rule.Bonus.NONE) {
            notes.add(Component.translatable(KEY + "note." + (block ? "fortune_" : "looting_") + rule.bonus().name().toLowerCase(Locale.ROOT)).getString());
        }
        if (block) notes.add(Component.translatable(KEY + (rule.silkTouch() ? "note.silk_touch" : "note.no_silk_touch")).getString());
        if (!rule.playerOnly()) notes.add(Component.translatable(KEY + "note.any_cause").getString());
        if (rule.toInventory()) notes.add(Component.translatable(KEY + "note.to_inventory").getString());
        if (rule.cooldown() > 0) notes.add(Component.translatable(KEY + "note.cooldown", seconds(rule.cooldown())).getString());
        if (!notes.isEmpty()) {
            String text = String.join(", ", notes);
            lines.add(Component.literal(Character.toUpperCase(text.charAt(0)) + text.substring(1)));
        }

        Component chance = rule.chance() >= 1.0
                ? Component.translatable(KEY + "always")
                : Component.translatable(KEY + "chance", percent(rule.chance()));
        if (rule.xp() > 0) chance = Component.translatable(KEY + "with_xp", chance, rule.xp());

        ItemStack output = rule.item() == null ? null : new ItemStack(rule.item(), rule.count());
        return new DropDisplay(sources, icon, output, chance, lines);
    }

    private static Set<Block> blocks(Rule rule) {
        Set<Block> out = new LinkedHashSet<>();
        for (ResourceLocation id : rule.ids()) BuiltInRegistries.BLOCK.getOptional(id).ifPresent(out::add);
        for (ResourceLocation tag : rule.tags()) {
            BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, tag))
                    .ifPresent(set -> set.forEach(holder -> out.add(holder.value())));
        }
        return out;
    }

    private static Set<EntityType<?>> entities(Rule rule) {
        Set<EntityType<?>> out = new LinkedHashSet<>();
        for (ResourceLocation id : rule.ids()) BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(out::add);
        for (ResourceLocation tag : rule.tags()) {
            BuiltInRegistries.ENTITY_TYPE.getTag(TagKey.create(Registries.ENTITY_TYPE, tag))
                    .ifPresent(set -> set.forEach((Holder<EntityType<?>> holder) -> out.add(holder.value())));
        }
        return out;
    }

    private static Item spawnEgg(EntityType<?> type) {
        return VersionCompat.spawnEgg(type);
    }

    private static void addItem(List<ItemStack> list, Item item) {
        if (item != null && item != Items.AIR) list.add(new ItemStack(item));
    }

    /** "Stone" for one id, "#c:stones" for tags, names joined with commas. */
    private static String where(Rule rule) {
        List<String> parts = new ArrayList<>();
        for (ResourceLocation id : rule.ids()) {
            if (rule.source() == Rule.Source.BLOCK) {
                parts.add(BuiltInRegistries.BLOCK.getOptional(id).map(b -> b.getName().getString()).orElse(id.toString()));
            } else {
                parts.add(BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(t -> t.getDescription().getString()).orElse(id.toString()));
            }
        }
        for (ResourceLocation tag : rule.tags()) parts.add("#" + tag);
        return String.join(", ", parts);
    }

    /** "Fire", "Fire or Water", species names, or "any" for rules without filters. */
    private static String pokemon(Rule rule) {
        List<String> parts = new ArrayList<>();
        for (String type : rule.types()) parts.add(capitalize(type));
        for (ResourceLocation species : rule.ids()) parts.add(capitalize(species.getPath()));
        return parts.isEmpty() ? Component.translatable(KEY + "any").getString() : String.join(" / ", parts);
    }

    /** "Y 0 or lower, in The Nether, biome: Lush Caves", or null when the rule works everywhere. */
    private static Component place(Rule rule) {
        List<String> parts = new ArrayList<>();
        if (rule.hasMinY() && rule.hasMaxY()) {
            parts.add(Component.translatable(KEY + "place.y_range", rule.minY(), rule.maxY()).getString());
        } else if (rule.hasMinY()) {
            parts.add(Component.translatable(KEY + "place.y_above", rule.minY()).getString());
        } else if (rule.hasMaxY()) {
            parts.add(Component.translatable(KEY + "place.y_below", rule.maxY()).getString());
        }
        if (!rule.dimensions().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (ResourceLocation id : rule.dimensions()) names.add(titleCase(id.getPath()));
            parts.add(Component.translatable(KEY + "place.dimension", String.join(", ", names)).getString());
        }
        if (!rule.biomeIds().isEmpty() || !rule.biomeTags().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (ResourceLocation id : rule.biomeIds()) {
                String key = "biome." + id.getNamespace() + "." + id.getPath();
                names.add(Language.getInstance().has(key) ? Component.translatable(key).getString() : titleCase(id.getPath()));
            }
            for (ResourceLocation tag : rule.biomeTags()) names.add("#" + tag);
            parts.add(Component.translatable(KEY + "place.biome", String.join(", ", names)).getString());
        }
        if (parts.isEmpty()) return null;
        String text = String.join(", ", parts);
        return Component.literal(Character.toUpperCase(text.charAt(0)) + text.substring(1));
    }

    /** "the_nether" -> "The Nether" */
    private static String titleCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (String word : s.split("_")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1).replace('_', ' ');
    }

    private static String percent(double chance) {
        return trim(chance * 100) + "%";
    }

    private static String seconds(int ticks) {
        return trim(ticks / 20.0) + " s";
    }

    private static String trim(double value) {
        String s = String.format(Locale.ROOT, "%.4f", value);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
