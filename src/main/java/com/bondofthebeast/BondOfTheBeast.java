package com.bondofthebeast;


import com.bondofthebeast.block.ModBlockEntities;
import com.bondofthebeast.block.ModBlocks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BondOfTheBeast implements ModInitializer {
    public static final String MOD_ID = "bondofthebeast";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.registerModItems();
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerBlockEntities();
        ModItemGroups.registerItemGroups();
        ModPackets.registerC2SPackets();
        ModEvents.registerEvents();
        EntityConditions.register();
        BientityConditions.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ModCommands.registerCommands(dispatcher);
        });
    }

    public static Identifier identifier(String path) {
        return new Identifier(MOD_ID, path);
    }

    public static void grantAdvancement(ServerPlayerEntity player, String path) {
        if (player == null || player.getServer() == null) return;
        Advancement advancement = player.getServer().getAdvancementLoader().get(new Identifier(MOD_ID, path));
        if (advancement != null) {
            AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancement);
            if (!progress.isDone()) {
                for (String criterion : progress.getUnobtainedCriteria()) {
                    player.getAdvancementTracker().grantCriterion(advancement, criterion);
                }
            }
        }
    }
}