package com.frost.envoys.network.payload;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SkinDataPayload(UUID npcUuid, byte[] pngData) implements CustomPacketPayload {
    public static final Type<SkinDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "skin_data"));
    public static final StreamCodec<FriendlyByteBuf, SkinDataPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SkinDataPayload::npcUuid,
            ByteBufCodecs.BYTE_ARRAY, SkinDataPayload::pngData,
            SkinDataPayload::new
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}