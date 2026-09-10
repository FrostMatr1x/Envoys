package com.frost.envoys.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestAnimPayload(String name) implements CustomPacketPayload {
    public static final Type<RequestAnimPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "request_anim"));
    public static final StreamCodec<FriendlyByteBuf, RequestAnimPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RequestAnimPayload::name,
            RequestAnimPayload::new
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
