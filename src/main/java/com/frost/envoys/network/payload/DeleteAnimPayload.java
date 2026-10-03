package com.frost.envoys.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeleteAnimPayload(String name) implements CustomPacketPayload {
    public static final Type<DeleteAnimPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "delete_anim"));

    public static final StreamCodec<FriendlyByteBuf, DeleteAnimPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), DeleteAnimPayload::name,
            DeleteAnimPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
