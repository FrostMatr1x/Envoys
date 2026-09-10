package com.frost.envoys.network.payload;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SkinInfoPayload(UUID npcUuid, String url, String hash, String model) implements CustomPacketPayload {
    public static final Type<SkinInfoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "skin_info"));
    public static final StreamCodec<FriendlyByteBuf, SkinInfoPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SkinInfoPayload::npcUuid,
            ByteBufCodecs.STRING_UTF8, SkinInfoPayload::url,
            ByteBufCodecs.STRING_UTF8, SkinInfoPayload::hash,
            ByteBufCodecs.STRING_UTF8, SkinInfoPayload::model,
            SkinInfoPayload::new
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}