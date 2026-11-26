package com.frost.envoys.gui.graph;

import com.frost.envoys.client.gui.script.ScriptNodeTypes;

import net.minecraft.network.chat.Component;

public enum GraphNodeKind {
    START(ScriptNodeTypes.START, 0xFF4CAF50),
    DIALOGUE(ScriptNodeTypes.DIALOGUE, 0xFF4A90E2),
    QUEST_CHECK(ScriptNodeTypes.QUEST_CHECK, 0xFFE0A030);

    private final String typeId;
    private final int color;

    GraphNodeKind(String typeId, int color) {
        this.typeId = typeId;
        this.color = color;
    }

    public String typeId() {
        return this.typeId;
    }

    public int color() {
        return this.color;
    }

    public Component display() {
        return ScriptNodeTypes.displayName(this.typeId);
    }
}
