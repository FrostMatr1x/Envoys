package com.frost.envoys.action.event;

public class NpcKickEvent extends AbstractNpcEventData {
    @Override
    public EventType type() {
        return EventType.KICK;
    }
}
