package com.frost.envoys.network.payload;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectDialogAnswerPayload(UUID npcUuid, String nextActionId) implements CustomPacketPayload {
    public static final Type<SelectDialogAnswerPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "select_dialog_answer"));

    public static final StreamCodec<FriendlyByteBuf, SelectDialogAnswerPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SelectDialogAnswerPayload::npcUuid,
            ByteBufCodecs.STRING_UTF8, SelectDialogAnswerPayload::nextActionId,
            SelectDialogAnswerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}