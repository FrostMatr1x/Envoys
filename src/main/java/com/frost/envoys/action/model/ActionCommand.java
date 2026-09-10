package com.frost.envoys.action.model;

import java.util.ArrayList;
import java.util.List;

public class ActionCommand extends AbstractActionData {
    public final List<String> commands = new ArrayList<>();

    public ActionCommand(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "command";
    }
}
