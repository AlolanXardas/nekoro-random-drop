package dev.xardas.nekororandomdrop;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import dev.xardas.nekororandomdrop.compat.VersionCompat;

/** What happens when a block drops or a mob dies; every loader calls these from its own events. */
public final class DropLogic {
    private DropLogic() { }

    /** A block was mined and drops its items (the loader already checked the tool can harvest it). */
    public static void onBlockDrops(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player, ItemStack tool,
                                    Consumer<ItemEntity> drops) {
        if (RuleConfig.rules(Rule.Source.BLOCK).isEmpty()) return;
        int silk = VersionCompat.silkTouch(level, tool);
        int fortune = VersionCompat.fortune(level, tool);
        Vec3 center = Vec3.atCenterOf(pos);
        for (Rule rule : RuleConfig.rules(Rule.Source.BLOCK)) {
            if (silk > 0 && rule.silkTouch() == Rule.SilkTouch.NO) continue;
            if (rule.minTool() != null && !VersionCompat.meetsTool(tool, rule.minTool())) continue;
            if (!rule.matchesBlock(state) || !rule.matchesPlace(level, pos)) continue;
            double factor = rule.silkTouch() == Rule.SilkTouch.BONUS ? RuleConfig.silkTouchFactor(silk) : 1;
            Rewards.roll(rule, level, center, player, fortune, factor, drops);
        }
    }

    /** A mob died and drops its loot. */
    public static void onMobDeath(LivingEntity entity, DamageSource source, Consumer<ItemEntity> drops) {
        if (RuleConfig.rules(Rule.Source.MOB).isEmpty() || !(entity.level() instanceof ServerLevel level)) return;
        ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p : null;
        int looting = VersionCompat.looting(level, source);
        BlockPos pos = entity.blockPosition();
        ItemStack weapon = VersionCompat.weapon(source);
        for (Rule rule : RuleConfig.rules(Rule.Source.MOB)) {
            if (rule.minTool() != null && !VersionCompat.meetsTool(weapon, rule.minTool())) continue;
            if (rule.matchesEntity(entity.getType()) && rule.matchesPlace(level, pos)) {
                Rewards.roll(rule, level, entity.position(), player, looting, drops);
            }
        }
    }
}
