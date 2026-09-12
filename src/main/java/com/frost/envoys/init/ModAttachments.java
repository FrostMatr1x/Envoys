package com.frost.envoys.init;

import com.frost.envoys.Envoys;
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
}
