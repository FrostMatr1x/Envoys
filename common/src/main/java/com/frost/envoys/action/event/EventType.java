package com.frost.envoys.action.event;

public enum EventType {
    UPDATE("update"),
    CLICK("click"),
    KICK("kick"),
    RANGE("range");

    private final String jsonKey;

    EventType(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    public String jsonKey() {
        return jsonKey;
    }

    public static EventType fromKey(String key) {
        for (EventType type : values()) {
            if (type.jsonKey.equals(key)) {
                return type;
            }
        }
        return null;
    }

    public NpcEventData createEvent() {
        return switch (this) {
            case UPDATE -> new NpcUpdateEvent();
            case CLICK -> new NpcClickEvent();
            case KICK -> new NpcKickEvent();
            case RANGE -> new NpcRangeEvent();
        };
    }
}
