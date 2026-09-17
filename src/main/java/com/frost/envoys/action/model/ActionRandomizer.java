package com.frost.envoys.action.model;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.SerializedName;

public class ActionRandomizer extends AbstractActionData {
    @SerializedName("options")
    public List<String> options = new ArrayList<>();

    public ActionRandomizer(String id) {
        super(id);
        this.options.add("");
    }

    @Override
    public String getType() {
        return "randomizer";
    }
}
