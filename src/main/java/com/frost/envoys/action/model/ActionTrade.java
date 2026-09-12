package com.frost.envoys.action.model;

import java.util.ArrayList;
import java.util.List;

import com.frost.envoys.npc.NPCTrade;
import com.google.gson.annotations.SerializedName;

public class ActionTrade extends AbstractActionData {
    public final List<NPCTrade> trades = new ArrayList<>();

    @SerializedName("base_slots")
    public Integer baseSlots = null;

    public ActionTrade(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "trade";
    }
}
