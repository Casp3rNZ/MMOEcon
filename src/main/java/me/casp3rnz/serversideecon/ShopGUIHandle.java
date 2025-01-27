package me.casp3rnz.serversideecon;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

public class ShopGUIHandle extends GenericContainerScreenHandler {
    public ShopGUIHandle(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, Inventory inventory, int rows) {
        super(type, syncId, playerInventory, inventory, rows);
    }

    // Prevent players from taking or inserting items
    @Override
    protected Slot addSlot(Slot slot) {
        return new Slot(slot.inventory, slot.getIndex(), slot.x, slot.y) {
            @Override
            public boolean canTakeItems(PlayerEntity playerEntity) {
                return false;
            }

            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        };
    }
}