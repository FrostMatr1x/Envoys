package com.frost.envoys.client.gui.script;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;

public final class ScriptNodeTypes {

    public static final String START = "start";
    public static final String SAY = "say";
    public static final String WAIT = "wait";
    public static final String MOVE = "move";
    public static final String COMMAND = "command";
    public static final String DIALOGUE = "dialogue_choice";
    public static final String TRADE = "trade";
    public static final String QUEST_START = "quest_start";
    public static final String QUEST_CHECK = "quest_check";
    public static final String QUEST_ADVANCE = "quest_advance";
    public static final String QUEST_COMPLETE = "quest_complete";
    public static final String LOOK_AT = "lookAt";
    public static final String RANDOM = "random_choice";
    public static final String SAVE_POINT = "savepoint";
    public static final String LOAD_POINT = "loadpoint";

    public static final List<String> LINEAR_TYPES = List.of(
            START, SAY, WAIT, MOVE, COMMAND, TRADE, QUEST_START, QUEST_ADVANCE, QUEST_COMPLETE, LOOK_AT);

    public static final List<String> BRANCH_TYPES = List.of(DIALOGUE, RANDOM, QUEST_CHECK);

    private static final Set<String> PLAYER_REQUIRED = Set.of(
            DIALOGUE, TRADE, LOOK_AT, QUEST_START, QUEST_CHECK, QUEST_ADVANCE, QUEST_COMPLETE,
            SAVE_POINT, LOAD_POINT);

    private ScriptNodeTypes() {
    }

    public static boolean isBranch(String type) {
        return BRANCH_TYPES.contains(type);
    }

    public static boolean isKnown(String type) {
        return LINEAR_TYPES.contains(type) || BRANCH_TYPES.contains(type)
                || SAVE_POINT.equals(type) || LOAD_POINT.equals(type);
    }

    public static boolean isSavePoint(String type) {
        return SAVE_POINT.equals(type);
    }

    public static boolean isLoadPoint(String type) {
        return LOAD_POINT.equals(type);
    }

    public static boolean isJump(String type) {
        return isSavePoint(type) || isLoadPoint(type);
    }

    public static boolean requiresPlayer(String type) {
        return PLAYER_REQUIRED.contains(type);
    }

    public static Component displayName(String type) {
        if (type == null) {
            return Component.empty();
        }
        String key = switch (type) {
            case START, SAY, WAIT, MOVE, COMMAND, DIALOGUE, TRADE, QUEST_START, QUEST_CHECK,
                 QUEST_ADVANCE, QUEST_COMPLETE, LOOK_AT, RANDOM, SAVE_POINT, LOAD_POINT ->
                    "envoys.node." + type;
            default -> null;
        };
        return key == null ? Component.literal(type) : Component.translatable(key);
    }
}
