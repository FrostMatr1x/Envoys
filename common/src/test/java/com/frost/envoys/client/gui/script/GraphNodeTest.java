package com.frost.envoys.client.gui.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphNodeTest {

    @Test
    void paramFallsBackOnlyWhenMissing() {
        GraphNode node = new GraphNode("1", "say");
        assertEquals("fallback", node.param("text", "fallback"));
        node.params.put("text", "");
        assertEquals("", node.param("text", "fallback"));
        node.params.put("text", "value");
        assertEquals("value", node.param("text", "fallback"));
    }

    @Test
    void intParamTrimsAndFallsBack() {
        GraphNode node = new GraphNode("1", "wait");
        assertEquals(7, node.intParam("ticks", 7));
        node.params.put("ticks", " 42 ");
        assertEquals(42, node.intParam("ticks", 7));
        node.params.put("ticks", "garbage");
        assertEquals(7, node.intParam("ticks", 7));
        node.params.put("ticks", "");
        assertEquals(7, node.intParam("ticks", 7));
    }

    @Test
    void floatParamTrimsAndFallsBack() {
        GraphNode node = new GraphNode("1", "move");
        assertEquals(1.5f, node.floatParam("x", 1.5f));
        node.params.put("x", " 2.25 ");
        assertEquals(2.25f, node.floatParam("x", 1.5f));
        node.params.put("x", "nan?");
        assertEquals(1.5f, node.floatParam("x", 1.5f));
    }

    @Test
    void boolParamUsesJavaParsing() {
        GraphNode node = new GraphNode("1", "savepoint");
        assertTrue(node.boolParam("exit", true));
        node.params.put("exit", "true");
        assertTrue(node.boolParam("exit", false));
        node.params.put("exit", "TRUE");
        assertTrue(node.boolParam("exit", false));
        node.params.put("exit", "false");
        assertFalse(node.boolParam("exit", true));
        node.params.put("exit", "garbage");
        assertFalse(node.boolParam("exit", true));
    }

    @Test
    void isBranchMatchesNodeTypes() {
        assertTrue(new GraphNode("1", ScriptNodeTypes.DIALOGUE).isBranch());
        assertTrue(new GraphNode("1", ScriptNodeTypes.RANDOM).isBranch());
        assertTrue(new GraphNode("1", ScriptNodeTypes.QUEST_CHECK).isBranch());
        assertFalse(new GraphNode("1", ScriptNodeTypes.SAY).isBranch());
    }
}
