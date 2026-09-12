package com.frost.envoys.action.model;

import com.google.gson.annotations.SerializedName;

public class ActionLoadPoint extends AbstractActionData {

    @SerializedName("save_id")
    public String saveId = "";

    public ActionLoadPoint(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "load_point";
    }
}
