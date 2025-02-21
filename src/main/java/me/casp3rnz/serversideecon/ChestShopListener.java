package me.casp3rnz.serversideecon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class ChestShopListener {

    private static final Set<Block> allowedContainers = new HashSet<>();
    private static final Set<Item> allowedSigns = new HashSet<>();

    public static void loadConfig() {
        try (FileReader reader = new FileReader("config/MMOEconChestshopConfig.json")) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray containers = json.getAsJsonArray("allowed_containers");
            for (JsonElement element : containers) {
                allowedContainers.add(Registries.BLOCK.get(new Identifier(element.getAsString())));
            }
            JsonArray signs = json.getAsJsonArray("allowed_signs");
            for (JsonElement element : signs) {
                allowedSigns.add(Registries.ITEM.get(new Identifier(element.getAsString())));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient && hand == Hand.MAIN_HAND) {
                ItemStack heldItem = player.getStackInHand(hand);
                if (allowedSigns.contains(heldItem.getItem())) {
                    BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());
                    if (blockEntity instanceof ChestBlockEntity) {
                        Block block = world.getBlockState(hitResult.getBlockPos()).getBlock();
                        if (allowedContainers.contains(block)) {
                            createShop((ServerPlayerEntity) player, (ChestBlockEntity) blockEntity);
                            return ActionResult.SUCCESS;
                        }
                    }
                }
            }
            return ActionResult.PASS;
        });
    }

    private static void createShop(ServerPlayerEntity player, ChestBlockEntity chest) {
        // Implement shop creation logic here
        player.sendMessage(Text.of("Chest shop created!"), false);
    }
}