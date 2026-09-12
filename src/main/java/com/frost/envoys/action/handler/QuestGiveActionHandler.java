package com.frost.envoys.action.handler;

import java.util.Optional;

import com.frost.envoys.Envoys;
import com.frost.envoys.action.ActionContext;
import com.frost.envoys.action.NpcActionHandler;
import com.frost.envoys.action.model.ActionQuestGive;
import com.frost.envoys.quest.PlayerQuestManager;
import com.frost.envoys.quest.QuestDefinition;
import com.frost.envoys.quest.QuestResolver;

import net.minecraft.world.entity.player.Player;

public final class QuestGiveActionHandler implements NpcActionHandler<ActionQuestGive> {

    @Override
    public void execute(ActionQuestGive action, ActionContext context) {
        Optional<QuestDefinition> resolved = QuestResolver.resolve(context.manager(), action.questTarget);
        if (resolved.isEmpty()) {
            Envoys.LOGGER.warn("[Envoys] Quest target '{}' not found for NPC {}", action.questTarget, context.npcUuid());
            context.advance(action.nextActionId);
            return;
        }

        Player player = context.player();
        if (player != null) {
            PlayerQuestManager.give(player, resolved.get().questUuid);
        }

        context.advance(action.nextActionId);
    }
}
