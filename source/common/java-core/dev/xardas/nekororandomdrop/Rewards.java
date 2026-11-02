package dev.xardas.nekororandomdrop;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Rolls a rule and hands out the items and xp. */
public final class Rewards {
    private static final Map<UUID, Map<Integer, Integer>> lastReward = new HashMap<>();

    private Rewards() { }

    static void clearCooldowns() {
        lastReward.clear();
    }

    public static void roll(Rule rule, ServerLevel level, Vec3 pos, ServerPlayer player, int bonusLevel,
                            Consumer<ItemEntity> drops) {
        roll(rule, level, pos, player, bonusLevel, 1, drops);
    }

    /**
     * @param bonusLevel   Fortune or Looting level, 0 when it does not apply
     * @param chanceFactor the rule's chance is multiplied by this (Silk Touch "bonus")
     * @param drops        where dropped items go (an event's drop list), or null to spawn them in the world
     */
    public static void roll(Rule rule, ServerLevel level, Vec3 pos, ServerPlayer player, int bonusLevel,
                            double chanceFactor, Consumer<ItemEntity> drops) {
        if (rule.playerOnly() && player == null) return;
        int now = level.getServer().getTickCount();
        if (rule.cooldown() > 0 && player != null) {
            Integer last = lastReward.computeIfAbsent(player.getUUID(), u -> new HashMap<>()).get(rule.index());
            if (last != null && now - last < rule.cooldown()) return;
        }
        RandomSource random = level.getRandom();
        double chance = rule.chance() * chanceFactor;
        if (rule.bonus() == Rule.Bonus.CHANCE) chance *= bonusLevel + 1;
        if (random.nextDouble() >= chance) return;
        if (rule.cooldown() > 0 && player != null) lastReward.get(player.getUUID()).put(rule.index(), now);

        if (rule.item() != null) {
            long count = rule.count();
            if (rule.bonus() == Rule.Bonus.MULTIPLY && bonusLevel > 0) {
                // same formula as vanilla ore drops, so any level works (modded enchantments go up to 255)
                count *= Math.max(0, random.nextInt(bonusLevel + 2) - 1) + 1;
            }
            int max = new ItemStack(rule.item()).getMaxStackSize();
            while (count > 0) {
                ItemStack stack = new ItemStack(rule.item(), (int) Math.min(count, max));
                count -= stack.getCount();
                if (rule.toInventory() && player != null) {
                    // whatever does not fit is dropped at the player's feet
                    player.getInventory().placeItemBackInInventory(stack);
                } else {
                    ItemEntity entity = new ItemEntity(level, pos.x, pos.y, pos.z, stack);
                    entity.setDefaultPickUpDelay();
                    if (drops != null) drops.accept(entity);
                    else level.addFreshEntity(entity);
                }
            }
        }
        if (rule.xp() > 0) {
            // always as orbs: xp given directly would skip Mending
            ExperienceOrb.award(level, rule.toInventory() && player != null ? player.position() : pos, rule.xp());
        }
    }
}
