package me.casp3rnz.serversideecon;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public class PlaytimeManager {
    private static final Map<ServerPlayerEntity, Long> playerPlaytime = new HashMap<>();

    // Call this to initialize the tick event listener
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                // Track playtime in ticks
                long timePlayed = playerPlaytime.getOrDefault(player, 0L);
                timePlayed++;

                // Update playtime for the player
                playerPlaytime.put(player, timePlayed);

                // Reward player if they've been online for the specified playtime interval
                if (timePlayed % ConfigManager.playtimeInterval == 0) {
                    rewardPlaytime(player);
                }
            }
        });
    }

    // Reward money for playtime
    private static void rewardPlaytime(ServerPlayerEntity player) {
        // Add money to the player's balance based on the config
        serverside.balanceManager.addBalance(player.getName().getString(), ConfigManager.playtimeReward);

        // Send feedback to player
        player.sendMessage(Text.of("You've earned $" + ConfigManager.playtimeReward + " for 30 minutes of playtime!"), false);
    }
}
