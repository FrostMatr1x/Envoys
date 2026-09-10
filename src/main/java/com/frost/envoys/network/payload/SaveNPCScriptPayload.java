package com.frost.envoys.network.payload;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SaveNPCScriptPayload(UUID npcId, String jsonScript) implements CustomPacketPayload {
    public static final Type<SaveNPCScriptPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "save_npc_script"));

    public static final StreamCodec<FriendlyByteBuf, SaveNPCScriptPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SaveNPCScriptPayload::npcId,
            ByteBufCodecs.stringUtf8(1048576), SaveNPCScriptPayload::jsonScript,
            SaveNPCScriptPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}