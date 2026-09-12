package com.frost.envoys.action.handler;

import java.util.Optional;

import com.frost.envoys.Envoys;
import com.frost.envoys.action.ActionContext;
import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.NpcActionHandler;
import com.frost.envoys.action.model.ActionQuestMarkCompleted;
import com.frost.envoys.quest.PlayerQuestManager;
import com.frost.envoys.quest.QuestDefinition;
import com.frost.envoys.quest.QuestIndex;
import com.frost.envoys.quest.QuestResolver;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class QuestMarkCompletedActionHandler implements NpcActionHandler<ActionQuestMarkCompleted> {

    @Override
    public void execute(ActionQuestMarkCompleted action, ActionContext context) {
        // Quest state is persisted on the server-side player; writing on a client-side Player
        // silently desyncs and is never persisted. Fail loudly if this ever runs client-side.
        Player online = context.player();
        if (!(online instanceof ServerPlayer player)) {
            Envoys.LOGGER.warn("[Envoys] quest_mark_completed skipped: no ServerPlayer (got {}) for NPC {} (target '{}')",
                    online == null ? "null" : online.getClass().getSimpleName(),
                    context.npcUuid(), action.questTarget);
            context.advance(action.nextActionId);
            return;
        }

        Optional<QuestDefinition> resolved = QuestResolver.resolve(context.manager(), action.questTarget);
        if (resolved.isEmpty()) {
            Envoys.LOGGER.warn("[Envoys] Quest target '{}' not found for NPC {} (npc local quests: {}; global quests: {})",
                    action.questTarget, context.npcUuid(),
                    describeLocalQuests(context.manager()), QuestIndex.entries().size());
            context.advance(action.nextActionId);
            return;
        }

        QuestDefinition quest = resolved.get();
        Envoys.LOGGER.info("[Envoys] Marking quest '{}' (local_id='{}', uuid='{}') completed for player {} via NPC {}",
                quest.title, quest.localId, quest.questUuid, player.getName().getString(), context.npcUuid());
        PlayerQuestManager.markCompleted(player, quest.questUuid);

        context.advance(action.nextActionId);
    }

    private static String describeLocalQuests(NPCInteractManager manager) {
        if (manager == null || manager.quests == null || manager.quests.isEmpty()) {
            return "<none>";
        }
        StringBuilder sb = new StringBuilder();
        for (QuestDefinition quest : manager.quests) {
            if (quest == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("local_id='").append(quest.localId).append("'/uuid='").append(quest.questUuid).append("'");
        }
        return sb.toString();
    }
}
