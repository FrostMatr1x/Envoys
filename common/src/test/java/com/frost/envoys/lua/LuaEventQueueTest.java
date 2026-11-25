package com.frost.envoys.lua;

import com.frost.envoys.action.event.EventType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuaEventQueueTest {

    @Test
    void fifoOrder() {
        LuaEventQueue queue = new LuaEventQueue();
        LuaEvent first = new LuaEvent(EventType.CLICK, null);
        LuaEvent second = new LuaEvent(EventType.KICK, null);

        queue.push(first);
        queue.push(second);

        assertEquals(List.of(first, second), queue.drain());
        assertTrue(queue.drain().isEmpty());
    }

    @Test
    void updateIsDeduplicated() {
        LuaEventQueue queue = new LuaEventQueue();
        LuaEvent first = new LuaEvent(EventType.UPDATE, null);
        LuaEvent second = new LuaEvent(EventType.UPDATE, null);

        queue.push(first);
        queue.push(second);

        List<LuaEvent> drained = queue.drain();
        assertEquals(1, drained.size());
        assertSame(first, drained.get(0));
    }

    @Test
    void capacityEvictsOldest() {
        LuaEventQueue queue = new LuaEventQueue();
        LuaEvent first = null;
        for (int i = 0; i < 17; i++) {
            LuaEvent event = new LuaEvent(EventType.CLICK, null);
            if (i == 0) {
                first = event;
            }
            queue.push(event);
        }

        List<LuaEvent> drained = queue.drain();
        assertEquals(16, drained.size());
        assertTrue(!drained.contains(first));
    }

    @Test
    void nullEventsAreIgnored() {
        LuaEventQueue queue = new LuaEventQueue();
        queue.push(null);
        queue.push(new LuaEvent(null, null));
        assertTrue(queue.drain().isEmpty());
    }

    @Test
    void clearRemovesEverything() {
        LuaEventQueue queue = new LuaEventQueue();
        queue.push(new LuaEvent(EventType.CLICK, null));
        queue.clear();
        assertTrue(queue.drain().isEmpty());
    }

    @Test
    void eventStoresTypeAndPlayer() {
        LuaEvent event = new LuaEvent(EventType.RANGE, null);
        assertSame(EventType.RANGE, event.type);
        assertNull(event.player);
    }
}
