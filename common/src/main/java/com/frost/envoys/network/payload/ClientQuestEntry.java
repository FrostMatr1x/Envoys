package com.frost.envoys.network.payload;

import com.frost.envoys.quest.QuestType;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ClientQuestEntry(
    String questUuid,
    String title,
    String npcName,
    boolean visibleInGui,
    QuestType type,
    String itemId,
    int targetProgress,
    int currentProgress,
    boolean completed
) {

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientQuestEntry> STREAM_CODEC = StreamCodec.of(
        (buf, entry) -> {
            ByteBufCodecs.STRING_UTF8.encode(buf, entry.questUuid == null ? "" : entry.questUuid);
            ByteBufCodecs.STRING_UTF8.encode(buf, entry.title == null ? "" : entry.title);
            ByteBufCodecs.STRING_UTF8.encode(buf, entry.npcName == null ? "" : entry.npcName);
            buf.writeBoolean(entry.visibleInGui);
            ByteBufCodecs.STRING_UTF8.encode(buf, entry.type == null ? QuestType.BOOLEAN.name() : entry.type.name());
            ByteBufCodecs.STRING_UTF8.encode(buf, entry.itemId == null ? "" : entry.itemId);
            buf.writeInt(entry.targetProgress);
            buf.writeInt(entry.currentProgress);
            buf.writeBoolean(entry.completed);
        },
        buf -> {
            String questUuid = ByteBufCodecs.STRING_UTF8.decode(buf);
            String title = ByteBufCodecs.STRING_UTF8.decode(buf);
            String npcName = ByteBufCodecs.STRING_UTF8.decode(buf);
            boolean visibleInGui = buf.readBoolean();
            QuestType type;
            try {
                type = QuestType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf));
            } catch (IllegalArgumentException exception) {
                type = QuestType.BOOLEAN;
            }
            String itemId = ByteBufCodecs.STRING_UTF8.decode(buf);
            int targetProgress = buf.readInt();
            int currentProgress = buf.readInt();
            boolean completed = buf.readBoolean();
            return new ClientQuestEntry(questUuid, title, npcName, visibleInGui, type, itemId, targetProgress, currentProgress, completed);
        }
    );
}
