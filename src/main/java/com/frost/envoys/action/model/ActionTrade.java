package com.frost.envoys.action.model;

import java.util.ArrayList;
import java.util.List;

import com.frost.envoys.npc.NPCTrade;

public class ActionTrade extends AbstractActionData {
    public final List<NPCTrade> trades = new ArrayList<>();

    public ActionTrade(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "trade";
    }
}
