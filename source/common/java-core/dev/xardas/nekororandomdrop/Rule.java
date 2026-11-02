package dev.xardas.nekororandomdrop;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

/** One line of the config: where it triggers, what it gives and how often. */
public record Rule(
        int index,
        Source source,
        Set<ResourceLocation> ids,
        List<ResourceLocation> tags,
        Set<String> types,
        Item item,
        int count,
        double chance,
        int xp,
        Bonus bonus,
        SilkTouch silkTouch,
        boolean playerOnly,
        boolean toInventory,
        int cooldown,
        int minY,
        int maxY,
        Set<ResourceLocation> dimensions,
        Set<ResourceLocation> biomeIds,
        List<ResourceLocation> biomeTags,
        String minTool) {

    /** Tool tiers for "min_tool", lowest first. */
    public static final List<String> TOOL_TIERS = List.of("wood", "stone", "iron", "diamond", "netherite");

    public enum Source { BLOCK, MOB, POKEMON_KILL, POKEMON_CATCH }

    /** Height, dimension and biome of the mined block, the dead mob or the player who caught a Pokemon. */
    public boolean matchesPlace(ServerLevel level, BlockPos pos) {
        if (pos.getY() < minY || pos.getY() > maxY) return false;
        if (!dimensions.isEmpty() && !dimensions.contains(level.dimension().location())) return false;
        if (biomeIds.isEmpty() && biomeTags.isEmpty()) return true;
        // biomes can change with height too, so this checks the exact block
        Holder<Biome> biome = level.getBiome(pos);
        for (ResourceLocation id : biomeIds) {
            if (biome.is(id)) return true;
        }
        for (ResourceLocation tag : biomeTags) {
            if (biome.is(TagKey.create(Registries.BIOME, tag))) return true;
        }
        return false;
    }

    public boolean hasMinY() {
        return minY != Integer.MIN_VALUE;
    }

    public boolean hasMaxY() {
        return maxY != Integer.MAX_VALUE;
    }

    /** How Fortune (blocks) or Looting (mobs) changes the reward. */
    public enum Bonus { MULTIPLY, CHANCE, NONE }

    /** Blocks mined with Silk Touch: no drop, a normal drop, or the chance goes up with the Silk Touch level. */
    public enum SilkTouch { NO, YES, BONUS }

    public boolean matchesBlock(BlockState state) {
        if (ids.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) return true;
        for (ResourceLocation tag : tags) {
            if (state.is(TagKey.create(Registries.BLOCK, tag))) return true;
        }
        return false;
    }

    public boolean matchesEntity(EntityType<?> type) {
        if (ids.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type))) return true;
        for (ResourceLocation tag : tags) {
            if (type.is(TagKey.create(Registries.ENTITY_TYPE, tag))) return true;
        }
        return false;
    }

    /** Pokemon rules: no species and no type given means every Pokemon. */
    public boolean matchesPokemon(ResourceLocation species, Iterable<String> pokemonTypes) {
        if (!ids.isEmpty() && !ids.contains(species)) return false;
        if (types.isEmpty()) return true;
        for (String type : pokemonTypes) {
            if (types.contains(type.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    public String describe() {
        StringBuilder sb = new StringBuilder("#").append(index + 1).append(' ').append(source.name().toLowerCase(Locale.ROOT));
        if (!ids.isEmpty()) sb.append(' ').append(ids);
        if (!tags.isEmpty()) sb.append(" #").append(tags);
        if (!types.isEmpty()) sb.append(" type ").append(types);
        sb.append(" -> ");
        if (item != null) sb.append(count).append("x ").append(BuiltInRegistries.ITEM.getKey(item));
        if (xp > 0) sb.append(item != null ? " + " : "").append(xp).append(" xp");
        sb.append(String.format(Locale.ROOT, " (%s%%)", trim(chance * 100)));
        if (cooldown > 0) sb.append(" cooldown ").append(cooldown);
        if (minTool != null) sb.append(" needs ").append(minTool).append(" tool");
        if (hasMinY() && hasMaxY()) sb.append(" y ").append(minY).append("..").append(maxY);
        else if (hasMinY()) sb.append(" y >= ").append(minY);
        else if (hasMaxY()) sb.append(" y <= ").append(maxY);
        if (!dimensions.isEmpty()) sb.append(" in ").append(dimensions);
        if (!biomeIds.isEmpty() || !biomeTags.isEmpty()) {
            List<String> biomes = new ArrayList<>();
            biomeIds.forEach(id -> biomes.add(id.toString()));
            biomeTags.forEach(tag -> biomes.add("#" + tag));
            sb.append(" biome ").append(biomes);
        }
        return sb.toString();
    }

    private static String trim(double value) {
        String s = String.format(Locale.ROOT, "%.4f", value);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
