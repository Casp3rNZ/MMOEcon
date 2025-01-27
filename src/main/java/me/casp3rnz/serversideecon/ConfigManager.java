package me.casp3rnz.serversideecon;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {

    private static final Path CONFIG_PATH = Paths.get("config/MMOEcon.json");
    public static float playtimeReward = 200f;  // Default values
    public static float killReward = 2.50f;       // Default values
    public static long playtimeInterval = 36000L;
    public static boolean enablePlaytimeRewards = true;
    public static boolean enableKillRewards = true;
    public static float defaultStartAmount = 500;
    public static boolean enableGUIShop = true;

    // Load the configuration file
    public static void loadConfig() {
        if (!CONFIG_PATH.toFile().exists()) {
            try {
                CONFIG_PATH.toFile().createNewFile();
                Gson gson = new Gson();
                JsonObject configJson = new JsonObject();
                configJson.addProperty("playtimeReward", playtimeReward);
                configJson.addProperty("killReward", killReward);
                configJson.addProperty("playtimeInterval", playtimeInterval);
                configJson.addProperty("enablePlaytimeRewards", enablePlaytimeRewards);
                configJson.addProperty("enableKillRewards", enableKillRewards);
                configJson.addProperty("defaultStartAmount", defaultStartAmount);
                configJson.addProperty("enableGUIShop", enableGUIShop);

                try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
                    gson.toJson(configJson, writer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
            Gson gson = new Gson();
            JsonObject configJson = gson.fromJson(reader, JsonObject.class);

            playtimeReward = configJson.get("playtimeReward").getAsFloat();
            killReward = configJson.get("killReward").getAsFloat();
            playtimeInterval = configJson.get("playtimeInterval").getAsLong();
            enablePlaytimeRewards = configJson.get("enablePlaytimeRewards").getAsBoolean();
            enableKillRewards = configJson.get("enableKillRewards").getAsBoolean();
            defaultStartAmount = configJson.get("defaultStartAmount").getAsFloat();
            enableGUIShop = configJson.get("enableGUIShop").getAsBoolean();
        } catch (IOException | JsonParseException e) {
            e.printStackTrace();
        }
    }
}