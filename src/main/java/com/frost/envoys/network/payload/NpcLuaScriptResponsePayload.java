package com.frost.envoys.network.payload;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NpcLuaScriptResponsePayload(UUID npcUuid, String fileName, String source, boolean exists, String message)
        implements CustomPacketPayload {

    public static final Type<NpcLuaScriptResponsePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "npc_lua_script_response"));

    public static final StreamCodec<FriendlyByteBuf, NpcLuaScriptResponsePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, payload.npcUuid);
                ByteBufCodecs.stringUtf8(256).encode(buf, payload.fileName != null ? payload.fileName : "main.lua");
                ByteBufCodecs.stringUtf8(1048576).encode(buf, payload.source != null ? payload.source : "");
                buf.writeBoolean(payload.exists);
                buf.writeUtf(payload.message != null ? payload.message : "", 4096);
            },
            buf -> new NpcLuaScriptResponsePayload(
                    UUIDUtil.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.stringUtf8(256).decode(buf),
                    ByteBufCodecs.stringUtf8(1048576).decode(buf),
                    buf.readBoolean(),
                    buf.readUtf(4096))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
