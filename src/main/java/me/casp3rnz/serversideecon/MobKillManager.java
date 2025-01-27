package me.casp3rnz.serversideecon;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class MobKillManager {

    // Call this to initialize the mob death listener
    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!entity.isAlive() && player instanceof ServerPlayerEntity) {
                serverside.balanceManager.addBalance(player.getName().getString(), ConfigManager.killReward);
                player.sendMessage(Text.of("You've earned $" + ConfigManager.killReward + " for killing a mob!"), false);
            }
            return ActionResult.PASS;
        });
    }
}