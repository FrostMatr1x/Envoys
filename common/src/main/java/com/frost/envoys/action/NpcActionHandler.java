package com.frost.envoys.action;

import com.frost.envoys.action.model.EntityActionData;

public interface NpcActionHandler<T extends EntityActionData> {

    void execute(T action, ActionContext context);
}
