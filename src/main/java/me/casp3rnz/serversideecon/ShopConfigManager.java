package me.casp3rnz.serversideecon;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ShopConfigManager {

    private static final Path CONFIG_PATH = Paths.get("config/MMOShop.json");
    public static List<Category> categories = new ArrayList<>();

    public static void init() {
        try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
            Gson gson = new Gson();
            JsonObject configJson = gson.fromJson(reader, JsonObject.class);
            JsonArray categoriesJson = configJson.getAsJsonArray("categories");

            // Parse categories
            for (int i = 0; i < categoriesJson.size(); i++) {
                JsonObject categoryJson = categoriesJson.get(i).getAsJsonObject();
                String categoryName = categoryJson.get("name").getAsString();
                String representativeItem = categoryJson.get("representativeItem").getAsString();
                JsonArray itemsJson = categoryJson.getAsJsonArray("items");

                List<ShopItem> items = new ArrayList<>();
                for (int j = 0; j < itemsJson.size(); j++) {
                    JsonObject itemJson = itemsJson.get(j).getAsJsonObject();
                    String id = itemJson.get("id").getAsString();
                    String displayName = itemJson.get("displayName").getAsString();
                    int buyPrice = itemJson.get("buyPrice").getAsInt();
                    int sellPrice = itemJson.get("sellPrice").getAsInt();
                    items.add(new ShopItem(id, displayName, buyPrice, sellPrice));
                }

                categories.add(new Category(categoryName, representativeItem, items));
                //print success
                System.out.println("Loaded category: " + categoryName);
            }
        } catch (IOException | JsonParseException e) {
            System.out.println("No Config present");
            // If there's an issue loading the config, log it but continue with default values
        }
    }

    // reload config
    public static void reloadConfig() {
        categories.clear();
        init();
    }

    public static class ShopItem {
        public String id;
        public String displayName;
        public int buyPrice;
        public int sellPrice;

        public ShopItem(String id, String displayName, int buyPrice, int sellPrice) {
            this.id = id;
            this.displayName = displayName;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
        }
    }

    public static class Category {
        public String name;
        public String representativeItem;
        public List<ShopItem> items;

        public Category(String name, String representativeItem, List<ShopItem> items) {
            this.name = name;
            this.representativeItem = representativeItem;
            this.items = items;
        }
    }
}
