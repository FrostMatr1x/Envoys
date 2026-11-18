package com.frost.envoys.lua;

import com.frost.envoys.action.event.EventType;

import org.squiddev.cobalt.LuaValue;

public final class LuaEvent {

    public final EventType type;
    public final LuaValue player;

    public LuaEvent(EventType type, LuaValue player) {
        this.type = type;
        this.player = player;
    }
}
