package dev.xardas.nekororandomdrop;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import dev.xardas.nekororandomdrop.compat.CobblemonCompat;

/** NeoForge entry point. Runs late on the drop events, so drops cancelled by other mods are skipped. */
@Mod(RandomDrop.MOD_ID)
public final class NekoroRandomDrop {
    public NekoroRandomDrop() {
        RuleConfig.init(FMLPaths.CONFIGDIR.get());
        NeoForge.EVENT_BUS.register(NekoroRandomDrop.class);
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

    /** Fires only when the block really drops, so a wrong tool or creative mode gives nothing. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.isCanceled()) return;
        ServerPlayer player = event.getBreaker() instanceof ServerPlayer p ? p : null;
        DropLogic.onBlockDrops(event.getLevel(), event.getPos(), event.getState(), player, event.getTool(), event.getDrops()::add);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.isCanceled()) return;
        DropLogic.onMobDeath(event.getEntity(), event.getSource(), event.getDrops()::add);
    }
}
