package com.frost.envoys.action.handler;

import com.frost.envoys.action.ActionContext;
import com.frost.envoys.action.NpcActionHandler;
import com.frost.envoys.action.PlayerCheckpointData;
import com.frost.envoys.action.model.ActionLoadPoint;
import com.frost.envoys.action.model.ActionSavePoint;

public final class LoadPointActionHandler implements NpcActionHandler<ActionLoadPoint> {

    @Override
    public void execute(ActionLoadPoint action, ActionContext context) {
        String npcUuid = context.npcUuid() == null ? null : context.npcUuid().toString();
        String checkpointUuid = PlayerCheckpointData.getCheckpoint(context.player(), npcUuid, action.saveId);

        if (checkpointUuid != null) {
            ActionSavePoint savePoint = context.runner().findSavePoint(checkpointUuid);
            if (savePoint != null) {
                context.advance(savePoint.nextActionId);
                return;
            }
        }

        context.advance(action.nextActionId);
    }
}
