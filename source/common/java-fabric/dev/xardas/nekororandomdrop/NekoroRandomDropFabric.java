package dev.xardas.nekororandomdrop;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import dev.xardas.nekororandomdrop.compat.CobblemonCompat;

/** Fabric entry point (1.20.1 and 1.21.1). */
public final class NekoroRandomDropFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        RuleConfig.init(FabricLoader.getInstance().getConfigDir());
        ServerLifecycleEvents.SERVER_STARTING.register(server -> RuleConfig.load());
        // /reload also reloads the config (and JEI rebuilds its Random Drop tab after it)
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> RuleConfig.load());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> RandomDropCommand.register(dispatcher));

        // after the block is gone; the same checks as for real drops: survival and a tool that can harvest it
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative()
                    && serverPlayer.hasCorrectToolForDrops(state)) {
                DropLogic.onBlockDrops(level, pos, state, serverPlayer, serverPlayer.getMainHandItem(), null);
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> DropLogic.onMobDeath(entity, source, null));

        // only touches Cobblemon classes when Cobblemon is installed
        if (FabricLoader.getInstance().isModLoaded("cobblemon")) CobblemonCompat.init();
    }
}
