package dev.xardas.nekororandomdrop.compat;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Minecraft 1.21.1: enchantments are data-driven, resource locations are parsed with ResourceLocation.parse. */
public final class VersionCompat {
    private static Map<EntityType<?>, Item> spawnEggs;

    private VersionCompat() { }

    public static ResourceLocation id(String text) {
        return ResourceLocation.parse(text);
    }

    public static int fortune(ServerLevel level, ItemStack tool) {
        return enchantment(level, Enchantments.FORTUNE, tool);
    }

    public static int silkTouch(ServerLevel level, ItemStack tool) {
        return enchantment(level, Enchantments.SILK_TOUCH, tool);
    }

    public static int looting(ServerLevel level, DamageSource source) {
        return enchantment(level, Enchantments.LOOTING, source.getWeaponItem());
    }

    private static int enchantment(ServerLevel level, ResourceKey<Enchantment> key, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
                .orElse(0);
    }

    /** Any spawn egg item for this entity, modded ones included. */
    public static Item spawnEgg(EntityType<?> type) {
        if (spawnEggs == null) {
            Map<EntityType<?>, Item> eggs = new HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof SpawnEggItem egg) eggs.putIfAbsent(egg.getType(new ItemStack(item)), item);
            }
            spawnEggs = eggs;
        }
        return spawnEggs.get(type);
    }
}
