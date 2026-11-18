package com.frost.envoys.lua;

import com.frost.envoys.action.event.EventType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class LuaEventQueue {

    private static final int CAPACITY = 16;

    private final ArrayDeque<LuaEvent> events = new ArrayDeque<>();

    public void push(LuaEvent event) {
        if (event == null || event.type == null) {
            return;
        }

        if (event.type == EventType.UPDATE) {
            for (LuaEvent queued : events) {
                if (queued.type == EventType.UPDATE) {
                    return;
                }
            }
        }

        while (events.size() >= CAPACITY) {
            events.pollFirst();
        }
        events.addLast(event);
    }

    public List<LuaEvent> drain() {
        List<LuaEvent> drained = new ArrayList<>(events);
        events.clear();
        return drained;
    }

    public void clear() {
        events.clear();
    }
}
