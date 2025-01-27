package me.casp3rnz.serversideecon;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ShopGUI {
    public static void openShop(ServerPlayerEntity player) {
        Inventory inventory = new SimpleInventory(54); // Double chest size

        // Fill the inventory with category icons
        int slot = 0;
        for (ShopConfigManager.Category category : ShopConfigManager.categories) {
            ItemStack categoryItem = new ItemStack(Registries.ITEM.get(new Identifier(category.representativeItem)));
            categoryItem.setCustomName(Text.of(category.name));
            inventory.setStack(slot++, categoryItem);
        }

        for (int i = 45; i < 54; i++) {
            ItemStack placeholder = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:glass_pane")));
            placeholder.setCustomName(Text.of(""));
            inventory.setStack(i, placeholder);
        }

        // Open the inventory for the player
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) ->
                new ShopGUIHandle(ScreenHandlerType.GENERIC_9X6, syncId, inv, inventory, 6),
                Text.of("Shop")));
    }

    public void openCategory(ServerPlayerEntity player, ShopConfigManager.Category category) {
        Inventory inventory = new SimpleInventory(54); // Double chest size

        // Fill the inventory with items from the selected category
        int slot = 0;
        for (ShopConfigManager.ShopItem item : category.items) {
            ItemStack shopItem = new ItemStack(Registries.ITEM.get(new Identifier(item.id)));
            shopItem.setCustomName(Text.of(item.displayName));
            inventory.setStack(slot++, shopItem);
        }

        // Add placeholder items for navigation
        for (int i = 45; i < 54; i++) {
            ItemStack placeholder = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:barrier")));
            placeholder.setCustomName(Text.of("Placeholder"));
            inventory.setStack(i, placeholder);
        }

        // Open the inventory for the player
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) ->
                new ShopGUIHandle(ScreenHandlerType.GENERIC_9X6, syncId, inv, inventory, 6),
                Text.of(category.name)));
    }
}