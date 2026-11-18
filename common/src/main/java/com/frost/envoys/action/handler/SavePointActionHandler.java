package com.frost.envoys.action.handler;

import com.frost.envoys.action.ActionContext;
import com.frost.envoys.action.NpcActionHandler;
import com.frost.envoys.action.PlayerCheckpointData;
import com.frost.envoys.action.model.ActionSavePoint;

public final class SavePointActionHandler implements NpcActionHandler<ActionSavePoint> {

    @Override
    public void execute(ActionSavePoint action, ActionContext context) {
        String npcUuid = context.npcUuid() == null ? null : context.npcUuid().toString();
        PlayerCheckpointData.setCheckpoint(context.player(), npcUuid, action.saveId, action.checkpointUuid);
        if (action.exitOnSave) {
            context.advance(null);
        } else {
            context.advance(action.nextActionId);
        }
    }
}
