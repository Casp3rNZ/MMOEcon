package me.casp3rnz.serversideecon;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import net.minecraft.nbt.NbtCompound;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopGUI {
    private static final Map<UUID, Integer> windowStateMap = new HashMap<>();
    private static final Map<UUID, ShopConfigManager.Category> lastCategoryOpenedMap = new HashMap<>();
    private static final Map<UUID, Integer> transactionQuantityMap = new HashMap<>();
    private static final Map<UUID, Item> lastTransactionItemMap = new HashMap<>();
    private static final Map<UUID, Boolean> saleOrBuyMap = new HashMap<>();
    private static final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");
    private static final Map<UUID, Integer> currentPageMap = new HashMap<>();

    public static int getCurrentPage(UUID playerUUID) {
        return currentPageMap.getOrDefault(playerUUID, 0);
    }

    public static void setCurrentPage(UUID playerUUID, int page) {
        currentPageMap.put(playerUUID, page);
    }

    public static void incCurrentPage(UUID playerUUID, int increment) {
        int newPage = currentPageMap.getOrDefault(playerUUID, 0) + increment;
        currentPageMap.put(playerUUID, Math.max(newPage, 0));
    }

    public static int getWindowState(UUID playerUUID) {
        return windowStateMap.getOrDefault(playerUUID, 0);
    }

    public static void setWindowState(UUID playerUUID, int state) {
        windowStateMap.put(playerUUID, state);
    }

    public static ShopConfigManager.Category getLastCategoryOpened(UUID playerUUID) {
        return lastCategoryOpenedMap.get(playerUUID);
    }

    public static void setLastCategoryOpened(UUID playerUUID, ShopConfigManager.Category category) {
        lastCategoryOpenedMap.put(playerUUID, category);
    }

    public static int getTransactionQuantity(UUID playerUUID) {
        return transactionQuantityMap.getOrDefault(playerUUID, 1);
    }

    public static void incTransactionQuantity(UUID playerUUID, int quantity) {
        int newTransQ = transactionQuantityMap.getOrDefault(playerUUID, 1) + quantity;
        if (newTransQ < 1) {
            transactionQuantityMap.put(playerUUID, 1);
        } else transactionQuantityMap.put(playerUUID, Math.min(newTransQ, 256));
    }

    public static Item getLastTransactionItem(UUID playerUUID) {
        return lastTransactionItemMap.get(playerUUID);
    }

    public static void setLastTransactionItem(UUID playerUUID, Item item) {
        lastTransactionItemMap.put(playerUUID, item);
    }

    public static boolean isSaleOrBuy(UUID playerUUID) {
        return saleOrBuyMap.getOrDefault(playerUUID, false);
    }

    public static void setSaleOrBuy(UUID playerUUID, boolean set) {
        saleOrBuyMap.put(playerUUID, set);
    }

    public static String formatMoney(float amount) {
        return moneyFormat.format(amount);
    }

    public static boolean hasSellPrice(String itemId) {
        for (ShopConfigManager.ShopItem shopItem : ShopConfigManager.getAllShopItems()) {
            if (shopItem.id.equals(itemId) && shopItem.sellPrice != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasBuyPrice(String itemId) {
        for (ShopConfigManager.ShopItem shopItem : ShopConfigManager.getAllShopItems()) {
            if (shopItem.id.equals(itemId) && shopItem.buyPrice != null) {
                return true;
            }
        }
        return false;
    }

    public static void clearItemNBT(ItemStack itemStack) {
        if (itemStack.hasNbt()) {
            NbtCompound tag = itemStack.getNbt();
            if (tag != null) {
                tag.remove("display");
                tag.remove("Enchantments");
                tag.remove("AttributeModifiers");
            }
        }
    }

    public static void openShop(ServerPlayerEntity player) {
        UUID playerUUID = player.getUuid();
        try {
            Inventory inventory = new SimpleInventory(54); // Double chest size
            setWindowState(playerUUID, 0);
            incTransactionQuantity(playerUUID, -1000); // Reset quantity
            setCurrentPage(playerUUID, 0);

            // Fill the inventory with category icons
            int slot = 0;
            for (ShopConfigManager.Category category : ShopConfigManager.categories) {
                ItemStack categoryItem = new ItemStack(Registries.ITEM.get(new Identifier(category.representativeItem)));
                clearItemNBT(categoryItem);
                categoryItem.setCustomName(Text.of(category.name));
                inventory.setStack(slot++, categoryItem);
            }

            // Add placeholder items for navigation
            for (int i = 45; i < 54; i++) {
                ItemStack placeholder = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:glass_pane")));
                placeholder.setCustomName(Text.of(""));
                inventory.setStack(i, placeholder);
            }

            ItemStack placeholder = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:lantern")));
            placeholder.setCustomName(Text.of("Shop Guide"));
            NbtList lore = new NbtList();
            lore.add(NbtString.of("[\"\",{\"text\":\"You're Balance: $" + formatMoney(PlayerBalanceManager.getBalance(playerUUID)) +"\",\"color\":\"aqua\"}]"));
            lore.add(NbtString.of("[\"\",{\"text\":\"Left-click to buy.\",\"color\":\"green\"}]"));
            lore.add(NbtString.of("[\"\",{\"text\":\"Right-click Item's to sell.\",\"color\":\"red\"}]"));

            NbtCompound displayTag = placeholder.getOrCreateSubNbt("display");
            displayTag.put("Lore", lore);
            placeholder.setSubNbt("display", displayTag);
            inventory.setStack(49, placeholder);

            // Open the inventory for the player
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) ->
                    new ShopGUIHandle(ScreenHandlerType.GENERIC_9X6, syncId, inv, inventory, 6), Text.of("Store Categories")));
        } catch (Exception e) {
            System.out.println("No Shop Config present");
        }
    }

    public static void openCategory(ServerPlayerEntity player, ShopConfigManager.Category category) {
        UUID playerUUID = player.getUuid();
        Inventory inventory = new SimpleInventory(54); // Double chest size
        setWindowState(playerUUID, 1);
        setLastCategoryOpened(playerUUID, category);
        incTransactionQuantity(playerUUID, -1000); // Reset quantity

        int currentPage = getCurrentPage(playerUUID);
        int itemsPerPage = 45; // 5 rows of 9 slots
        int totalItems = category.items.size();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);

        // Fill the inventory with items from the selected category
        int slot = 0;
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
        for (int i = startIndex; i < endIndex; i++) {
            ShopConfigManager.ShopItem item = category.items.get(i);
            ItemStack shopItem = new ItemStack(Registries.ITEM.get(new Identifier(item.id)));
            NbtList lore = new NbtList();
            if (hasBuyPrice(item.id)) {
                lore.add(NbtString.of("[\"\",{\"text\":\"Buy Price: $" + formatMoney(item.buyPrice) + "\",\"color\":\"green\"}]"));
            }
            if (hasSellPrice(item.id)) {
                lore.add(NbtString.of("[\"\",{\"text\":\"Sell Price: $" + formatMoney(item.sellPrice) + "\",\"color\":\"red\"}]"));
            }
            NbtCompound displayTag = shopItem.getOrCreateSubNbt("display");
            displayTag.put("Lore", lore);
            shopItem.setSubNbt("display", displayTag);
            inventory.setStack(slot++, shopItem);
        }

        // Add placeholder items for navigation
        ItemStack placeholder = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:barrier")));
        placeholder.setCustomName(Text.of("Go Back"));
        inventory.setStack(45, placeholder);

        for(int i = 46; i < 54; i++) {
            ItemStack placeholder2 = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:glass_pane")));
            placeholder2.setCustomName(Text.of(""));
            inventory.setStack(i, placeholder2);
        }
        // Server Guide
        placeholder = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:lantern")));
        placeholder.setCustomName(Text.literal("Shop Guide").formatted(Formatting.AQUA));
        NbtList lore = new NbtList();
        lore.add(NbtString.of("[\"\",{\"text\":\"You're Balance: $" + formatMoney(PlayerBalanceManager.getBalance(playerUUID)) +"\",\"color\":\"aqua\"}]"));
        lore.add(NbtString.of("[\"\",{\"text\":\"Left-click to buy.\",\"color\":\"green\"}]"));
        lore.add(NbtString.of("[\"\",{\"text\":\"Right-click Item's to sell.\",\"color\":\"red\"}]"));

        NbtCompound displayTag = placeholder.getOrCreateSubNbt("display");
        displayTag.put("Lore", lore);
        placeholder.setSubNbt("display", displayTag);
        inventory.setStack(49, placeholder);

        // navigation arrows
        if (currentPage > 0) {
            placeholder = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:arrow")));
            placeholder.setCustomName(Text.of("Previous Page"));
            inventory.setStack(48, placeholder);
        }
        if(currentPage < totalPages - 1) {
            placeholder = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:arrow")));
            placeholder.setCustomName(Text.of("Next Page"));
            inventory.setStack(50, placeholder);
        }

        // Open the inventory for the player
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) ->
                new ShopGUIHandle(ScreenHandlerType.GENERIC_9X6, syncId, inv, inventory, 6),
                Text.of(category.name)));
    }

    public static void openQuantitySelection(ServerPlayerEntity player, Item transactionItem, boolean saleOrBuy) {
        UUID playerUUID = player.getUuid();
        Inventory inventory = new SimpleInventory(27); // Single chest size
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) ->
                new ShopGUIHandle(ScreenHandlerType.GENERIC_9X3, syncId, inv, inventory, 3),
                Text.of("Quantity Selection")));

        setWindowState(playerUUID, 2);
        setLastTransactionItem(playerUUID, transactionItem);
        setSaleOrBuy(playerUUID, saleOrBuy);
        int transQ = getTransactionQuantity(playerUUID);

        // Go Back
        ItemStack item = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:barrier")));
        item.setCustomName(Text.of("Go Back"));
        inventory.setStack(18, item);

        // Quantity selection Negative
        item = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:red_stained_glass_pane")));
        item.setCustomName(Text.literal("-1").formatted(Formatting.RED));
        inventory.setStack(10, item);

        item = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:red_stained_glass_pane")));
        item.setCustomName(Text.literal("-10").formatted(Formatting.RED));
        inventory.setStack(11, item);

        item = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:red_stained_glass_pane")));
        item.setCustomName(Text.literal("-64").formatted(Formatting.RED));
        inventory.setStack(12, item);

        // item in trade
        item = new ItemStack(transactionItem);
        inventory.setStack(4, item);

        // Quantity count item in centre slot
        item = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:emerald")));
        item.setCustomName(Text.of("" + transQ));
        inventory.setStack(13, item);

        // Quantity selection Positive
        item = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:green_stained_glass_pane")));
        item.setCustomName(Text.literal("+1").formatted(Formatting.GREEN));
        inventory.setStack(14, item);

        item = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:green_stained_glass_pane")));
        item.setCustomName(Text.literal("+10").formatted(Formatting.GREEN));
        inventory.setStack(15, item);

        item = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:green_stained_glass_pane")));
        item.setCustomName(Text.literal("+64").formatted(Formatting.GREEN));
        inventory.setStack(16, item);

        // Quantity count section
        float itemPrice = 0;
        float itemSellPrice = 0;
        for (ShopConfigManager.ShopItem item1 : getLastCategoryOpened(playerUUID).items) {
            if (item1.id.equals(Registries.ITEM.getId(transactionItem).toString())) {
                    if (item1.sellPrice != null){
                    itemSellPrice = item1.sellPrice;
                    }
                    if (item1.buyPrice != null) {
                        itemPrice = item1.buyPrice;
                    }
            }
        }

        if (!saleOrBuy) { // Buy
            // Quantity Item
            item = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:emerald")));
            item.setCustomName(Text.literal("Buying: " + transQ).formatted(Formatting.GREEN));
            NbtList lore = new NbtList();
            lore.add(NbtString.of("[\"\",{\"text\":\"Total: $" + formatMoney(itemPrice * transQ) + "\",\"color\":\"green\"}]"));
            NbtCompound displayTag = item.getOrCreateSubNbt("display");
            displayTag.put("Lore", lore);
            item.setSubNbt("display", displayTag);
            inventory.setStack(13, item);

            // Buy Item
            item = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:emerald_block")));
            item.setCustomName(Text.literal("BUY").formatted(Formatting.GREEN));
            inventory.setStack(26, item);
        } else { // Sell
            // Quantity Item
            item = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:orange_concrete")));
            item.setCustomName(Text.literal("Selling: " + transQ).formatted(Formatting.RED));
            NbtList lore = new NbtList();
            lore.add(NbtString.of("[\"\",{\"text\":\"Total: $" + formatMoney(itemSellPrice * transQ) + "\",\"color\":\"red\"}]"));
            NbtCompound displayTag = item.getOrCreateSubNbt("display");
            displayTag.put("Lore", lore);
            item.setSubNbt("display", displayTag);
            inventory.setStack(13, item);

            // Sell Item
            item = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:red_concrete")));
            item.setCustomName(Text.literal("SELL").formatted(Formatting.RED));
            inventory.setStack(26, item);

            // Sell Inventory
            item = new ItemStack(Registries.ITEM.get(new Identifier("minecraft:redstone_block")));
            item.setCustomName(Text.literal("Sell Inventory").formatted(Formatting.RED));
            inventory.setStack(9, item);
        }

        // Open the inventory for the player
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) ->
                new ShopGUIHandle(ScreenHandlerType.GENERIC_9X3, syncId, inv, inventory, 3),
                Text.of("Quantity Selection")));
    }

    public static void processTransaction(ServerPlayerEntity player, String transactionItemString, float total, boolean saleOrBuy) {
        UUID playerUUID = player.getUuid();
        Item transactionItem = Registries.ITEM.get(new Identifier(transactionItemString));
        int transactionQuantity = getTransactionQuantity(playerUUID);

        if (saleOrBuy) { // Sell
            int totalItems = 0;
            for (ItemStack stack : player.getInventory().main) {
                if (stack.getItem() == transactionItem) {
                    totalItems += stack.getCount();
                }
            }

            if (totalItems >= transactionQuantity) {
                int remainingQuantity = transactionQuantity;
                for (int i = 0; i < player.getInventory().size(); i++) {
                    ItemStack stack = player.getInventory().getStack(i);
                    if (stack.getItem() == transactionItem) {
                        int stackCount = stack.getCount();
                        if (stackCount <= remainingQuantity) {
                            player.getInventory().removeStack(i);
                            remainingQuantity -= stackCount;
                        } else {
                            stack.decrement(remainingQuantity);
                            player.getInventory().setStack(i, stack);
                            remainingQuantity = 0;
                        }
                        if (remainingQuantity == 0) break;
                    }
                }
                // Log the transaction
                String transactionType = isSaleOrBuy(playerUUID) ? "sold" : "bought";
                String logMessage = String.format("Player %s %s %s %s for $%.2f", player.getName().getString(), transactionType, transactionQuantity, transactionItem.getName().getString(), total);
                TransactionLogger.log(logMessage);
                setCurrentPage(playerUUID, 0);
                PlayerBalanceManager.addBalance(playerUUID, total);
                player.sendMessage(Text.of("Sold " + transactionQuantity + " " + transactionItem.getName().getString() + " for $" + formatMoney(total)), false);
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.7F, 1.0F); // Play success sound
            } else {
                player.sendMessage(Text.of("You don't have enough items to sell."), false);
            }
        } else { // Buy
            float playerBalance = PlayerBalanceManager.getBalance(playerUUID);
            if (playerBalance >= total) {
                int freeSlots = 0;
                for (ItemStack stack : player.getInventory().main) {
                    if (stack.isEmpty()) {
                        freeSlots++;
                    } else if (stack.getItem() == transactionItem && stack.getCount() < stack.getMaxCount()) {
                        freeSlots += (stack.getMaxCount() - stack.getCount()) / transactionItem.getMaxCount();
                    }
                }

                if (freeSlots >= (transactionQuantity / transactionItem.getMaxCount())) {
                    int remainingQuantity = transactionQuantity;
                    for (int i = 0; i < player.getInventory().size(); i++) {
                        ItemStack stack = player.getInventory().getStack(i);
                        if (stack.isEmpty()) {
                            player.getInventory().setStack(i, new ItemStack(transactionItem, Math.min(remainingQuantity, transactionItem.getMaxCount())));
                            remainingQuantity -= Math.min(remainingQuantity, transactionItem.getMaxCount());
                        } else if (stack.getItem() == transactionItem && stack.getCount() < stack.getMaxCount()) {
                            int addable = Math.min(remainingQuantity, stack.getMaxCount() - stack.getCount());
                            stack.increment(addable);
                            player.getInventory().setStack(i, stack);
                            remainingQuantity -= addable;
                        }
                        if (remainingQuantity == 0) break;
                    }

                    // Log the transaction
                    String transactionType = isSaleOrBuy(playerUUID) ? "sold" : "bought";
                    String logMessage = String.format("Player %s %s %s %s for $%.2f", player.getName().getString(), transactionType, transactionQuantity, transactionItem.getName().getString(), total);
                    TransactionLogger.log(logMessage);
                    setCurrentPage(playerUUID, 0);
                    PlayerBalanceManager.subtractBalance(playerUUID, total);
                    player.sendMessage(Text.of("Bought " + transactionQuantity + " " + transactionItem.getName().getString() + " for $" + formatMoney(total)), false);
                    player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.7F, 1.0F); // Play success sound
                } else {
                    player.sendMessage(Text.of("You don't have enough space in your inventory."), false);
                }
            } else {
                player.sendMessage(Text.of("You don't have enough money to buy this item."), false);
            }
        }
    }

    public static void sellInventoryWide(ServerPlayerEntity player, String transactionItemString) {
        UUID playerUUID = player.getUuid();
        Item transactionItem2 = Registries.ITEM.get(new Identifier(transactionItemString));
        float totalReimbursement = 0;
        int totalSold = 0;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == transactionItem2) {
                int stackCount = stack.getCount();
                totalSold += stackCount;
                player.getInventory().removeStack(i);
            }
        }

        for (ShopConfigManager.ShopItem item : getLastCategoryOpened(playerUUID).items) {
            if (item.id.equals(Registries.ITEM.getId(transactionItem2).toString())) {
                totalReimbursement = item.sellPrice * totalSold;
                break;
            }
        }

        if (totalSold > 0) {
            // Log the transaction
            String transactionType = isSaleOrBuy(playerUUID) ? "sold" : "bought";
            String logMessage = String.format("Player %s %s %s for $%.2f", player.getName().getString(), transactionType, transactionItem2.getName().getString(), getTransactionQuantity(playerUUID) * totalReimbursement);
            TransactionLogger.log(logMessage);
            PlayerBalanceManager.addBalance(playerUUID, totalReimbursement);
            player.sendMessage(Text.of("Sold " + totalSold + " " + transactionItem2.getName().getString() + " for $" + formatMoney(totalReimbursement)), false);
            player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.7F, 1.0F); // Play success sound
        } else {
            player.sendMessage(Text.of("No items to sell."), false);
        }
    }
}