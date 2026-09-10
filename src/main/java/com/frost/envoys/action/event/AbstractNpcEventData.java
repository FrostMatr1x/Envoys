package com.frost.envoys.action.event;

import com.frost.envoys.action.model.EntityActionData;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNpcEventData implements NpcEventData {
    public boolean enabled = true;
    public List<EntityActionData> actions = new ArrayList<>();

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public List<EntityActionData> actions() {
        return actions;
    }
}
