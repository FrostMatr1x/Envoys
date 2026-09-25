package com.frost.envoys.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LuaScriptUploadResultPayload(boolean success, String message) implements CustomPacketPayload {
    public static final Type<LuaScriptUploadResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "lua_script_upload_result"));

    public static final StreamCodec<FriendlyByteBuf, LuaScriptUploadResultPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.success);
                buf.writeUtf(payload.message != null ? payload.message : "", 4096);
            },
            buf -> new LuaScriptUploadResultPayload(buf.readBoolean(), buf.readUtf(4096))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
