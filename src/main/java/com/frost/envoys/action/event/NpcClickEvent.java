package com.frost.envoys.action.event;

public class NpcClickEvent extends AbstractNpcEventData {
    @Override
    public EventType type() {
        return EventType.CLICK;
    }
}
