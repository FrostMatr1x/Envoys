package com.frost.envoys.platform;

import com.frost.envoys.action.MerchantSlotData;
import com.frost.envoys.action.PlayerCheckpointData;
import com.frost.envoys.quest.PlayerQuestTracker;

import net.minecraft.world.entity.player.Player;

public interface AttachmentAccess {
    PlayerQuestTracker getQuests(Player player);

    boolean hasQuests(Player player);

    PlayerCheckpointData getCheckpoints(Player player);

    boolean hasCheckpoints(Player player);

    MerchantSlotData getMerchantSlots(Player player);

    boolean hasMerchantSlots(Player player);
}
