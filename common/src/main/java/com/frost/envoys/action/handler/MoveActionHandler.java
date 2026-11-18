package com.frost.envoys.action.handler;

import com.frost.envoys.action.ActionContext;
import com.frost.envoys.action.NpcActionHandler;
import com.frost.envoys.action.model.ActionMove;
import com.frost.envoys.npc.entity.BaseNPC;

public final class MoveActionHandler implements NpcActionHandler<ActionMove> {

    @Override
    public void execute(ActionMove action, ActionContext context) {
        BaseNPC npc = context.npc();

        npc.beginScriptedMovement();
        npc.getNavigation().stop();

        boolean started = npc.getNavigation().moveTo(
                action.targetX,
                action.targetY,
                action.targetZ,
                1.0D
        );

        // Если перемещение успешно запустилось — ждём окончания пути
        if (started) {
            context.waitForMove();
        } else {
            // Если путь не найден (заблокирован/в воздухе), сразу переходим к следующему действию
            npc.endScriptedMovement();
            context.advance(action.nextActionId);
        }
    }
}
