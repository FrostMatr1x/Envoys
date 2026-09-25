package com.frost.envoys.client.gui.script;

import java.util.ArrayList;
import java.util.List;

public final class ActionCodeProvider {

    private ActionCodeProvider() {
    }

    public static String luaString(String value) {
        String text = value == null ? "" : value;
        StringBuilder sb = new StringBuilder(text.length() + 2);
        sb.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public static String floatLiteral(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return "0.0";
        }
        return Float.toString(value);
    }

    public static List<String> codeLines(GraphNode node) {
        List<String> lines = new ArrayList<>();
        switch (node.type) {
            case ScriptNodeTypes.SAY -> lines.add("envoys.say(" + luaString(node.param("text", "")) + ")");
            case ScriptNodeTypes.WAIT -> lines.add("envoys.wait(" + node.intParam("ticks", 0) + ")");
            case ScriptNodeTypes.MOVE -> lines.add("envoys.move("
                    + floatLiteral(node.floatParam("x", 0.0f)) + ", "
                    + floatLiteral(node.floatParam("y", 0.0f)) + ", "
                    + floatLiteral(node.floatParam("z", 0.0f)) + ")");
            case ScriptNodeTypes.COMMAND -> {
                for (String command : node.param("commands", "").split("\n", -1)) {
                    if (!command.isEmpty()) {
                        lines.add("envoys.command(" + luaString(command) + ")");
                    }
                }
            }
            case ScriptNodeTypes.TRADE -> lines.add(tradeCall(node));
            case ScriptNodeTypes.QUEST_START -> lines.add("envoys.quest.start("
                    + luaString(node.param("quest", "")) + ", player)");
            case ScriptNodeTypes.QUEST_ADVANCE -> lines.add("envoys.quest.advance("
                    + luaString(node.param("quest", "")) + ", " + node.intParam("amount", 1) + ", player)");
            case ScriptNodeTypes.QUEST_COMPLETE -> lines.add("envoys.quest.complete("
                    + luaString(node.param("quest", "")) + ", player)");
            case ScriptNodeTypes.LOOK_AT -> {
                if ("coords".equals(node.param("mode", "player"))) {
                    lines.add("envoys.lookAt({x=" + floatLiteral(node.floatParam("x", 0.0f))
                            + ", y=" + floatLiteral(node.floatParam("y", 0.0f))
                            + ", z=" + floatLiteral(node.floatParam("z", 0.0f)) + "})");
                } else {
                    lines.add("envoys.lookAt(player)");
                }
            }
            default -> {
            }
        }
        return lines;
    }

    public static String tradeCall(GraphNode node) {
        StringBuilder sb = new StringBuilder("envoys.trade({");
        for (int i = 0; i < node.offers.size(); i++) {
            GraphNode.TradeOffer offer = node.offers.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append(" { in1=").append(luaString(offer.in1))
                    .append(", in1count=").append(Math.max(1, offer.in1count));
            if (offer.in2 != null && !offer.in2.isBlank()) {
                sb.append(", in2=").append(luaString(offer.in2))
                        .append(", in2count=").append(Math.max(1, offer.in2count));
            }
            sb.append(", out=").append(luaString(offer.out))
                    .append(", outcount=").append(Math.max(1, offer.outcount))
                    .append(", priceMultiplier=").append(floatLiteral(offer.priceMultiplier))
                    .append(", demand=").append(offer.demand)
                    .append(", maxTrades=").append(offer.maxTrades)
                    .append(", resetTime=").append(offer.resetTime);
            if (offer.requiredLevel > 0) {
                sb.append(", requiredLevel=").append(offer.requiredLevel);
            }
            sb.append(" }");
        }
        sb.append(" }, player)");
        return sb.toString();
    }

