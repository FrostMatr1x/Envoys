package com.frost.envoys.quest;

import java.util.List;
import java.util.UUID;

import com.google.gson.annotations.SerializedName;

public class QuestDefinition {

    @SerializedName("quest_uuid")
    public String questUuid = "";

    @SerializedName("local_id")
    public String localId = "";

    public String title = "";

    @SerializedName("required_completions")
    public int requiredCompletions = 1;

    @SerializedName("visible_in_gui")
    public boolean visibleInGui = true;

    public QuestType type = QuestType.BOOLEAN;

    @SerializedName("item_id")
    public String itemId = "";

    @SerializedName("item_count")
    public int itemCount = 1;

    @SerializedName("consume_items")
    public boolean consumeItems = true;

    @SerializedName("entity_id")
    public String entityId = "";

    @SerializedName("kill_count")
    public int killCount = 1;

    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }

    public boolean isValid() {
        if (localId == null || localId.isBlank()) {
            return false;
        }
        if (questUuid == null || questUuid.isBlank()) {
            return false;
        }
        if (type == null) {
            return false;
        }
        if (type == QuestType.ITEM) {
            return itemId != null && !itemId.isBlank() && itemCount >= 1;
        }
        if (type == QuestType.KILL) {
            return entityId != null && !entityId.isBlank() && killCount >= 1;
        }
        return requiredCompletions >= 1;
    }

    public static boolean hasLocalIdCollision(List<QuestDefinition> quests, QuestDefinition self) {
        if (quests == null || self == null || self.localId == null || self.localId.isBlank()) {
            return false;
        }
        for (QuestDefinition quest : quests) {
            if (quest == null || quest == self) {
                continue;
            }
            if (self.localId.equals(quest.localId)) {
                return true;
            }
        }
        return false;
    }
}
