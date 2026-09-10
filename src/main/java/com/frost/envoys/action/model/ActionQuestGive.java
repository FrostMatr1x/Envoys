package com.frost.envoys.action.model;

public class ActionQuestGive extends AbstractActionData {
    public String questText = "";
    public String questType = ""; // [TEMP/TODO] — тип квеста

    public ActionQuestGive(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "quest_give";
    }
}
