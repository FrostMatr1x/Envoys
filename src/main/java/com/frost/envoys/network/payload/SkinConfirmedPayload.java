package com.frost.envoys.network.payload;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SkinConfirmedPayload(UUID npcUuid, String hash) implements CustomPacketPayload {
    public static final Type<SkinConfirmedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "skin_confirmed"));
    public static final StreamCodec<FriendlyByteBuf, SkinConfirmedPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SkinConfirmedPayload::npcUuid,
            ByteBufCodecs.STRING_UTF8, SkinConfirmedPayload::hash,
            SkinConfirmedPayload::new
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}