package com.frost.envoys.action.event;

import com.frost.envoys.action.model.ActionStart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcEventDataTest {

    @Test
    void eventTypeKeysAndFactories() {
        assertEquals("update", EventType.UPDATE.jsonKey());
        assertEquals("click", EventType.CLICK.jsonKey());
        assertEquals("kick", EventType.KICK.jsonKey());
        assertEquals("range", EventType.RANGE.jsonKey());

        assertEquals(EventType.UPDATE, EventType.fromKey("update"));
        assertEquals(EventType.CLICK, EventType.fromKey("click"));
        assertEquals(EventType.KICK, EventType.fromKey("kick"));
        assertEquals(EventType.RANGE, EventType.fromKey("range"));
        assertNull(EventType.fromKey("unknown"));
        assertNull(EventType.fromKey(null));

        assertInstanceOf(NpcUpdateEvent.class, EventType.UPDATE.createEvent());
        assertInstanceOf(NpcClickEvent.class, EventType.CLICK.createEvent());
        assertInstanceOf(NpcKickEvent.class, EventType.KICK.createEvent());
        assertInstanceOf(NpcRangeEvent.class, EventType.RANGE.createEvent());
    }

    @Test
    void defaultsAreEnabledAndActionsAreMutable() {
        NpcEventData event = EventType.CLICK.createEvent();
        assertTrue(event.enabled());
        assertTrue(event.actions().isEmpty());

        event.actions().add(new ActionStart("a"));
        assertEquals(1, event.actions().size());

        event.setEnabled(false);
        assertFalse(event.enabled());
    }

    @Test
    void eachSubclassReportsItsType() {
        assertEquals(EventType.UPDATE, new NpcUpdateEvent().type());
        assertEquals(EventType.CLICK, new NpcClickEvent().type());
        assertEquals(EventType.KICK, new NpcKickEvent().type());
        assertEquals(EventType.RANGE, new NpcRangeEvent().type());
    }
}
