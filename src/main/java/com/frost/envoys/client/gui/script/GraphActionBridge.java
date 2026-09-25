package com.frost.envoys.client.gui.script;

import com.frost.envoys.action.model.ActionChat;
import com.frost.envoys.action.model.ActionCommand;
import com.frost.envoys.action.model.ActionDelay;
import com.frost.envoys.action.model.ActionMove;
import com.frost.envoys.action.model.ActionQuestAdvanceStep;
import com.frost.envoys.action.model.ActionQuestGive;
import com.frost.envoys.action.model.ActionLoadPoint;
import com.frost.envoys.action.model.ActionQuestMarkCompleted;
import com.frost.envoys.action.model.ActionSavePoint;
import com.frost.envoys.action.model.ActionStart;
import com.frost.envoys.action.model.ActionTrade;
import com.frost.envoys.action.model.EntityActionData;
import com.frost.envoys.npc.NPCTrade;
import com.frost.envoys.quest.QuestInventoryUtil;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class GraphActionBridge {

    private GraphActionBridge() {
    }

    public static boolean supports(String type) {
        return switch (type) {
            case ScriptNodeTypes.START, ScriptNodeTypes.SAY, ScriptNodeTypes.WAIT, ScriptNodeTypes.MOVE,
                 ScriptNodeTypes.COMMAND, ScriptNodeTypes.TRADE, ScriptNodeTypes.QUEST_START,
                 ScriptNodeTypes.QUEST_ADVANCE, ScriptNodeTypes.QUEST_COMPLETE,
                 ScriptNodeTypes.SAVE_POINT, ScriptNodeTypes.LOAD_POINT -> true;
            default -> false;
        };
    }

    public static EntityActionData toAction(GraphNode node) {
        return switch (node.type) {
            case ScriptNodeTypes.START -> {
                ActionStart action = new ActionStart(node.id);
                action.nextActionId = node.nextId;
                yield action;
            }
            case ScriptNodeTypes.SAY -> {
                ActionChat action = new ActionChat(node.id);
                action.message = node.param("text", "");
                action.isGlobal = node.boolParam("global", false);
                action.nextActionId = node.nextId;
                yield action;
            }
            case ScriptNodeTypes.WAIT -> {
                ActionDelay action = new ActionDelay(node.id);
                int ticks = node.intParam("ticks", 0);
                yield applyTicks(action, ticks);
            }
            case ScriptNodeTypes.MOVE -> {
                ActionMove action = new ActionMove(node.id);
                action.targetX = node.floatParam("x", 0.0f);
                action.targetY = node.floatParam("y", 0.0f);
                action.targetZ = node.floatParam("z", 0.0f);
                action.nextActionId = node.nextId;
                yield action;
            }
            case ScriptNodeTypes.COMMAND -> {
                ActionCommand action = new ActionCommand(node.id);
                String commands = node.param("commands", "");
                if (!commands.isEmpty()) {
                    for (String command : commands.split("\n", -1)) {
                        if (!command.isEmpty()) {
                            action.commands.add(command);
                        }
                    }
                }
                action.nextActionId = node.nextId;
                yield action;
            }
            case ScriptNodeTypes.TRADE -> {
                ActionTrade action = new ActionTrade(node.id);
                for (GraphNode.TradeOffer offer : node.offers) {
                    action.trades.add(toNpcTrade(offer));
                }
                action.nextActionId = node.nextId;
                yield action;
            }
            case ScriptNodeTypes.QUEST_START -> {
                ActionQuestGive action = new ActionQuestGive(node.id);
                action.questTarget = node.param("quest", "");
                action.nextActionId = node.nextId;
                yield action;
            }
            case ScriptNodeTypes.QUEST_ADVANCE -> {
                ActionQuestAdvanceStep action = new ActionQuestAdvanceStep(node.id);
                action.questTarget = node.param("quest", "");
                action.completionId = node.param("completionId", "lua_step");
                action.nextActionId = node.nextId;
                yield action;
            }
            case ScriptNodeTypes.QUEST_COMPLETE -> {
                ActionQuestMarkCompleted action = new ActionQuestMarkCompleted(node.id);
                action.questTarget = node.param("quest", "");
                action.nextActionId = node.nextId;
                yield action;
            }
            case ScriptNodeTypes.SAVE_POINT -> {
                ActionSavePoint action = new ActionSavePoint(node.id);
                action.saveId = node.param("name", "");
                action.checkpointUuid = node.param("cp", action.checkpointUuid);
                action.exitOnSave = node.boolParam("exit", false);
                action.nextActionId = node.nextId;
                yield action;
            }
            case ScriptNodeTypes.LOAD_POINT -> {
                ActionLoadPoint action = new ActionLoadPoint(node.id);
                action.saveId = node.param("target", "");
                action.nextActionId = node.nextId;
                yield action;
            }
            default -> null;
        };
    }

    public static void applyAction(GraphNode node, EntityActionData action) {
        String next = nextIdOf(action);
        switch (action) {
            case ActionChat chat -> {
                node.params.put("text", chat.message == null ? "" : chat.message);
                node.params.put("global", Boolean.toString(chat.isGlobal));
            }
            case ActionDelay delay -> node.params.put("ticks", Integer.toString(ticksOf(delay)));
            case ActionMove move -> {
                node.params.put("x", Float.toString(move.targetX));
                node.params.put("y", Float.toString(move.targetY));
                node.params.put("z", Float.toString(move.targetZ));
            }
            case ActionCommand command -> {
                node.params.put("commands", String.join("\n", command.commands));
            }
            case ActionTrade trade -> {
                List<GraphNode.TradeOffer> offers = new ArrayList<>();
                for (NPCTrade npcTrade : trade.trades) {
                    offers.add(fromNpcTrade(npcTrade));
                }
                for (int i = 0; i < offers.size() && i < node.offers.size(); i++) {
                    offers.get(i).requiredLevel = node.offers.get(i).requiredLevel;
                }
                node.offers.clear();
                node.offers.addAll(offers);
            }
            case ActionQuestGive give -> node.params.put("quest", give.questTarget == null ? "" : give.questTarget);
            case ActionQuestAdvanceStep advance -> {
                node.params.put("quest", advance.questTarget == null ? "" : advance.questTarget);
                node.params.put("completionId", advance.completionId == null ? "" : advance.completionId);
            }
            case ActionQuestMarkCompleted completed ->
                    node.params.put("quest", completed.questTarget == null ? "" : completed.questTarget);
            case ActionSavePoint savePoint -> {
                node.params.put("name", savePoint.saveId == null ? "" : savePoint.saveId);
                String cp = savePoint.checkpointUuid;
                if (cp == null || cp.isBlank()) {
                    cp = node.param("cp", "");
                }
                if (cp == null || cp.isBlank()) {
                    cp = java.util.UUID.randomUUID().toString();
                }
                node.params.put("cp", cp);
                node.params.put("exit", Boolean.toString(savePoint.exitOnSave));
            }
            case ActionLoadPoint loadPoint ->
                    node.params.put("target", loadPoint.saveId == null ? "" : loadPoint.saveId);
            default -> {
            }
        }
        node.nextId = (next == null || next.isBlank()) ? null : next;
    }

    private static String nextIdOf(EntityActionData action) {
        if (action instanceof com.frost.envoys.action.model.AbstractActionData abstractAction) {
            return abstractAction.nextActionId;
        }
        return null;
    }

    private static ActionDelay applyTicks(ActionDelay action, int ticks) {
        if (ticks > 0 && ticks % 20 == 0) {
            action.duration = ticks / 20;
            action.timeUnit = 's';
        } else {
            action.duration = Math.max(0, ticks);
            action.timeUnit = 't';
        }
        return action;
    }

    private static int ticksOf(ActionDelay action) {
        int factor = switch (action.timeUnit) {
            case 's' -> 20;
            case 'm' -> 1200;
            case 'h' -> 72000;
            default -> 1;
        };
        return Math.max(0, action.duration) * factor;
    }

    public static NPCTrade toNpcTrade(GraphNode.TradeOffer offer) {
        NPCTrade trade = new NPCTrade();
        trade.input1 = stackOf(offer.in1, offer.in1count);
        trade.input2 = stackOf(offer.in2, offer.in2count);
        trade.output = stackOf(offer.out, offer.outcount);
        trade.priceMultiplier = offer.priceMultiplier;
        trade.demand = offer.demand;
        trade.maxTrades = offer.maxTrades;
        trade.resetTime = offer.resetTime;
        return trade;
    }

    public static GraphNode.TradeOffer fromNpcTrade(NPCTrade trade) {
        GraphNode.TradeOffer offer = new GraphNode.TradeOffer();
        offer.in1 = idOf(trade.input1);
        offer.in1count = countOf(trade.input1);
        offer.in2 = idOf(trade.input2);
        offer.in2count = countOf(trade.input2);
        offer.out = idOf(trade.output);
        offer.outcount = countOf(trade.output);
        offer.priceMultiplier = trade.priceMultiplier;
        offer.demand = trade.demand;
        offer.maxTrades = trade.maxTrades;
        offer.resetTime = trade.resetTime;
        return offer;
    }

    private static ItemStack stackOf(String id, int count) {
        Item item = QuestInventoryUtil.resolveItem(id);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, Math.max(1, count));
    }

    private static String idOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "" : key.toString();
    }

    private static int countOf(ItemStack stack) {
        return stack == null || stack.isEmpty() ? 1 : stack.getCount();
    }
}
