package com.frost.envoys.client.gui.script;

import java.util.ArrayList;
import java.util.List;

public final class ActionGraph {

    public String eventType = "";
    public String arg = "";
    public boolean enabled = false;
    public boolean present = false;
    public String startId;
    public List<GraphNode> nodes = new ArrayList<>();

    public ActionGraph() {
    }

    public ActionGraph(String eventType) {
        this.eventType = eventType;
    }

    public GraphNode node(String id) {
        if (id == null) {
            return null;
        }
        for (GraphNode node : nodes) {
            if (id.equals(node.id)) {
                return node;
            }
        }
        return null;
    }

    public GraphNode startNode() {
        return node(startId);
    }

    public String entryId() {
        for (GraphNode node : nodes) {
            if (ScriptNodeTypes.START.equals(node.type)) {
                return node.id;
            }
        }
        return startId;
    }

    public GraphNode startMarkerNode() {
        for (GraphNode node : nodes) {
            if (ScriptNodeTypes.START.equals(node.type)) {
                return node;
            }
        }
        return null;
    }

    public GraphNode findFirstNode() {
        return nodes.isEmpty() ? null : nodes.get(0);
    }

    public void addNode(GraphNode node) {
        if (node == null) {
            return;
        }
        if (node.id == null || node.id.isBlank()) {
            node.id = nextId();
        }
        if (node(node.id) != null) {
            node.id = nextId();
        }
        nodes.add(node);
        if (startId == null || startId.isBlank()) {
            startId = node.id;
        }
    }

    public String nextId() {
        int index = nodes.size() + 1;
        while (node(Integer.toString(index)) != null) {
            index++;
        }
        return Integer.toString(index);
    }

    public void removeNode(String id) {
        if (id == null) {
            return;
        }
        nodes.removeIf(node -> id.equals(node.id));
        if (id.equals(startId)) {
            startId = nodes.isEmpty() ? null : nodes.get(0).id;
        }
        for (GraphNode node : nodes) {
            if (id.equals(node.nextId)) {
                node.nextId = null;
            }
            for (GraphNode.BranchOption option : node.options) {
                if (id.equals(option.headId)) {
                    option.headId = null;
                }
            }
        }
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }
}
