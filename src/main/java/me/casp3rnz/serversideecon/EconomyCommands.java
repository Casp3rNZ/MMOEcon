package me.casp3rnz.serversideecon;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import java.util.*;

import static net.minecraft.server.command.CommandManager.literal;

public class EconomyCommands {

    private static final SuggestionProvider<ServerCommandSource> PLAYER_SUGGESTIONS = (context, builder) -> CommandSource.suggestMatching(context.getSource().getPlayerNames(), builder);

    public static void registerCommands() {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SellCommand.register(dispatcher);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("bal").executes(ctx -> {
                UUID playerUUID = Objects.requireNonNull(ctx.getSource().getPlayer()).getUuid();
                float balance = PlayerBalanceManager.getBalance(playerUUID);
                ctx.getSource().sendFeedback(() -> Text.of("Your balance: $" + ShopGUI.formatMoney(balance)), false);
                return Command.SINGLE_SUCCESS;
            }));

            dispatcher.register(literal("money").executes(ctx -> {
                UUID playerUUID = Objects.requireNonNull(ctx.getSource().getPlayer()).getUuid();
                float balance = PlayerBalanceManager.getBalance(playerUUID);
                ctx.getSource().sendFeedback(() -> Text.of("Your balance: $" + ShopGUI.formatMoney(balance)), false);
                return Command.SINGLE_SUCCESS;
            }));

            // /pay command
            dispatcher.register(literal("pay")
                    .then(CommandManager.argument("target", StringArgumentType.string())
                            .suggests(PLAYER_SUGGESTIONS)
                            .then(CommandManager.argument("amount", FloatArgumentType.floatArg())
                                    .executes(ctx -> {
                                        ServerPlayerEntity senderPlayer = ctx.getSource().getPlayer();
                                        String targetName = StringArgumentType.getString(ctx, "target");
                                        ServerPlayerEntity targetPlayer = ctx.getSource().getServer().getPlayerManager().getPlayer(targetName);
                                        if (targetPlayer == null) {
                                            ctx.getSource().sendFeedback(() -> Text.of("Player not found."), false);
                                            return Command.SINGLE_SUCCESS;
                                        }
                                        UUID senderUUID = senderPlayer.getUuid();
                                        UUID targetUUID = targetPlayer.getUuid();
                                        float amount = FloatArgumentType.getFloat(ctx, "amount");

                                        if (amount <= 0) {
                                            ctx.getSource().sendFeedback(() -> Text.of("Amount must be greater than zero."), false);
                                            return Command.SINGLE_SUCCESS;
                                        }

                                        float senderBalance = PlayerBalanceManager.getBalance(senderUUID);
                                        if (senderBalance >= amount) {
                                            PlayerBalanceManager.subtractBalance(senderUUID, amount);
                                            PlayerBalanceManager.addBalance(targetUUID, amount);
                                            ctx.getSource().sendFeedback(() -> Text.of("You paid $" + amount + " to " + targetName), false);
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
                                List<Map.Entry<UUID, Float>> sortedBalances = new ArrayList<>(PlayerBalanceManager.getBalances().entrySet());
                                sortedBalances.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
                                ctx.getSource().sendFeedback(() -> Text.of("Wealthiest players:"), false);
                                MinecraftServer server = ctx.getSource().getServer();
                                for (int i = 0; i < Math.min(10, sortedBalances.size()); i++) {
                                    final int index = i;
                                    Map.Entry<UUID, Float> entry = sortedBalances.get(index);
                                    Optional<GameProfile> profile = server.getUserCache().getByUuid(entry.getKey());
                                    String playerName = profile.isPresent() ? profile.get().getName() : "unknown";
                                    ctx.getSource().sendFeedback(() -> Text.of((index + 1) + ". " + playerName + ": $" + ShopGUI.formatMoney(entry.getValue())), false);
                                }
                                return Command.SINGLE_SUCCESS;
                            })));

            // Load shopGUI
            dispatcher.register(literal("shop").executes(ctx -> {
                ShopGUI.openShop(ctx.getSource().getPlayer());
                return Command.SINGLE_SUCCESS;
            }));

            // Reload shop config
            dispatcher.register(literal("shop")
                    .then(literal("reload").executes(ctx -> {
                        ShopConfigManager.reloadConfig();
                        ctx.getSource().sendFeedback(() -> Text.of("Shop config reloaded!"), false);
                        return Command.SINGLE_SUCCESS;
                    })));
        });
    }
}