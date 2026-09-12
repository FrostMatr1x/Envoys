package com.frost.envoys.action;

import java.util.HashMap;
import java.util.Map;

import com.frost.envoys.init.ModAttachments;
import com.mojang.serialization.Codec;

import net.minecraft.world.entity.player.Player;

public class PlayerCheckpointData {

    public static final Codec<PlayerCheckpointData> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.STRING))
                    .xmap(PlayerCheckpointData::new, PlayerCheckpointData::asMap);

    private final Map<String, Map<String, String>> checkpoints;

    public PlayerCheckpointData() {
        this.checkpoints = new HashMap<>();
    }

    public PlayerCheckpointData(Map<String, Map<String, String>> checkpoints) {
        this.checkpoints = new HashMap<>();
        if (checkpoints != null) {
            for (Map.Entry<String, Map<String, String>> entry : checkpoints.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                this.checkpoints.put(entry.getKey(), new HashMap<>(entry.getValue()));
            }
        }
    }

    public void put(String npcUuid, String saveId, String checkpointUuid) {
        if (isBlank(npcUuid) || isBlank(saveId) || isBlank(checkpointUuid)) {
            return;
        }
        this.checkpoints.computeIfAbsent(npcUuid, key -> new HashMap<>()).put(saveId, checkpointUuid);
    }

    public String get(String npcUuid, String saveId) {
        if (isBlank(npcUuid) || isBlank(saveId)) {
            return null;
        }
        Map<String, String> bySaveId = this.checkpoints.get(npcUuid);
        return bySaveId == null ? null : bySaveId.get(saveId);
    }

    public Map<String, Map<String, String>> asMap() {
        return this.checkpoints;
    }

    public static void setCheckpoint(Player player, String npcUuid, String saveId, String checkpointUuid) {
        if (player == null) {
            return;
        }
        player.getData(ModAttachments.PLAYER_CHECKPOINTS).put(npcUuid, saveId, checkpointUuid);
    }

    public static String getCheckpoint(Player player, String npcUuid, String saveId) {
        if (player == null || isBlank(npcUuid) || isBlank(saveId) || !player.hasData(ModAttachments.PLAYER_CHECKPOINTS)) {
            return null;
        }
        return player.getData(ModAttachments.PLAYER_CHECKPOINTS).get(npcUuid, saveId);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
