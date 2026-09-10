package com.frost.envoys.action.model;

import com.google.gson.annotations.SerializedName;

public class ActionChat extends AbstractActionData {
    public String message = "";
    @SerializedName("is_global")
    public boolean isGlobal = false;

    public ActionChat(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "chat";
    }
}
