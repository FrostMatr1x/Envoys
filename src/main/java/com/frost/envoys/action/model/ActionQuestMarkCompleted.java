package com.frost.envoys.action.model;

import com.google.gson.annotations.SerializedName;

public class ActionQuestMarkCompleted extends AbstractActionData {

    @SerializedName("quest_target")
    public String questTarget = "";

    public ActionQuestMarkCompleted(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "quest_mark_completed";
    }
}
