package me.casp3rnz.serversideecon;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class serverside implements ModInitializer {
    public static final String MOD_ID = "mmoecon";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static PlayerBalanceManager balanceManager = new PlayerBalanceManager();

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Load the configuration values
        ConfigManager.loadConfig();

        // Initialize the balance manager
        balanceManager.init();

        if (ConfigManager.enableGUIShop) {
            // Initialize GUI Shop
            ShopConfigManager.init();
        }

        // Initialize Economy commands
        EconomyCommands.registerCommands();

        LOGGER.info("Casp3rNZ's MMO Economy Mod Initialized!");
    }

}