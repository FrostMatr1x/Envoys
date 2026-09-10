package com.frost.envoys.action.model;

public class ActionQuestCheck extends AbstractActionData {
    // [TODO] — признак выполнения хранится в runtime-слое (флаг в ActionContext)
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
