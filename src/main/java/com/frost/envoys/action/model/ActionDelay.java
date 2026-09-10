package com.frost.envoys.action.model;

import com.google.gson.annotations.SerializedName;

public class ActionDelay extends AbstractActionData {
    public int duration;
    @SerializedName("time_unit")
    public char timeUnit = 's';

    public ActionDelay(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "delay";
    }
}
