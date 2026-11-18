package com.frost.envoys.platform;

import java.nio.file.Path;

import com.frost.envoys.action.MerchantSlotData;
import com.frost.envoys.action.PlayerCheckpointData;
import com.frost.envoys.gui.menu.NPCMerchantMenu;
import com.frost.envoys.quest.PlayerQuestTracker;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class EnvoysPlatform {
    private static Platform platform;

    private EnvoysPlatform() {
    }

    public static void init(Platform platform) {
        EnvoysPlatform.platform = platform;
    }

    public static Platform get() {
        if (platform == null) {
            throw new IllegalStateException("Envoys platform is not initialized");
        }
        return platform;
    }

    public static boolean isClient() {
        return get().isClient();
    }

    public static boolean isModLoaded(String modId) {
        return get().isModLoaded(modId);
    }

    public static MinecraftServer getServer() {
        return get().getServer();
    }

    public static Path getGameDir() {
        return get().getGameDir();
    }

    public static Path getConfigDir() {
        return get().getConfigDir();
    }

    public static ConfigData config() {
        return get().getConfig();
    }


    public static PlayerQuestTracker quests(Player player) {
        return get().getAttachments().getQuests(player);
    }

    public static boolean hasQuests(Player player) {
        return get().getAttachments().hasQuests(player);
    }

    public static PlayerCheckpointData checkpoints(Player player) {
        return get().getAttachments().getCheckpoints(player);
    }

    public static boolean hasCheckpoints(Player player) {
        return get().getAttachments().hasCheckpoints(player);
    }

    public static MerchantSlotData merchantSlots(Player player) {
        return get().getAttachments().getMerchantSlots(player);
    }

    public static boolean hasMerchantSlots(Player player) {
        return get().getAttachments().hasMerchantSlots(player);
    }

    public static void executeTradeAll(ServerPlayer player, NPCMerchantMenu menu, int shopItem) {
        get().getBackpacks().executeTradeAll(player, menu, shopItem);
    }
}
