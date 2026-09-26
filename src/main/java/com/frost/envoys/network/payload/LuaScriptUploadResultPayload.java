package com.frost.envoys.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LuaScriptUploadResultPayload(boolean success, Component message) implements CustomPacketPayload {
    public static final Type<LuaScriptUploadResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "lua_script_upload_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LuaScriptUploadResultPayload> CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.BOOL, LuaScriptUploadResultPayload::success,
            ComponentSerialization.STREAM_CODEC, LuaScriptUploadResultPayload::message,
            LuaScriptUploadResultPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
