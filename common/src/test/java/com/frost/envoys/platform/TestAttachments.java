package com.frost.envoys.platform;

import com.frost.envoys.action.MerchantSlotData;
import com.frost.envoys.action.PlayerCheckpointData;
import com.frost.envoys.quest.PlayerQuestTracker;

import net.minecraft.world.entity.player.Player;

import java.util.IdentityHashMap;
import java.util.Map;

public class TestAttachments implements AttachmentAccess {

    private final Map<Player, PlayerQuestTracker> quests = new IdentityHashMap<>();
    private final Map<Player, PlayerCheckpointData> checkpoints = new IdentityHashMap<>();
    private final Map<Player, MerchantSlotData> merchantSlots = new IdentityHashMap<>();

    @Override
    public PlayerQuestTracker getQuests(Player player) {
        return quests.computeIfAbsent(player, key -> new PlayerQuestTracker());
    }

    @Override
    public boolean hasQuests(Player player) {
        return quests.containsKey(player);
    }

    @Override
    public PlayerCheckpointData getCheckpoints(Player player) {
        return checkpoints.computeIfAbsent(player, key -> new PlayerCheckpointData());
    }

    @Override
    public boolean hasCheckpoints(Player player) {
        return checkpoints.containsKey(player);
    }

    @Override
    public MerchantSlotData getMerchantSlots(Player player) {
        return merchantSlots.computeIfAbsent(player, key -> new MerchantSlotData());
    }

    @Override
    public boolean hasMerchantSlots(Player player) {
        return merchantSlots.containsKey(player);
    }

    public void reset() {
        quests.clear();
        checkpoints.clear();
        merchantSlots.clear();
    }
}
