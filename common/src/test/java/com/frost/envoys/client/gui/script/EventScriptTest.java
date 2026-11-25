package com.frost.envoys.client.gui.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventScriptTest {

    @Test
    void defaultStateAndLocks() {
        EventScript script = new EventScript(new ActionGraph("click"));
        assertEquals(EventUiState.GREEN_SYNCED, script.state);
        assertFalse(script.locked());

        script.state = EventUiState.GREY_LOCKED_CUSTOM;
        assertTrue(script.locked());

        script.state = EventUiState.GREEN_SYNCED;
        script.graph = null;
        assertTrue(script.locked());
    }

    @Test
    void graphOrEmptyCreatesAndCaches() {
        EventScript script = new EventScript();
        ActionGraph created = script.graphOrEmpty("range");
        assertNotNull(created);
        assertEquals("range", created.eventType);
        assertSame(created, script.graph);
        assertSame(created, script.graphOrEmpty("click"));
    }
}
