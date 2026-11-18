package com.frost.envoys.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TradeAllPayload(int shopItem) implements CustomPacketPayload {
    public static final Type<TradeAllPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "trade_all"));

    public static final StreamCodec<FriendlyByteBuf, TradeAllPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TradeAllPayload::shopItem,
            TradeAllPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
