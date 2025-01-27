package me.casp3rnz.serversideecon;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.literal;

public class EconomyCommands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("bal").executes(ctx -> {
                String playerName = ctx.getSource().getName();
                float balance = serverside.balanceManager.getBalance(playerName);
                ctx.getSource().sendFeedback(() -> Text.of("Your balance: $" + balance), false);
                return Command.SINGLE_SUCCESS;
            }));

            dispatcher.register(literal("money").executes(ctx -> {
                String playerName = ctx.getSource().getName();
                float balance = serverside.balanceManager.getBalance(playerName);
                ctx.getSource().sendFeedback(() -> Text.of("Your balance: $" + balance), false);
                return Command.SINGLE_SUCCESS;
            }));

            // /pay command
            dispatcher.register(literal("pay")
                    .then(CommandManager.argument("target", StringArgumentType.string())
                            .then(CommandManager.argument("amount", FloatArgumentType.floatArg())
                                    .executes(ctx -> {
                                        String sender = ctx.getSource().getName();
                                        String target = StringArgumentType.getString(ctx, "target");
                                        float amount = FloatArgumentType.getFloat(ctx, "amount");

                                        if (amount <= 0) {
                                            ctx.getSource().sendFeedback(() -> Text.of("Amount must be greater than zero."), false);
                                            return Command.SINGLE_SUCCESS;
                                        }

                                        // todo: add check if player exists.

                                        // Process the payment
                                        float senderBalance = serverside.balanceManager.getBalance(sender);
                                        if (senderBalance >= amount) {
                                            serverside.balanceManager.subtractBalance(sender, amount);
                                            serverside.balanceManager.addBalance(target, amount);
                                            serverside.balanceManager.saveBalances();

                                            ctx.getSource().sendFeedback(() -> Text.of("You paid $" + amount + " to " + target), false);
                                            return Command.SINGLE_SUCCESS;
                                        } else {
                                            ctx.getSource().sendFeedback(() -> Text.of("You don't have enough money."), false);
                                            return Command.SINGLE_SUCCESS;
                                        }
                                    }))));

            // /bal top command
            dispatcher.register(literal("bal")
                    .then(literal("top")
                            .executes(ctx -> {
                                List<Map.Entry<String, Float>> sortedBalances = new ArrayList<Map.Entry<String, Float>>(serverside.balanceManager.getBalances().entrySet());
                                ctx.getSource().sendFeedback(() -> Text.of("Wealthiest players:"), false);
                                for (int i = 0; i < Math.min(10, sortedBalances.size()); i++) {
                                    final int index = i;
                                    Map.Entry<String, Float> entry = sortedBalances.get(index);
                                    ctx.getSource().sendFeedback(() -> Text.of((index + 1) + ". " + entry.getKey() + ": $" + entry.getValue()), false);
                                }
                                return Command.SINGLE_SUCCESS;
                            })));
            // Load shopGUI
            dispatcher.register(literal("shop").executes(ctx -> {
                ShopGUI.openShop(ctx.getSource().getPlayer());
                return Command.SINGLE_SUCCESS;
            }));

            //reload shop config
            dispatcher.register(literal("shop")
                    .then(literal("reload").executes(ctx -> {
                        ShopConfigManager.reloadConfig();
                        ctx.getSource().sendFeedback(() -> Text.of("Shop config reloaded!"), false);
                        return Command.SINGLE_SUCCESS;
                    })));
        });
    }
}
