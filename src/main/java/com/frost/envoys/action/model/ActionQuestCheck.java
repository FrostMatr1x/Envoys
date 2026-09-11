package com.frost.envoys.action.model;

import com.google.gson.annotations.SerializedName;

public class ActionQuestCheck extends AbstractActionData {

    @SerializedName("quest_target")
    public String questTarget = "";

    public String actionIfCompleted;
    public String actionIfNotCompleted;

    public ActionQuestCheck(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "quest_check";
    }
}
