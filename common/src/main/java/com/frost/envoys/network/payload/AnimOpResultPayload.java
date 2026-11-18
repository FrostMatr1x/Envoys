package com.frost.envoys.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AnimOpResultPayload(boolean success, String op, String name, String message) implements CustomPacketPayload {
    public static final Type<AnimOpResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "anim_op_result"));

    public static final StreamCodec<FriendlyByteBuf, AnimOpResultPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AnimOpResultPayload::success,
            ByteBufCodecs.stringUtf8(32), AnimOpResultPayload::op,
            ByteBufCodecs.stringUtf8(256), AnimOpResultPayload::name,
            ByteBufCodecs.stringUtf8(256), AnimOpResultPayload::message,
            AnimOpResultPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
