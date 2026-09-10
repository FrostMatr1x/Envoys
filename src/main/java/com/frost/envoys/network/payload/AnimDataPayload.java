package com.frost.envoys.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AnimDataPayload(String name, byte[] jsonData) implements CustomPacketPayload {
    public static final Type<AnimDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "anim_data"));
    public static final StreamCodec<FriendlyByteBuf, AnimDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AnimDataPayload::name,
            ByteBufCodecs.BYTE_ARRAY, AnimDataPayload::jsonData,
            AnimDataPayload::new
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
