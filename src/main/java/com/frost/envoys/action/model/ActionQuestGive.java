package com.frost.envoys.action.model;

import com.google.gson.annotations.SerializedName;

public class ActionQuestGive extends AbstractActionData {

    @SerializedName("quest_target")
    public String questTarget = "";

    public ActionQuestGive(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "quest_give";
    }
}
