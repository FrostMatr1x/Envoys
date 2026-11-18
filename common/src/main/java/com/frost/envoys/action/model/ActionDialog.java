package com.frost.envoys.action.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.network.chat.Component;

public class ActionDialog extends AbstractActionData {

    public String NPCName = "";
    public Component npcMessage;

    public final Map<String, String> answers = new LinkedHashMap<>();

    public ActionDialog(String id, String npcname) {
        super(id);
        NPCName = npcname;
    }

    @Override
    public String getType() {
        return "dialog";
    }

    public ActionDialog addAnswer(String answerText, String nextActionId) {
        answers.put(answerText, nextActionId);
        return this;
    }
}
