package com.frost.envoys.action.model;

import com.google.gson.annotations.SerializedName;

public abstract class AbstractActionData implements EntityActionData {
    protected final String id;

    @SerializedName("next_action_id")
    public String nextActionId;

    protected AbstractActionData(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
