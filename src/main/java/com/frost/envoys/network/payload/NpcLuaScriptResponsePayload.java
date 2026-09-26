package com.frost.envoys.network.payload;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NpcLuaScriptResponsePayload(UUID npcUuid, String fileName, String source, boolean exists, Component message)
        implements CustomPacketPayload {

    public static final Type<NpcLuaScriptResponsePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "npc_lua_script_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NpcLuaScriptResponsePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, payload.npcUuid);
                ByteBufCodecs.stringUtf8(256).encode(buf, payload.fileName != null ? payload.fileName : "main.lua");
                ByteBufCodecs.stringUtf8(1048576).encode(buf, payload.source != null ? payload.source : "");
                buf.writeBoolean(payload.exists);
                ComponentSerialization.STREAM_CODEC.encode(buf, payload.message == null ? Component.empty() : payload.message);
            },
            buf -> new NpcLuaScriptResponsePayload(
                    UUIDUtil.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.stringUtf8(256).decode(buf),
                    ByteBufCodecs.stringUtf8(1048576).decode(buf),
                    buf.readBoolean(),
                    ComponentSerialization.STREAM_CODEC.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
