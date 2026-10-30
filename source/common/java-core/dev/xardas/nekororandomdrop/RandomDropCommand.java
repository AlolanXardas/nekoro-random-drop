package dev.xardas.nekororandomdrop;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** /randomdrop reload and /randomdrop list (operators only). */
public final class RandomDropCommand {
    private RandomDropCommand() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("randomdrop")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload").executes(ctx -> {
                    RuleConfig.Result result = RuleConfig.load();
                    String text = "Nekoro Random Drop: loaded " + result.loaded() + " rules"
                            + (result.skipped() > 0 ? ", skipped " + result.skipped() + " (see the log)" : "");
                    ctx.getSource().sendSuccess(() -> Component.literal(text), true);
                    return result.loaded();
                }))
                .then(Commands.literal("list").executes(ctx -> {
                    if (RuleConfig.all().isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("Nekoro Random Drop: no rules"), false);
                    }
                    for (Rule rule : RuleConfig.all()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(rule.describe()), false);
                    }
                    return RuleConfig.all().size();
                })));
    }
}
