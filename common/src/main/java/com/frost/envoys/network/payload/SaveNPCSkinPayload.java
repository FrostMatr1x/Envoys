package com.frost.envoys.network.payload;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SaveNPCSkinPayload(UUID npcUuid, String skinType, String skinValue, String model,
                                 byte[] pngData) implements CustomPacketPayload {
    public SaveNPCSkinPayload {
        if (pngData == null) {
            pngData = new byte[0];
        }
    }

    public static final Type<SaveNPCSkinPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "save_npc_skin"));
    public static final StreamCodec<FriendlyByteBuf, SaveNPCSkinPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SaveNPCSkinPayload::npcUuid,
            ByteBufCodecs.STRING_UTF8, SaveNPCSkinPayload::skinType,
            ByteBufCodecs.STRING_UTF8, SaveNPCSkinPayload::skinValue,
            ByteBufCodecs.STRING_UTF8, SaveNPCSkinPayload::model,
            ByteBufCodecs.BYTE_ARRAY, SaveNPCSkinPayload::pngData,
            SaveNPCSkinPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}