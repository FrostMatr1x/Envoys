package com.frost.envoys.network.payload;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncPlayerQuestsPayload(List<ClientQuestEntry> quests) implements CustomPacketPayload {

    public static final Type<SyncPlayerQuestsPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("envoys", "sync_player_quests"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerQuestsPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            List<ClientQuestEntry> list = payload.quests == null ? List.of() : payload.quests;
            buf.writeInt(list.size());
            for (ClientQuestEntry entry : list) {
                ClientQuestEntry.STREAM_CODEC.encode(buf, entry);
            }
        },
        buf -> {
            int size = buf.readInt();
            List<ClientQuestEntry> list = new ArrayList<>(Math.max(0, size));
            for (int i = 0; i < size; i++) {
                list.add(ClientQuestEntry.STREAM_CODEC.decode(buf));
            }
            return new SyncPlayerQuestsPayload(list);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
