package me.casp3rnz.serversideecon;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerBalanceManager {
    static final File BALANCE_FILE = new File("MMOEconomy_balances.dat");
    static final Map<UUID, Float> balances = new HashMap<>();

    public void init() {
        // Initialize balances from file
        if (BALANCE_FILE.exists()) {
            try (DataInputStream in = new DataInputStream(new FileInputStream(BALANCE_FILE))) {
                int count = in.readInt();
                for (int i = 0; i < count; i++) {
                    UUID uuid = UUID.fromString(in.readUTF());
                    float balance = in.readFloat();
                    balances.put(uuid, balance);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server1) -> {
            PlayerEntity joinedPlayer = handler.getPlayer();
            UUID playerUUID = joinedPlayer.getUuid();
            if (!balances.containsKey(playerUUID)) {
                balances.put(playerUUID, 500f); // Start with $500
            }
            saveBalances();
        });
    }

    public static void saveBalances() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(BALANCE_FILE))) {
            out.writeInt(balances.size());
            for (Map.Entry<UUID, Float> entry : balances.entrySet()) {
                out.writeUTF(entry.getKey().toString());
                out.writeFloat(entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static float getBalance(UUID playerUUID) {
        return balances.getOrDefault(playerUUID, 500f);  // Default balance is 500
    }

    public static void setBalance(UUID playerUUID, float amount) {
        balances.put(playerUUID, amount);
        saveBalances();
    }

    public static void addBalance(UUID playerUUID, float amount) {
        balances.put(playerUUID, getBalance(playerUUID) + amount);
        saveBalances();
    }

    public static void subtractBalance(UUID playerUUID, float amount) {
        balances.put(playerUUID, MathHelper.clamp(getBalance(playerUUID) - amount, 0, Float.MAX_VALUE));
        saveBalances();
    }

    public static Map<UUID, Float> getBalances() {
        return balances;
    }
}