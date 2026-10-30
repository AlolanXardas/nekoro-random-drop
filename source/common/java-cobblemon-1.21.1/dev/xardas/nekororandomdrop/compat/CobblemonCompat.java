package dev.xardas.nekororandomdrop.compat;

import java.util.ArrayList;
import java.util.List;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.drops.LootDroppedEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import dev.xardas.nekororandomdrop.Rewards;
import dev.xardas.nekororandomdrop.Rule;
import dev.xardas.nekororandomdrop.RuleConfig;

/**
 * Loaded only when Cobblemon is installed. Cobblemon drops a Pokemon's loot both when it is killed and when it
 * is defeated in battle, so LOOT_DROPPED covers both for "pokemon_kill".
 */
public final class CobblemonCompat {
    private CobblemonCompat() { }

    public static void init() {
        CobblemonEvents.LOOT_DROPPED.subscribe(Priority.LOWEST, event -> {
            onLootDropped(event);
            return Unit.INSTANCE;
        });
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            onCaptured(event);
            return Unit.INSTANCE;
        });
    }

    private static void onLootDropped(LootDroppedEvent event) {
        if (event.isCanceled() || RuleConfig.rules(Rule.Source.POKEMON_KILL).isEmpty()) return;
        if (!(event.getEntity() instanceof PokemonEntity entity) || !(entity.level() instanceof ServerLevel level)) return;
        ServerPlayer player = event.getPlayer();
        Pokemon pokemon = entity.getPokemon();
        for (Rule rule : RuleConfig.rules(Rule.Source.POKEMON_KILL)) {
            if (matches(rule, pokemon) && rule.matchesPlace(level, entity.blockPosition())) {
                Rewards.roll(rule, level, entity.position(), player, 0, null);
            }
        }
    }

    private static void onCaptured(PokemonCapturedEvent event) {
        if (RuleConfig.rules(Rule.Source.POKEMON_CATCH).isEmpty()) return;
        ServerPlayer player = event.getPlayer();
        if (!(player.level() instanceof ServerLevel level)) return;
        Pokemon pokemon = event.getPokemon();
        for (Rule rule : RuleConfig.rules(Rule.Source.POKEMON_CATCH)) {
            if (matches(rule, pokemon) && rule.matchesPlace(level, player.blockPosition())) {
                Rewards.roll(rule, level, player.position(), player, 0, null);
            }
        }
    }

    private static boolean matches(Rule rule, Pokemon pokemon) {
        List<String> types = new ArrayList<>();
        for (ElementalType type : pokemon.getTypes()) types.add(type.getName());
        return rule.matchesPokemon(pokemon.getSpecies().getResourceIdentifier(), types);
    }
}
