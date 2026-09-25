package com.frost.envoys.client.gui.script;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ScriptProject {

    public static final java.util.List<String> EVENT_ORDER = java.util.List.of("update", "click", "kick", "range");

    public Map<String, EventScript> events = new LinkedHashMap<>();
    public boolean globalCustom = false;
    public String globalCustomSource = "";
    public boolean dirty = false;

    public EventScript event(String eventType) {
        return events.get(eventType);
    }

    public EventScript eventOrCreate(String eventType) {
        return events.computeIfAbsent(eventType, key -> new EventScript(new ActionGraph(key)));
    }
}
