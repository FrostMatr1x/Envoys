package com.frost.envoys.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PushAnimPayload(String name, byte[] jsonData, String hash) implements CustomPacketPayload {
    public static final Type<PushAnimPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "push_anim"));

    public static final StreamCodec<FriendlyByteBuf, PushAnimPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), PushAnimPayload::name,
            ByteBufCodecs.BYTE_ARRAY, PushAnimPayload::jsonData,
            ByteBufCodecs.stringUtf8(64), PushAnimPayload::hash,
            PushAnimPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
