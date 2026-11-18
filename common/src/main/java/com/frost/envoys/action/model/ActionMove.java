package com.frost.envoys.action.model;

import com.google.gson.annotations.SerializedName;

public class ActionMove extends AbstractActionData {
    @SerializedName("target_x")
    public float targetX;
    @SerializedName("target_y")
    public float targetY;
    @SerializedName("target_z")
    public float targetZ;
    @SerializedName("wait_until_reached")
    public boolean waitUntilReached = false;

    public ActionMove(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "move";
    }
}
