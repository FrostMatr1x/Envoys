package com.frost.envoys.network.payload;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenDialogPayload(
    UUID npcUuid, 
    String actionId, 
    String npcName, 
    Component npcMessage, 
    Map<String, String> answers
) implements CustomPacketPayload {

    public static final Type<OpenDialogPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "open_dialog"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDialogPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            UUIDUtil.STREAM_CODEC.encode(buf, payload.npcUuid);

            ByteBufCodecs.STRING_UTF8.encode(buf, payload.actionId != null ? payload.actionId : "");
            ByteBufCodecs.STRING_UTF8.encode(buf, payload.npcName != null ? payload.npcName : "NPC");

            Component msg = payload.npcMessage != null ? payload.npcMessage : Component.empty();
            ComponentSerialization.STREAM_CODEC.encode(buf, msg);

            if (payload.answers == null) {
                buf.writeInt(0);
            } else {
                buf.writeInt(payload.answers.size());
                payload.answers.forEach((key, val) -> {
                    buf.writeUtf(key != null ? key : "");
                    buf.writeUtf(val != null ? val : "");
                });
            }
        },
        buf -> {
            UUID uuid = UUIDUtil.STREAM_CODEC.decode(buf);
            String actionId = ByteBufCodecs.STRING_UTF8.decode(buf);
            String name = ByteBufCodecs.STRING_UTF8.decode(buf);
            Component msg = ComponentSerialization.STREAM_CODEC.decode(buf);
            
            int size = buf.readInt();
            Map<String, String> answers = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                String key = buf.readUtf();
                String val = buf.readUtf();
                answers.put(key, val);
            }
            return new OpenDialogPayload(uuid, actionId, name, msg, answers);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}