package com.frost.envoys.network.payload;

import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NPCInteractPayload(UUID entityId) implements CustomPacketPayload {
    
    public static final Type<NPCInteractPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "open_interact_gui"));

    public static final StreamCodec<FriendlyByteBuf, NPCInteractPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, NPCInteractPayload::entityId,
            NPCInteractPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
