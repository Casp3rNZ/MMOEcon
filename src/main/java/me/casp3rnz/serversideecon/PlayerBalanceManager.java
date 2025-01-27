package me.casp3rnz.serversideecon;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class PlayerBalanceManager {
    static final File BALANCE_FILE = new File("economy_balances.dat");
    final Map<String, Float> balances = new HashMap<>();

    public void init() {
        // initialize balances from file
        if (BALANCE_FILE.exists()) {
            try (DataInputStream in = new DataInputStream(new FileInputStream(BALANCE_FILE))) {
                int count = in.readInt();
                for (int i = 0; i < count; i++) {
                    balances.put(in.readUTF(), in.readFloat());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Initialize a player's balance with a default value on player join
    public void initializePlayer(PlayerEntity player) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerEntity joinedPlayer = handler.getPlayer();
            if (!balances.containsKey(joinedPlayer.getUuid().toString())) {
                balances.put(joinedPlayer.getUuid().toString(), 500f); // Start with $500
            }
            saveBalances();
        });
    }

    public void saveBalances() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(BALANCE_FILE))) {
            out.writeInt(balances.size());
            for (Map.Entry<String, Float> entry : balances.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeFloat(entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public float getBalance(String player) {
        return balances.getOrDefault(player, 500f);  // Default balance is 500
    }

    public void setBalance(String player, float amount) {
        balances.put(player, amount);
        saveBalances();
    }

    public void addBalance(String player, float amount) {
        balances.put(player, getBalance(player) + amount);
        saveBalances();
    }

    public void subtractBalance(String player, float amount) {
        balances.put(player, MathHelper.clamp(getBalance(player) - amount, 0, Float.MAX_VALUE));
        saveBalances();
    }

    public Map<String, Float> getBalances() {
        return balances;
    }
}