    public static String branchDispatcherJump(GraphNode node) {
        return switch (node.type) {
            case ScriptNodeTypes.DIALOGUE -> {
                StringBuilder sb = new StringBuilder("local _ans_").append(node.id).append(" = envoys.dialogue(")
                        .append(luaString(node.param("text", ""))).append(", {");
                for (int i = 0; i < node.options.size(); i++) {
                    GraphNode.BranchOption option = node.options.get(i);
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append('[').append(luaString(option.label)).append("] = ").append(luaString(option.key));
                }
                sb.append("}, player)\n");
                sb.append("local _ret_").append(node.id).append('\n');
                for (int i = 0; i < node.options.size(); i++) {
                    GraphNode.BranchOption option = node.options.get(i);
                    sb.append(i == 0 ? "if " : "elseif ")
                            .append("_ans_").append(node.id).append(" == ").append(luaString(option.key))
                            .append(" then _ret_").append(node.id).append(" = choice_")
                            .append(i + 1).append('_').append(node.id).append("(player) ");
                }
                sb.append("end\n");
                sb.append("if _ret_").append(node.id).append(" ~= nil then return _ret_").append(node.id).append(" end");
                yield sb.toString();
            }
            case ScriptNodeTypes.RANDOM -> {
                StringBuilder sb = new StringBuilder("local r_").append(node.id)
                        .append(" = math.random(").append(Math.max(1, node.options.size())).append(")\n");
                sb.append("local _ret_").append(node.id).append('\n');
                for (int i = 0; i < node.options.size(); i++) {
                    sb.append(i == 0 ? "if " : "elseif ")
                            .append('r').append('_').append(node.id).append(" == ").append(i + 1)
                            .append(" then _ret_").append(node.id).append(" = choice_")
                            .append(i + 1).append('_').append(node.id).append("(player) ");
                }
                sb.append("end\n");
                sb.append("if _ret_").append(node.id).append(" ~= nil then return _ret_").append(node.id).append(" end");
                yield sb.toString();
            }
            case ScriptNodeTypes.QUEST_CHECK -> {
                StringBuilder sb = new StringBuilder("local _ret_").append(node.id).append('\n');
                sb.append("if envoys.quest.status(").append(luaString(node.param("quest", "")))
                        .append(", player) == \"COMPLETED\" then _ret_").append(node.id)
                        .append(" = choice_1_").append(node.id).append("(player) else _ret_").append(node.id)
                        .append(" = choice_2_").append(node.id).append("(player) end\n");
                sb.append("if _ret_").append(node.id).append(" ~= nil then return _ret_").append(node.id).append(" end");
                yield sb.toString();
            }
            default -> "";
        };
    }

    public static String branchDispatcher(GraphNode node) {
        return switch (node.type) {
            case ScriptNodeTypes.DIALOGUE -> {
                StringBuilder sb = new StringBuilder("local _ans_")
                        .append(node.id).append(" = envoys.dialogue(")
                        .append(luaString(node.param("text", ""))).append(", {");
                for (int i = 0; i < node.options.size(); i++) {
                    GraphNode.BranchOption option = node.options.get(i);
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append('[').append(luaString(option.label)).append("] = ").append(luaString(option.key));
                }
                sb.append("}, player)\n");
                for (int i = 0; i < node.options.size(); i++) {
                    GraphNode.BranchOption option = node.options.get(i);
                    sb.append(i == 0 ? "if " : "elseif ")
                            .append("_ans_").append(node.id).append(" == ").append(luaString(option.key))
                            .append(" then choice_").append(i + 1).append('_').append(node.id).append("(player) ");
                }
                sb.append("end");
                yield sb.toString();
            }
            case ScriptNodeTypes.RANDOM -> {
                StringBuilder sb = new StringBuilder("local r_").append(node.id)
                        .append(" = math.random(").append(Math.max(1, node.options.size())).append(")\n");
                for (int i = 0; i < node.options.size(); i++) {
                    sb.append(i == 0 ? "if " : "elseif ")
                            .append('r').append('_').append(node.id).append(" == ").append(i + 1)
                            .append(" then choice_").append(i + 1).append('_').append(node.id).append("(player) ");
                }
                sb.append("end");
                yield sb.toString();
            }
            case ScriptNodeTypes.QUEST_CHECK -> {
                String quest = luaString(node.param("quest", ""));
                // Mirror QuestCheckActionHandler: a completed quest is consumed by the
                // check (reset), so it disappears from the quest wall and can be re-given.
                yield "if envoys.quest.status(" + quest + ", player) == \"COMPLETED\""
                        + " then envoys.quest.reset(" + quest + ", player)"
                        + " choice_1_" + node.id + "(player)"
                        + " else choice_2_" + node.id + "(player) end";
            }
            default -> "";
        };
    }
}
