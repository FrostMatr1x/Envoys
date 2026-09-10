package com.frost.envoys.network.payload;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record OpenSettingGuiPayload(
    boolean isCreativeTuner,
    UUID entityId,
    String name,
    float size,
    float speed,
    float hp,
    Vec3 holdPosition,
    boolean isVisible,
    boolean isHoldPosEnabled,
    boolean canTakeDamage,
    boolean useGravity,
    boolean creativeTunerOnly,
    String jsonScript,
    String emote
) implements CustomPacketPayload {

    public static final Type<OpenSettingGuiPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "open_setting_gui"));

    public static final StreamCodec<FriendlyByteBuf, OpenSettingGuiPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeBoolean(payload.isCreativeTuner);
            UUIDUtil.STREAM_CODEC.encode(buf, payload.entityId);
            buf.writeUtf(payload.name);
            buf.writeFloat(payload.size);
            buf.writeFloat(payload.speed);
            buf.writeFloat(payload.hp);
            buf.writeDouble(payload.holdPosition.x);
            buf.writeDouble(payload.holdPosition.y);
            buf.writeDouble(payload.holdPosition.z);
            buf.writeBoolean(payload.isVisible);
            buf.writeBoolean(payload.isHoldPosEnabled);
            buf.writeBoolean(payload.canTakeDamage);
            buf.writeBoolean(payload.useGravity);
            buf.writeBoolean(payload.creativeTunerOnly);
            buf.writeUtf(payload.jsonScript != null ? payload.jsonScript : "", 1048576);
            buf.writeUtf(payload.emote != null ? payload.emote : "");
        },
        buf -> new OpenSettingGuiPayload(
            buf.readBoolean(),
            UUIDUtil.STREAM_CODEC.decode(buf),
            buf.readUtf(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readUtf(1048576),
            buf.readUtf()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}