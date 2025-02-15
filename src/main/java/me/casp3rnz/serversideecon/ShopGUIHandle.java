package me.casp3rnz.serversideecon;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.Random;

public class ShopGUIHandle extends GenericContainerScreenHandler {
    public ShopGUIHandle(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, Inventory inventory, int rows) {
        super(type, syncId, playerInventory, inventory, rows);
        checkSize(inventory, rows * 9);
        inventory.onOpen(playerInventory.player);

        // Add shop inventory slots
        int i = (rows - 4) * 18;
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean canTakeItems(PlayerEntity playerEntity) {
                        return false;
                    }

                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        // Add player inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + i));
            }
        }

        // Add player hotbar slots
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 161 + i));
        }
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.getInventory().onClose(player);
    }

    @Override
    public void onSlotClick(int slotId, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotId < this.getInventory().size()) {
            if (player instanceof ServerPlayerEntity) {
                Random random = new Random();
                float pitch = 0.5F + random.nextFloat();
                ((ServerPlayerEntity) player).playSound(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 0.5F, pitch);
            }
        }

        if (slotId < 0) {
            return;
        }

        // Clear any picked up item
        this.setCursorStack(ItemStack.EMPTY);

        //Main shop Window
        int wst = ShopGUI.getWindowState(player.getUuid());
        if (wst == 0) {
            if (slotId < 45 && slotId < ShopConfigManager.categories.size() && !ShopConfigManager.categories.isEmpty()) {
                // Open category
                ShopConfigManager.Category category = ShopConfigManager.categories.get(slotId);
                ShopGUI.setLastCategoryOpened(player.getUuid(), category);
                ShopGUI.openCategory((ServerPlayerEntity) player, category);
            }
        }
        // Category Window
        else if(wst == 1) {
             if (slotId < 45) { // open category
                ShopConfigManager.Category category = ShopGUI.getLastCategoryOpened(player.getUuid());
                for (ShopConfigManager.ShopItem item : category.items) {
                    ItemStack clickedItem = this.getSlot(slotId).getStack();
                    if (item.id.equals(Registries.ITEM.getId(clickedItem.getItem()).toString())) {
                        ShopGUI.setLastTransactionItem(player.getUuid(), clickedItem.getItem());
                        switch(button){
                            case 0:
                                if(ShopGUI.hasBuyPrice(item.id)) {
                                    ShopGUI.setSaleOrBuy(player.getUuid(), false);
                                    ShopGUI.openQuantitySelection((ServerPlayerEntity) player, clickedItem.getItem(), false); // Left click to buy
                                    break;
                                }else{
                                    ShopGUI.openCategory((ServerPlayerEntity) player, category);
                                }
                            case 1:
                                if(ShopGUI.hasSellPrice(item.id)) {
                                    ShopGUI.setSaleOrBuy(player.getUuid(), true);
                                    ShopGUI.openQuantitySelection((ServerPlayerEntity) player, clickedItem.getItem(), true); // Right click to sell
                                    break;
                                }else{
                                    ShopGUI.openCategory((ServerPlayerEntity) player, category);
                                }
                        }
                    }
                }
            } else if (slotId == 48 || slotId == 50) { // previous or next page
                 ItemStack clickedItem = this.getSlot(slotId).getStack();
                 if (clickedItem.getItem() != Registries.ITEM.get(new Identifier("minecraft:glass_pane"))) {
                     int increment = (slotId == 48) ? -1 : 1;
                     ShopGUI.incCurrentPage(player.getUuid(), increment);
                     ShopGUI.openCategory((ServerPlayerEntity) player, ShopGUI.getLastCategoryOpened(player.getUuid()));
                 }
             }else if(slotId == 45){ // go back
                 ShopGUI.openShop((ServerPlayerEntity) player);
             }

        }
        // Quantity Window
        else if(wst == 2) {
            switch (slotId) {
                // Go back
                case 18 -> ShopGUI.openCategory((ServerPlayerEntity) player, ShopGUI.getLastCategoryOpened(player.getUuid()));
                // Positive quantity buttons
                case 14 -> {
                    ShopGUI.incTransactionQuantity(player.getUuid(),1);
                    ShopGUI.openQuantitySelection((ServerPlayerEntity) player, ShopGUI.getLastTransactionItem(player.getUuid()), ShopGUI.isSaleOrBuy(player.getUuid()));
                }
                case 15 -> {
                    ShopGUI.incTransactionQuantity(player.getUuid(),10);
                    ShopGUI.openQuantitySelection((ServerPlayerEntity) player, ShopGUI.getLastTransactionItem(player.getUuid()), ShopGUI.isSaleOrBuy(player.getUuid()));
                }
                case 16 -> {
                    ShopGUI.incTransactionQuantity(player.getUuid(),64);
                    ShopGUI.openQuantitySelection((ServerPlayerEntity) player, ShopGUI.getLastTransactionItem(player.getUuid()), ShopGUI.isSaleOrBuy(player.getUuid()));
                }
                // Negative quantity buttons
                case 10 -> {
                    ShopGUI.incTransactionQuantity(player.getUuid(),-1);
                    ShopGUI.openQuantitySelection((ServerPlayerEntity) player, ShopGUI.getLastTransactionItem(player.getUuid()), ShopGUI.isSaleOrBuy(player.getUuid()));
                }
                case 11 -> {
                    ShopGUI.incTransactionQuantity(player.getUuid(),-10);
                    ShopGUI.openQuantitySelection((ServerPlayerEntity) player, ShopGUI.getLastTransactionItem(player.getUuid()), ShopGUI.isSaleOrBuy(player.getUuid()));
                }
                case 12 -> {
                    ShopGUI.incTransactionQuantity(player.getUuid(),-64);
                    ShopGUI.openQuantitySelection((ServerPlayerEntity) player, ShopGUI.getLastTransactionItem(player.getUuid()), ShopGUI.isSaleOrBuy(player.getUuid()));
                }

                // Sell Inventory
                case 9 -> {
                    if (ShopGUI.isSaleOrBuy(player.getUuid())) {
                        ShopGUI.sellInventoryWide((ServerPlayerEntity) player, ShopGUI.getLastTransactionItem(player.getUuid()).toString());
                        ShopGUI.openCategory((ServerPlayerEntity) player, ShopGUI.getLastCategoryOpened(player.getUuid()));
                    }
                }
                // Confirm transaction
                case 26 -> {
                    Item clickedItem = ShopGUI.getLastTransactionItem(player.getUuid());
                    String clickedItemId = Registries.ITEM.getId(clickedItem).toString();
                    // this is fucking stupid, but I don't know how to do it any better in Java/Fabric API.
                    if (ShopGUI.isSaleOrBuy(player.getUuid())) {
                        //Sell item
                            for (ShopConfigManager.ShopItem item : ShopGUI.getLastCategoryOpened(player.getUuid()).items) {
                                if (item.id.equals(clickedItemId)) {
                                    float price = item.sellPrice;
                                    float transactionAmount = price * ShopGUI.getTransactionQuantity(player.getUuid());
                                    ShopGUI.processTransaction((ServerPlayerEntity) player, clickedItemId, transactionAmount, true);
                                    ShopGUI.openCategory((ServerPlayerEntity) player, ShopGUI.getLastCategoryOpened(player.getUuid()));
                                    break;
                                }
                        }
                    } else {
                        // Buy item
                            for (ShopConfigManager.ShopItem item : ShopGUI.getLastCategoryOpened(player.getUuid()).items) {
                                if (item.id.equals(clickedItemId)) {
                                    float price = item.buyPrice;
                                    float transactionAmount = price * ShopGUI.getTransactionQuantity(player.getUuid());
                                    ShopGUI.processTransaction((ServerPlayerEntity) player, clickedItemId, transactionAmount, false);
                                    ShopGUI.openCategory((ServerPlayerEntity) player, ShopGUI.getLastCategoryOpened(player.getUuid()));
                                    break;
                                }
                            }
                    }
                }
            }
        }
    }
}