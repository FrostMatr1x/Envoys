package com.frost.envoys.neoforge;

import com.frost.envoys.EnvoysConstants;
import com.frost.envoys.action.MerchantSlotData;
import com.frost.envoys.action.PlayerCheckpointData;
import com.frost.envoys.platform.AttachmentAccess;
import com.frost.envoys.quest.PlayerQuestTracker;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class NeoForgeAttachments implements AttachmentAccess {
    public static final NeoForgeAttachments INSTANCE = new NeoForgeAttachments();

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EnvoysConstants.MODID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerQuestTracker>> PLAYER_QUESTS =
            ATTACHMENT_TYPES.register("player_quests", () -> AttachmentType
                    .builder(() -> new PlayerQuestTracker())
                    .serialize(PlayerQuestTracker.CODEC)
                    .copyOnDeath()
                    .build());

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerCheckpointData>> PLAYER_CHECKPOINTS =
            ATTACHMENT_TYPES.register("player_checkpoints", () -> AttachmentType
                    .builder(() -> new PlayerCheckpointData())
                    .serialize(PlayerCheckpointData.CODEC)
                    .copyOnDeath()
                    .build());

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<MerchantSlotData>> PLAYER_MERCHANT_SLOTS =
            ATTACHMENT_TYPES.register("player_merchant_slots", () -> AttachmentType
                    .builder(() -> new MerchantSlotData())
                    .serialize(MerchantSlotData.CODEC)
                    .copyOnDeath()
                    .build());

    private NeoForgeAttachments() {
    }

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }

    @Override
    public PlayerQuestTracker getQuests(Player player) {
        return player.getData(PLAYER_QUESTS);
    }

    @Override
    public boolean hasQuests(Player player) {
        return player.hasData(PLAYER_QUESTS);
    }

    @Override
    public PlayerCheckpointData getCheckpoints(Player player) {
        return player.getData(PLAYER_CHECKPOINTS);
    }

    @Override
    public boolean hasCheckpoints(Player player) {
        return player.hasData(PLAYER_CHECKPOINTS);
    }

    @Override
    public MerchantSlotData getMerchantSlots(Player player) {
        return player.getData(PLAYER_MERCHANT_SLOTS);
    }

    @Override
    public boolean hasMerchantSlots(Player player) {
        return player.hasData(PLAYER_MERCHANT_SLOTS);
    }
}
