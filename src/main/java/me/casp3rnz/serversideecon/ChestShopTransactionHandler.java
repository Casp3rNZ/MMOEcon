package me.casp3rnz.serversideecon;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Objects;
import java.util.UUID;

public class ChestShopTransactionHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient && hand == Hand.MAIN_HAND) {
                ItemStack heldItem = player.getStackInHand(hand);
                if (heldItem.getItem() == Items.OAK_SIGN) {
                    BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());
                    if (blockEntity instanceof ChestBlockEntity) {
                        // Place the sign and open the sign edit dialogue
                        BlockPos signPos = hitResult.getBlockPos().up();
                        BlockState signState = Blocks.OAK_SIGN.getDefaultState();
                        world.setBlockState(signPos, signState);
                        SignBlockEntity signBlockEntity = (SignBlockEntity) world.getBlockEntity(signPos);
                        if (signBlockEntity != null) {
                            player.openEditSignScreen(signBlockEntity, false);
                        }
                        return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient && blockEntity instanceof SignBlockEntity sign) {
                BlockEntity attachedBlockEntity = world.getBlockEntity(sign.getPos().down());
                if (attachedBlockEntity instanceof ChestBlockEntity) {
                    handleTransaction((ServerPlayerEntity) player, sign, (ChestBlockEntity) attachedBlockEntity);
                    return false; // Allow block breaking
                }
            }
            return true; // Allow block breaking
        });
    }

    private static void handleTransaction(ServerPlayerEntity player, SignBlockEntity sign, ChestBlockEntity chest) {
        SignText messages = sign.getText(false);

        if (messages.getMessages(true) < 4) {
            player.sendMessage(Text.of("Invalid shop sign."), false);
            return;
        }

        String owner = messages.getMessage(0, false).getString();
        int quantity;
        try {
            quantity = Integer.parseInt(messages.getMessage(1, false).getString());
        } catch (NumberFormatException e) {
            player.sendMessage(Text.of("Invalid quantity."), false);
            return;
        }

        String[] prices = messages.getMessage(2, false).getString().split(":");
        float buyPrice = -1;
        float sellPrice = -1;
        try {
            if (prices[0].startsWith("B")) {
                buyPrice = Float.parseFloat(prices[0].substring(1));
            }
            if (prices.length > 1 && prices[1].startsWith("S")) {
                sellPrice = Float.parseFloat(prices[1].substring(1));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Text.of("Invalid price format."), false);
            return;
        }

        ItemStack itemStack = chest.getStack(0);
        if (itemStack.isEmpty()) {
            player.sendMessage(Text.of("Shop is out of stock."), false);
            return;
        }

        if (buyPrice > 0 && player.isSneaking()) {
            // Handle buy transaction
            handleBuyTransaction(player, owner, quantity, buyPrice, itemStack, chest);
        } else if (sellPrice > 0) {
            // Handle sell transaction
            handleSellTransaction(player, owner, quantity, sellPrice, itemStack, chest);
        } else {
            player.sendMessage(Text.of("Invalid transaction."), false);
        }
    }

    private static void handleBuyTransaction(ServerPlayerEntity player, String owner, int quantity, float buyPrice, ItemStack itemStack, ChestBlockEntity chest) {
        // Check if the player has enough balance
        if (PlayerBalanceManager.getBalance(player.getUuid()) < buyPrice * quantity) {
            player.sendMessage(Text.of("You do not have enough balance to buy the items."), false);
            return;
        }

        // Check if the chest has enough items
        int totalItemsInChest = 0;
        for (int i = 0; i < chest.size(); i++) {
            ItemStack stack = chest.getStack(i);
            if (stack.getItem() == itemStack.getItem()) {
                totalItemsInChest += stack.getCount();
            }
        }
        if (totalItemsInChest < quantity) {
            player.sendMessage(Text.of("The shop does not have enough items in stock."), false);
            return;
        }

        // Deduct the items from the chest
        int remainingQuantity = quantity;
        for (int i = 0; i < chest.size() && remainingQuantity > 0; i++) {
            ItemStack stack = chest.getStack(i);
            if (stack.getItem() == itemStack.getItem()) {
                int countToTransfer = Math.min(stack.getCount(), remainingQuantity);
                ItemStack purchasedItems = stack.copy();
                purchasedItems.setCount(countToTransfer);
                if (!player.getInventory().insertStack(purchasedItems)) {
                    player.sendMessage(Text.of("Your inventory is full."), false);
                    return;
                }
                stack.decrement(countToTransfer);
                remainingQuantity -= countToTransfer;
            }
        }

        // Deduct the balance from the player
        PlayerBalanceManager.subtractBalance(player.getUuid(), buyPrice * quantity);

        // Send confirmation messages
        player.sendMessage(Text.of("You have successfully bought " + quantity + " items for " + buyPrice * quantity + " currency."), false);
    }

    private static void handleSellTransaction(ServerPlayerEntity player, String owner, int quantity, float sellPrice, ItemStack itemStack, ChestBlockEntity chest) {
        // Check if the player has enough items to sell
        int totalItemsInInventory = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == itemStack.getItem()) {
                totalItemsInInventory += stack.getCount();
            }
        }
        if (totalItemsInInventory < quantity) {
            player.sendMessage(Text.of("You do not have enough items to sell."), false);
            return;
        }

        // Check if the chest has enough space
        int totalEmptySlots = 0;
        for (int i = 0; i < chest.size(); i++) {
            if (chest.getStack(i).isEmpty()) {
                totalEmptySlots++;
            }
        }
        if (totalEmptySlots < quantity) {
            player.sendMessage(Text.of("The shop does not have enough space to store the items."), false);
            return;
        }

        // Check if the shop owner has enough balance
        UUID ownerUuid = Objects.requireNonNull(Objects.requireNonNull(player.getServer()).getPlayerManager().getPlayer(owner)).getUuid();
        if (PlayerBalanceManager.getBalance(ownerUuid) < sellPrice * quantity) {
            player.sendMessage(Text.of("The shop owner does not have enough balance to buy the items."), false);
            return;
        }

        // Remove the items from the player's inventory
        int remainingQuantity = quantity;
        for (int i = 0; i < player.getInventory().size() && remainingQuantity > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == itemStack.getItem()) {
                int countToTransfer = Math.min(stack.getCount(), remainingQuantity);
                stack.decrement(countToTransfer);
                remainingQuantity -= countToTransfer;
            }
        }

        // Add the items to the chest
        remainingQuantity = quantity;
        for (int i = 0; i < chest.size() && remainingQuantity > 0; i++) {
            ItemStack stack = chest.getStack(i);
            if (stack.isEmpty()) {
                ItemStack soldItems = itemStack.copy();
                soldItems.setCount(remainingQuantity);
                chest.setStack(i, soldItems);
                remainingQuantity = 0;
            } else if (stack.getItem() == itemStack.getItem() && stack.getCount() < stack.getMaxCount()) {
                int countToTransfer = Math.min(stack.getMaxCount() - stack.getCount(), remainingQuantity);
                stack.increment(countToTransfer);
                remainingQuantity -= countToTransfer;
            }
        }

        // Add the balance to the player
        PlayerBalanceManager.addBalance(player.getUuid(), sellPrice * quantity);

        // Deduct the balance from the shop owner
        PlayerBalanceManager.subtractBalance(ownerUuid, sellPrice * quantity);

        // Send confirmation messages
        player.sendMessage(Text.of("You have successfully sold " + quantity + " items for " + sellPrice * quantity + " currency."), false);
    }
}