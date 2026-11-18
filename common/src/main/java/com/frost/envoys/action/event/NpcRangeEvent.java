package com.frost.envoys.action.event;

import com.google.gson.annotations.SerializedName;

public class NpcRangeEvent extends AbstractNpcEventData {
    @SerializedName("range_distance")
    public float rangeDistance = 5.0f;

    @Override
    public EventType type() {
        return EventType.RANGE;
    }
}
