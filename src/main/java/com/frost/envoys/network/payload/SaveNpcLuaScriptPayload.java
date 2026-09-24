package com.frost.envoys.network.payload;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SaveNpcLuaScriptPayload(UUID npcId, String fileName, String source) implements CustomPacketPayload {
    public static final Type<SaveNpcLuaScriptPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "save_npc_lua_script"));

    public static final StreamCodec<FriendlyByteBuf, SaveNpcLuaScriptPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SaveNpcLuaScriptPayload::npcId,
            ByteBufCodecs.stringUtf8(256), SaveNpcLuaScriptPayload::fileName,
            ByteBufCodecs.stringUtf8(1048576), SaveNpcLuaScriptPayload::source,
            SaveNpcLuaScriptPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
