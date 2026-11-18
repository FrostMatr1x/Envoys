package com.frost.envoys.fabric;

import com.frost.envoys.EnvoysConstants;
import com.frost.envoys.action.MerchantSlotData;
import com.frost.envoys.action.PlayerCheckpointData;
import com.frost.envoys.platform.AttachmentAccess;
import com.frost.envoys.quest.PlayerQuestTracker;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class FabricAttachments implements AttachmentAccess {
    public static final FabricAttachments INSTANCE = new FabricAttachments();

    private static final AttachmentType<PlayerQuestTracker> PLAYER_QUESTS =
            AttachmentRegistry.<PlayerQuestTracker>builder()
                    .initializer(PlayerQuestTracker::new)
                    .persistent(PlayerQuestTracker.CODEC)
                    .copyOnDeath()
                    .buildAndRegister(id("player_quests"));

    private static final AttachmentType<PlayerCheckpointData> PLAYER_CHECKPOINTS =
            AttachmentRegistry.<PlayerCheckpointData>builder()
                    .initializer(PlayerCheckpointData::new)
                    .persistent(PlayerCheckpointData.CODEC)
                    .copyOnDeath()
                    .buildAndRegister(id("player_checkpoints"));

    private static final AttachmentType<MerchantSlotData> PLAYER_MERCHANT_SLOTS =
            AttachmentRegistry.<MerchantSlotData>builder()
                    .initializer(MerchantSlotData::new)
                    .persistent(MerchantSlotData.CODEC)
                    .copyOnDeath()
                    .buildAndRegister(id("player_merchant_slots"));

    private FabricAttachments() {
    }

    static void register() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(EnvoysConstants.MODID, path);
    }

    @Override
    public PlayerQuestTracker getQuests(Player player) {
        return player.getAttachedOrCreate(PLAYER_QUESTS);
    }

    @Override
    public boolean hasQuests(Player player) {
        return player.getAttached(PLAYER_QUESTS) != null;
    }

    @Override
    public PlayerCheckpointData getCheckpoints(Player player) {
        return player.getAttachedOrCreate(PLAYER_CHECKPOINTS);
    }

    @Override
    public boolean hasCheckpoints(Player player) {
        return player.getAttached(PLAYER_CHECKPOINTS) != null;
    }

    @Override
    public MerchantSlotData getMerchantSlots(Player player) {
        return player.getAttachedOrCreate(PLAYER_MERCHANT_SLOTS);
    }

    @Override
    public boolean hasMerchantSlots(Player player) {
        return player.getAttached(PLAYER_MERCHANT_SLOTS) != null;
    }
}
