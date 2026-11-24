package com.frost.envoys.client.gui.script;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptNodeTypesTest {

    @Test
    void branchTypes() {
        assertTrue(ScriptNodeTypes.isBranch(ScriptNodeTypes.DIALOGUE));
        assertTrue(ScriptNodeTypes.isBranch(ScriptNodeTypes.RANDOM));
        assertTrue(ScriptNodeTypes.isBranch(ScriptNodeTypes.QUEST_CHECK));
        assertFalse(ScriptNodeTypes.isBranch(ScriptNodeTypes.SAY));
        assertFalse(ScriptNodeTypes.isBranch("unknown"));
    }

    @Test
    void knownTypes() {
        for (String type : List.of(
                ScriptNodeTypes.START, ScriptNodeTypes.SAY, ScriptNodeTypes.WAIT, ScriptNodeTypes.MOVE,
                ScriptNodeTypes.COMMAND, ScriptNodeTypes.DIALOGUE, ScriptNodeTypes.TRADE,
                ScriptNodeTypes.QUEST_START, ScriptNodeTypes.QUEST_CHECK, ScriptNodeTypes.QUEST_ADVANCE,
                ScriptNodeTypes.QUEST_COMPLETE, ScriptNodeTypes.LOOK_AT, ScriptNodeTypes.RANDOM,
                ScriptNodeTypes.SAVE_POINT, ScriptNodeTypes.LOAD_POINT)) {
            assertTrue(ScriptNodeTypes.isKnown(type), type);
        }
        assertFalse(ScriptNodeTypes.isKnown("unknown"));
    }

    @Test
    void jumpTypes() {
        assertTrue(ScriptNodeTypes.isJump(ScriptNodeTypes.SAVE_POINT));
        assertTrue(ScriptNodeTypes.isJump(ScriptNodeTypes.LOAD_POINT));
        assertFalse(ScriptNodeTypes.isJump(ScriptNodeTypes.SAY));
        assertFalse(ScriptNodeTypes.isJump(null));
    }

    @Test
    void playerRequirements() {
        for (String type : List.of(
                ScriptNodeTypes.DIALOGUE, ScriptNodeTypes.TRADE, ScriptNodeTypes.LOOK_AT,
                ScriptNodeTypes.QUEST_START, ScriptNodeTypes.QUEST_CHECK, ScriptNodeTypes.QUEST_ADVANCE,
                ScriptNodeTypes.QUEST_COMPLETE, ScriptNodeTypes.SAVE_POINT, ScriptNodeTypes.LOAD_POINT)) {
            assertTrue(ScriptNodeTypes.requiresPlayer(type), type);
        }
        for (String type : List.of(
                ScriptNodeTypes.START, ScriptNodeTypes.SAY, ScriptNodeTypes.WAIT, ScriptNodeTypes.MOVE,
                ScriptNodeTypes.COMMAND, ScriptNodeTypes.RANDOM)) {
            assertFalse(ScriptNodeTypes.requiresPlayer(type), type);
        }
    }
}
