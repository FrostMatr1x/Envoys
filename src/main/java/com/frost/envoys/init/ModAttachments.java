package com.frost.envoys.init;

import com.frost.envoys.Envoys;
import com.frost.envoys.action.MerchantSlotData;
import com.frost.envoys.action.PlayerCheckpointData;
import com.frost.envoys.quest.PlayerQuestTracker;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {

    private ModAttachments() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Envoys.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerQuestTracker>> PLAYER_QUESTS =
            ATTACHMENT_TYPES.register("player_quests", () -> AttachmentType
                    .builder(() -> new PlayerQuestTracker())
                    .serialize(PlayerQuestTracker.CODEC)
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerCheckpointData>> PLAYER_CHECKPOINTS =
            ATTACHMENT_TYPES.register("player_checkpoints", () -> AttachmentType
                    .builder(() -> new PlayerCheckpointData())
                    .serialize(PlayerCheckpointData.CODEC)
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<MerchantSlotData>> PLAYER_MERCHANT_SLOTS =
            ATTACHMENT_TYPES.register("player_merchant_slots", () -> AttachmentType
                    .builder(() -> new MerchantSlotData())
                    .serialize(MerchantSlotData.CODEC)
                    .copyOnDeath()
                    .build());
}
