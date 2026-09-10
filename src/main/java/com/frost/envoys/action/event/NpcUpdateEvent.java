package com.frost.envoys.action.event;

public class NpcUpdateEvent extends AbstractNpcEventData {
    @Override
    public EventType type() {
        return EventType.UPDATE;
    }
}
