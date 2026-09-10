package com.frost.envoys.network.payload;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestSkinPayload(UUID npcUuid) implements CustomPacketPayload {
    public static final Type<RequestSkinPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "request_skin"));
    public static final StreamCodec<FriendlyByteBuf, RequestSkinPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RequestSkinPayload::npcUuid, RequestSkinPayload::new
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}