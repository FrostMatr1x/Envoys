package com.frost.envoys.action.handler;

import com.frost.envoys.action.ActionContext;
import com.frost.envoys.action.NpcActionHandler;
import com.frost.envoys.action.model.ActionStart;

public final class StartActionHandler implements NpcActionHandler<ActionStart> {

    @Override
    public void execute(ActionStart action, ActionContext context) {
        context.advance(action.nextActionId);
    }
}
