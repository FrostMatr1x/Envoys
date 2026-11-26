package com.frost.envoys.gui.graph;

public class GraphViewNode {

    public final String id;
    public final GraphNodeKind kind;
    public int x;
    public int y;
    public int width = 160;
    public int height = 64;

    public GraphViewNode(String id, GraphNodeKind kind, int x, int y) {
        this.id = id;
        this.kind = kind;
        this.x = x;
        this.y = y;
    }
}
