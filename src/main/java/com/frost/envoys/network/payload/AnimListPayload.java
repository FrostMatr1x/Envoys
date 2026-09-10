package com.frost.envoys.network.payload;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AnimListPayload(List<AnimInfo> animations) implements CustomPacketPayload {
    public static final Type<AnimListPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "anim_list"));

    public static final StreamCodec<FriendlyByteBuf, AnimListPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            List<AnimInfo> list = payload.animations() != null ? payload.animations() : List.of();
            buf.writeVarInt(list.size());
            for (AnimInfo info : list) {
                AnimInfo.STREAM_CODEC.encode(buf, info);
            }
        },
        buf -> {
            int size = buf.readVarInt();
            List<AnimInfo> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(AnimInfo.STREAM_CODEC.decode(buf));
            }
            return new AnimListPayload(list);
        }
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record AnimInfo(String name, String hash) {
        public static final StreamCodec<FriendlyByteBuf, AnimInfo> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, AnimInfo::name,
                ByteBufCodecs.STRING_UTF8, AnimInfo::hash,
                AnimInfo::new
        );
    }
}
