package com.frost.envoys.action.model;

public class ActionStart extends AbstractActionData {

    public ActionStart(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "start";
    }
}
