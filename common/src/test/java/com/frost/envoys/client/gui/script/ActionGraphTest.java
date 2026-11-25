package com.frost.envoys.client.gui.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionGraphTest {

    @Test
    void addNodeAssignsIdAndStart() {
        ActionGraph graph = new ActionGraph("click");
        GraphNode node = new GraphNode("", "say");
        graph.addNode(node);
        assertEquals("1", node.id);
        assertEquals("1", graph.startId);
        assertEquals("1", graph.entryId());
    }

    @Test
    void addNodeReassignsDuplicateId() {
        ActionGraph graph = new ActionGraph("click");
        graph.addNode(new GraphNode("1", "say"));
        GraphNode duplicate = new GraphNode("1", "wait");
        graph.addNode(duplicate);
        assertEquals("2", duplicate.id);
        assertEquals(2, graph.nodes.size());
    }

    @Test
    void nextIdSkipsCollisions() {
        ActionGraph graph = new ActionGraph("click");
        assertEquals("1", graph.nextId());
        graph.addNode(new GraphNode("1", "say"));
        graph.addNode(new GraphNode("3", "say"));
        assertEquals("4", graph.nextId());
        graph.addNode(new GraphNode("4", "say"));
        assertEquals("5", graph.nextId());
    }

    @Test
    void removeNodeClearsNextAndStart() {
        ActionGraph graph = new ActionGraph("click");
        GraphNode first = new GraphNode("1", "say");
        GraphNode second = new GraphNode("2", "wait");
        first.nextId = "2";
        graph.addNode(first);
        graph.addNode(second);

        graph.removeNode("1");

        assertEquals(1, graph.nodes.size());
        assertEquals("2", graph.startId);
        assertNull(second.nextId);
    }

    @Test
    void removeNodeClearsBranchHead() {
        ActionGraph graph = new ActionGraph("click");
        GraphNode branch = new GraphNode("1", ScriptNodeTypes.DIALOGUE);
        branch.options.add(new GraphNode.BranchOption("a", "A", "2"));
        GraphNode target = new GraphNode("2", "say");
        graph.addNode(branch);
        graph.addNode(target);

        graph.removeNode("2");

        assertNull(branch.options.get(0).headId);
    }

    @Test
    void removeLastNodeClearsStart() {
        ActionGraph graph = new ActionGraph("click");
        graph.addNode(new GraphNode("1", "say"));
        graph.removeNode("1");
        assertTrue(graph.isEmpty());
        assertNull(graph.startId);
        assertNull(graph.entryId());
    }

    @Test
    void entryIdPrefersStartMarker() {
        ActionGraph graph = new ActionGraph("click");
        GraphNode say = new GraphNode("1", "say");
        GraphNode start = new GraphNode("s", ScriptNodeTypes.START);
        graph.addNode(say);
        graph.addNode(start);

        assertEquals("1", graph.startId);
        assertEquals("s", graph.entryId());
        assertSame(start, graph.startMarkerNode());
        assertNull(graph.node("missing"));
        assertNull(graph.node(null));
    }

    @Test
    void firstNodeAndEmpty() {
        ActionGraph graph = new ActionGraph("click");
        assertTrue(graph.isEmpty());
        assertNull(graph.findFirstNode());

        GraphNode node = new GraphNode("1", "say");
        graph.addNode(node);
        assertFalse(graph.isEmpty());
        assertSame(node, graph.findFirstNode());
    }
}
