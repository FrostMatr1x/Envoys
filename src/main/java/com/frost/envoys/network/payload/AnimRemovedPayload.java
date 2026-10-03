package com.frost.envoys.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AnimRemovedPayload(String name) implements CustomPacketPayload {
    public static final Type<AnimRemovedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "anim_removed"));

    public static final StreamCodec<FriendlyByteBuf, AnimRemovedPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), AnimRemovedPayload::name,
            AnimRemovedPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
