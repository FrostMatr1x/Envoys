package com.frost.envoys.client.gui.script;

public final class EventScript {

    public EventUiState state = EventUiState.GREEN_SYNCED;
    public ActionGraph graph;
    public String rawSource = "";

    public EventScript() {
    }

    public EventScript(ActionGraph graph) {
        this.graph = graph;
    }

    public boolean locked() {
        return state == EventUiState.GREY_LOCKED_CUSTOM || graph == null;
    }

    public ActionGraph graphOrEmpty(String eventType) {
        if (graph == null) {
            ActionGraph empty = new ActionGraph(eventType);
            graph = empty;
        }
        return graph;
    }
}
