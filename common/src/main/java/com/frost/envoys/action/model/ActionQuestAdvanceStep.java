package com.frost.envoys.action.model;

import com.google.gson.annotations.SerializedName;

public class ActionQuestAdvanceStep extends AbstractActionData {

    @SerializedName("quest_target")
    public String questTarget = "";

    @SerializedName("completion_id")
    public String completionId = "";

    public ActionQuestAdvanceStep(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "quest_advance_step";
    }
}
