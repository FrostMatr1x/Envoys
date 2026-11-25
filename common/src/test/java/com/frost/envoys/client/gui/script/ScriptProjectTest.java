package com.frost.envoys.client.gui.script;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScriptProjectTest {

    @Test
    void eventOrCreateCachesByType() {
        ScriptProject project = new ScriptProject();
        assertNull(project.event("click"));

        EventScript created = project.eventOrCreate("click");
        assertEquals("click", created.graph.eventType);
        assertSame(created, project.event("click"));
        assertSame(created, project.eventOrCreate("click"));
    }

    @Test
    void eventOrderIsStable() {
        assertEquals(List.of("update", "click", "kick", "range"), ScriptProject.EVENT_ORDER);
    }
}
