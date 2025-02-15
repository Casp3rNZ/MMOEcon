package me.casp3rnz.serversideecon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class SellCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sell")
                .then(CommandManager.literal("hand")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            return sellHand(player);
                        }))
                .then(CommandManager.literal("inv")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            return sellInventory(player);
                        }))
        );
    }

    private static int sellHand(ServerPlayerEntity player) {
        ItemStack itemStack = player.getMainHandStack();
        if (!itemStack.isEmpty()) {
            String itemId = Registries.ITEM.getId(itemStack.getItem()).toString();
            for (ShopConfigManager.ShopItem shopItem : ShopConfigManager.getAllShopItems()) {
                if (shopItem.id.equals(itemId) && shopItem.sellPrice != null) {
                    int quantity = itemStack.getCount();
                    float total = shopItem.sellPrice * quantity;
                    PlayerBalanceManager.addBalance(player.getUuid(), total);
                    player.getInventory().removeStack(player.getInventory().selectedSlot);
                    TransactionLogger.log(player.getEntityName() + "sold" + quantity + " " + itemStack.getName().getString() + " for $" + ShopGUI.formatMoney(total));
                    player.sendMessage(Text.of("Sold " + quantity + " " + itemStack.getName().getString() + " for $" + ShopGUI.formatMoney(total)), false);
                    player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.7F, 1.0F);
                    return 1;
                }
            }
            player.sendMessage(Text.of("This item is not sellable."), false);
        } else {
            player.sendMessage(Text.of("You are not holding any item."), false);
        }
        return 0;
    }

    private static int sellInventory(ServerPlayerEntity player) {
        float totalReimbursement = 0;
        int totalSold = 0;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && !player.getInventory().armor.contains(stack) && !player.getInventory().offHand.contains(stack)) {
                String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                for (ShopConfigManager.ShopItem shopItem : ShopConfigManager.getAllShopItems()) {
                    if (shopItem.id.equals(itemId) && shopItem.sellPrice != null) {
                        int quantity = stack.getCount();
                        totalReimbursement += shopItem.sellPrice * quantity;
                        totalSold += quantity;
                        player.getInventory().removeStack(i);
                        TransactionLogger.log(player.getEntityName() + " sold " + quantity + " " + stack.getName().getString() + " for $" + ShopGUI.formatMoney(shopItem.sellPrice * quantity));
                        player.sendMessage(Text.of("Sold " + quantity + " " + stack.getName().getString() + " for $" + ShopGUI.formatMoney(shopItem.sellPrice * quantity)), false);
                        break;
                    }
                }
            }
        }

        if (totalSold > 0) {
            PlayerBalanceManager.addBalance(player.getUuid(), totalReimbursement);
            player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.7F, 1.0F);
        } else {
            player.sendMessage(Text.of("No sellable items in your inventory."), false);
        }
        return 1;
    }
}