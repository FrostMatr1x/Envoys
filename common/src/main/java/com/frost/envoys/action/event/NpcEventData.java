package com.frost.envoys.action.event;

import com.frost.envoys.action.model.EntityActionData;
import java.util.List;

public interface NpcEventData {
    EventType type();
    boolean enabled();
    void setEnabled(boolean enabled);
    List<EntityActionData> actions();
}
