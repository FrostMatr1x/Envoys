package com.frost.envoys.action.handler;

import java.util.Optional;

import com.frost.envoys.Envoys;
import com.frost.envoys.action.ActionContext;
import com.frost.envoys.action.NpcActionHandler;
import com.frost.envoys.action.model.ActionQuestCheck;
import com.frost.envoys.quest.PlayerQuestTags;
import com.frost.envoys.quest.QuestDefinition;
import com.frost.envoys.quest.QuestInventoryUtil;
import com.frost.envoys.quest.QuestResolver;
import com.frost.envoys.quest.QuestType;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public final class QuestCheckActionHandler implements NpcActionHandler<ActionQuestCheck> {

    @Override
    public void execute(ActionQuestCheck action, ActionContext context) {
        Optional<QuestDefinition> resolved = QuestResolver.resolve(context.manager(), action.questTarget);
        if (resolved.isEmpty()) {
            Envoys.LOGGER.warn("[Envoys] Quest target '{}' not found for NPC {}", action.questTarget, context.npcUuid());
            context.advance(action.nextActionId);
            return;
        }

        QuestDefinition quest = resolved.get();
        Player player = context.player();

        boolean completed = false;
        if (player != null) {
            if (quest.type == QuestType.ITEM) {
                Item item = QuestInventoryUtil.resolveItem(quest.itemId);
                completed = item != null
                        && PlayerQuestTags.hasActive(player, quest.questUuid)
                        && QuestInventoryUtil.hasAtLeast(player, item, quest.itemCount);
            } else {
                completed = PlayerQuestTags.hasCompleted(player, quest.questUuid);
            }
        }

        if (completed) {
            PlayerQuestTags.removeCompleted(player, quest.questUuid);
            PlayerQuestTags.removeActive(player, quest.questUuid);
            if (quest.type == QuestType.ITEM && quest.consumeItems) {
                QuestInventoryUtil.consume(player, QuestInventoryUtil.resolveItem(quest.itemId), quest.itemCount);
            }
            context.advance(action.actionIfCompleted);
        } else {
            context.advance(action.actionIfNotCompleted);
        }
    }
}
