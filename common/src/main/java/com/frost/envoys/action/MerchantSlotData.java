package com.frost.envoys.action;

import com.frost.envoys.platform.EnvoysPlatform;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.world.entity.player.Player;

public class MerchantSlotData {

    public static final Codec<MerchantSlotData> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .xmap(MerchantSlotData::new, MerchantSlotData::asMap);

    private final Map<String, Integer> unlocked;

    public MerchantSlotData() {
        this.unlocked = new HashMap<>();
    }

    public MerchantSlotData(Map<String, Integer> unlocked) {
        this.unlocked = new HashMap<>();
        if (unlocked != null) {
            for (Map.Entry<String, Integer> entry : unlocked.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                this.unlocked.put(entry.getKey(), Math.max(0, entry.getValue()));
            }
        }
    }

    public int getUnlocked(String npcUuid) {
        if (isBlank(npcUuid)) {
            return 0;
        }
        return this.unlocked.getOrDefault(npcUuid, 0);
    }

    public void addUnlocked(String npcUuid, int amount) {
        if (isBlank(npcUuid) || amount == 0) {
            return;
        }
        this.unlocked.put(npcUuid, Math.max(0, getUnlocked(npcUuid) + amount));
    }

    public Map<String, Integer> asMap() {
        return this.unlocked;
    }

    public static int getUnlocked(Player player, String npcUuid) {
        if (player == null || isBlank(npcUuid) || !EnvoysPlatform.hasMerchantSlots(player)) {
            return 0;
        }
        return EnvoysPlatform.merchantSlots(player).getUnlocked(npcUuid);
    }

    public static void addUnlocked(Player player, String npcUuid, int amount) {
        if (player == null) {
            return;
        }
        EnvoysPlatform.merchantSlots(player).addUnlocked(npcUuid, amount);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
