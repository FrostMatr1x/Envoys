package com.frost.envoys.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestAnimListPayload() implements CustomPacketPayload {
    public static final Type<RequestAnimListPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "request_anim_list"));
    public static final StreamCodec<FriendlyByteBuf, RequestAnimListPayload> CODEC = StreamCodec.unit(new RequestAnimListPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
