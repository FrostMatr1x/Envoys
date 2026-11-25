package com.frost.envoys.quest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDefinitionTest {

    @Test
    void booleanQuestRequiresIdsAndCompletions() {
        QuestDefinition quest = valid();
        quest.type = QuestType.BOOLEAN;
        assertTrue(quest.isValid());

        quest.requiredCompletions = 0;
        assertFalse(quest.isValid());
        quest.requiredCompletions = 1;

        quest.localId = " ";
        assertFalse(quest.isValid());
        quest.localId = "local";

        quest.questUuid = "";
        assertFalse(quest.isValid());
        quest.questUuid = "uuid";

        quest.type = null;
        assertFalse(quest.isValid());
    }

    @Test
    void itemQuestRequiresItemAndCount() {
        QuestDefinition quest = valid();
        quest.type = QuestType.ITEM;
        quest.itemId = "minecraft:apple";
        quest.itemCount = 1;
        assertTrue(quest.isValid());

        quest.itemId = "";
        assertFalse(quest.isValid());
        quest.itemId = "minecraft:apple";
        quest.itemCount = 0;
        assertFalse(quest.isValid());
    }

    @Test
    void killQuestRequiresEntityAndCount() {
        QuestDefinition quest = valid();
        quest.type = QuestType.KILL;
        quest.entityId = "minecraft:zombie";
        quest.killCount = 1;
        assertTrue(quest.isValid());

        quest.entityId = "";
        assertFalse(quest.isValid());
        quest.entityId = "minecraft:zombie";
        quest.killCount = 0;
        assertFalse(quest.isValid());
    }

    @Test
    void localIdCollision() {
        QuestDefinition self = valid();
        self.localId = "a";
        QuestDefinition other = valid();
        other.localId = "a";
        QuestDefinition different = valid();
        different.localId = "b";

        assertFalse(QuestDefinition.hasLocalIdCollision(null, self));
        assertFalse(QuestDefinition.hasLocalIdCollision(List.of(other), null));
        assertFalse(QuestDefinition.hasLocalIdCollision(List.of(self, different), self));
        assertTrue(QuestDefinition.hasLocalIdCollision(List.of(other), self));
        assertFalse(QuestDefinition.hasLocalIdCollision(List.of(other), different));

        self.localId = "";
        assertFalse(QuestDefinition.hasLocalIdCollision(List.of(other), self));
    }

    private static QuestDefinition valid() {
        QuestDefinition quest = new QuestDefinition();
        quest.localId = "local";
        quest.questUuid = "uuid";
        quest.type = QuestType.BOOLEAN;
        quest.requiredCompletions = 1;
        return quest;
    }
}
