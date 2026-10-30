package dev.xardas.nekororandomdrop.compat;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Minecraft 1.20.1: enchantments are plain objects, the weapon comes from the killer's main hand. */
public final class VersionCompat {
    private static Map<EntityType<?>, Item> spawnEggs;

    private VersionCompat() { }

    public static ResourceLocation id(String text) {
        return new ResourceLocation(text);
    }

    public static int fortune(ServerLevel level, ItemStack tool) {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool);
    }

    public static int silkTouch(ServerLevel level, ItemStack tool) {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool);
    }

    public static int looting(ServerLevel level, DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity killer)) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, killer.getMainHandItem());
    }

    /** Any spawn egg item for this entity, modded ones included. */
    public static Item spawnEgg(EntityType<?> type) {
        if (spawnEggs == null) {
            Map<EntityType<?>, Item> eggs = new HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof SpawnEggItem egg) eggs.putIfAbsent(egg.getType(null), item);
            }
            spawnEggs = eggs;
        }
        return spawnEggs.get(type);
    }
}
