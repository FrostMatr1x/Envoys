package com.frost.envoys.network.payload;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FetchNpcLuaScriptPayload(UUID npcUuid) implements CustomPacketPayload {
    public static final Type<FetchNpcLuaScriptPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "fetch_npc_lua_script"));

    public static final StreamCodec<FriendlyByteBuf, FetchNpcLuaScriptPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, FetchNpcLuaScriptPayload::npcUuid,
            FetchNpcLuaScriptPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
