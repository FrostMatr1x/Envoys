package com.frost.envoys.network.payload;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record SaveNPCPassportPayload(
    UUID npcUuid,
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
    boolean lookLocked,
    String emote
) implements CustomPacketPayload {

    public static final Type<SaveNPCPassportPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "save_npc_passport"));

    public static final StreamCodec<FriendlyByteBuf, SaveNPCPassportPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            UUIDUtil.STREAM_CODEC.encode(buf, payload.npcUuid);
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
            buf.writeBoolean(payload.lookLocked);
            buf.writeUtf(payload.emote != null ? payload.emote : "");
        },
        buf -> new SaveNPCPassportPayload(
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
            buf.readBoolean(),
            buf.readUtf()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}