package dev.xardas.nekororandomdrop;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import dev.xardas.nekororandomdrop.compat.CobblemonCompat;

/** Forge entry point (1.20.1 and 1.21.1). Runs last on the events, so actions cancelled by other mods are skipped. */
@Mod(RandomDrop.MOD_ID)
public final class NekoroRandomDrop {
    public NekoroRandomDrop() {
        RuleConfig.init(FMLPaths.CONFIGDIR.get());
        MinecraftForge.EVENT_BUS.register(NekoroRandomDrop.class);
        // only touches Cobblemon classes when Cobblemon is installed
        if (ModList.get().isLoaded("cobblemon")) CobblemonCompat.init();
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        RuleConfig.load();
    }

    /** /reload also reloads the config (and JEI rebuilds its Random Drop tab after it). */
    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener((ResourceManagerReloadListener) resources -> RuleConfig.load());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        RandomDropCommand.register(event.getDispatcher());
    }

    /** Forge has no block drop event, so the same checks as for real drops: survival and a tool that can harvest it. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;
        if (!(event.getLevel() instanceof ServerLevel level) || !player.hasCorrectToolForDrops(event.getState())) return;
        DropLogic.onBlockDrops(level, event.getPos(), event.getState(), player, player.getMainHandItem(), null);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.isCanceled()) return;
        DropLogic.onMobDeath(event.getEntity(), event.getSource(), event.getDrops()::add);
    }
}
