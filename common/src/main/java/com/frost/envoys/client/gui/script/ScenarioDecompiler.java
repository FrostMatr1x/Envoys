package com.frost.envoys.client.gui.script;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScenarioDecompiler {

    private static final Set<String> SPECIAL = Set.of(
            "id", "type", "next", "offers", "options", "keys", "count");
    private static final Pattern CHOICE = Pattern.compile("function\\s+choice_(\\d+)_(.+?)\\(player\\)");
    private static final Pattern EVENT_CLOSE = Pattern.compile("end,\\s*\\d+\\)");

    private ScenarioDecompiler() {
    }

    public static ScriptProject decompile(String source, List<String> eventOrder) {
        ScriptProject project = new ScriptProject();
        String[] lines = (source == null ? "" : source).split("\n", -1);

        List<String> outside = new ArrayList<>();
        boolean inEvent = false;
        List<String> block = new ArrayList<>();
        String currentName = null;
        String currentArg = null;
        boolean currentEnabled = true;
        boolean unknownBlock = false;

        for (String rawLine : lines) {
            String trimmed = rawLine.trim();
            String kind = ScriptMarkers.isMarker(trimmed) ? ScriptMarkers.kind(trimmed) : null;

            if (ScriptMarkers.EVENT_START.equals(kind)) {
                inEvent = true;
                block = new ArrayList<>();
                block.add(rawLine);
                Map<String, String> attrs = ScriptMarkers.attrs(trimmed);
                currentName = ScriptMarkers.value(attrs.get("name"));
                currentArg = ScriptMarkers.value(attrs.get("arg"));
                currentEnabled = !"false".equalsIgnoreCase(ScriptMarkers.value(attrs.get("enabled")));
            } else if (ScriptMarkers.EVENT_END.equals(kind) && inEvent) {
                block.add(rawLine);
                if (currentName != null && eventOrder.contains(currentName)) {
                    project.events.put(currentName, parseEvent(currentName, currentArg, currentEnabled, block));
                } else {
                    unknownBlock = true;
                    outside.addAll(block);
                }
                inEvent = false;
                block = new ArrayList<>();
                currentName = null;
                currentArg = null;
            } else if (inEvent) {
                block.add(rawLine);
            } else {
                outside.add(rawLine);
            }
        }
        if (inEvent) {
            unknownBlock = true;
            outside.addAll(block);
        }

        boolean substantive = unknownBlock;
        for (String line : outside) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("--")) {
                substantive = true;
                break;
            }
        }
        project.globalCustom = substantive;
        project.globalCustomSource = String.join("\n", outside).stripTrailing();

        for (String key : eventOrder) {
            project.events.computeIfAbsent(key, k -> {
                EventScript script = new EventScript(new ActionGraph(k));
                script.state = EventUiState.GREEN_SYNCED;
                return script;
            });
        }
        return project;
    }

    private static EventScript parseEvent(String name, String arg, boolean enabled, List<String> blockLines) {
        EventScript script = new EventScript();
        List<String> body = new ArrayList<>(blockLines);
        if (!body.isEmpty()) {
            body.remove(0);
        }
        if (!body.isEmpty()) {
            body.remove(body.size() - 1);
        }

        ActionGraph graph = new ActionGraph(name);
        graph.arg = arg == null ? "" : arg;
        graph.present = true;
        graph.enabled = enabled;

        Deque<BranchContext> branchStack = new ArrayDeque<>();
        GraphNode openAction = null;
        int openActionLines = 0;
        boolean locked = false;
        int blockDepth = 0;

        for (String rawLine : body) {
            String t = rawLine.trim();
            if (t.isEmpty() || t.equals("--[[") || t.equals("--]]")) {
                continue;
            }

            if (ScriptMarkers.isMarker(t)) {
                String kind = ScriptMarkers.kind(t);
                Map<String, String> attrs = ScriptMarkers.attrs(t);
                switch (kind == null ? "" : kind) {
                    case ScriptMarkers.ACTION_START -> {
                        GraphNode node = new GraphNode();
                        node.id = ScriptMarkers.value(attrs.get("id"));
                        node.type = ScriptMarkers.value(attrs.get("type"));
                        node.nextId = nullIf(ScriptMarkers.value(attrs.get("next")));
                        applyParams(node, attrs);
                        graph.nodes.add(node);
                        if (branchStack.isEmpty() && graph.startId == null) {
                            graph.startId = node.id;
                        }
                        assignHead(branchStack, node);
                        openAction = node;
                        openActionLines = 0;
                    }
                    case ScriptMarkers.ACTION_END -> {
                        if (openAction != null
                                && ScriptNodeTypes.isJump(openAction.type)
                                && openActionLines == 0) {
                            locked = true;
                        }
                        openAction = null;
                    }
                    case ScriptMarkers.COMMAND_LINE -> {
                        if (openAction != null) {
                            String text = ScriptMarkers.value(attrs.get("text"));
                            String current = openAction.params.get("commands");
                            openAction.params.put("commands", current == null ? text : current + "\n" + text);
                        }
                    }
                    case ScriptMarkers.TRADE_ITEM -> {
                        if (openAction != null) {
                            openAction.offers.add(readTradeOffer(attrs));
                        }
                    }
                    case ScriptMarkers.BRANCH_START -> {
                        GraphNode node = new GraphNode();
                        node.id = ScriptMarkers.value(attrs.get("id"));
                        node.type = ScriptMarkers.value(attrs.get("type"));
                        node.nextId = nullIf(ScriptMarkers.value(attrs.get("next")));
                        applyParams(node, attrs);
                        if (ScriptNodeTypes.DIALOGUE.equals(node.type)) {
                            List<String> labels = ScriptMarkers.parseList(attrs.get("options"));
                            List<String> keys = ScriptMarkers.parseList(attrs.get("keys"));
                            for (int i = 0; i < labels.size(); i++) {
                                String key = i < keys.size() ? keys.get(i) : Integer.toString(i + 1);
                                node.options.add(new GraphNode.BranchOption(key, labels.get(i), null));
                            }
                        } else if (ScriptNodeTypes.RANDOM.equals(node.type)) {
                            int count = parseInt(attrs.get("count"), 0);
                            for (int i = 0; i < count; i++) {
                                node.options.add(new GraphNode.BranchOption(
                                        Integer.toString(i + 1), "Option " + (i + 1), null));
                            }
                        } else if (ScriptNodeTypes.QUEST_CHECK.equals(node.type)) {
                            node.options.add(new GraphNode.BranchOption("completed", "Completed", null));
                            node.options.add(new GraphNode.BranchOption("not_completed", "Not completed", null));
                        } else {
                            locked = true;
                        }
                        graph.nodes.add(node);
                        if (branchStack.isEmpty() && graph.startId == null) {
                            graph.startId = node.id;
                        }
                        assignHead(branchStack, node);
                        openAction = null;
                        branchStack.push(new BranchContext(node));
                    }
                    case ScriptMarkers.BRANCH_END -> {
                        if (branchStack.isEmpty()) {
                            locked = true;
                        } else {
                            BranchContext ctx = branchStack.pop();
                            if (ctx.inOption) {
                                locked = true;
                            }
                        }
                    }
                    default -> locked = true;
                }
                continue;
            }

            if (!enabled && t.startsWith("-- ")) {
                t = t.substring(3).trim();
                if (t.isEmpty()) {
                    continue;
                }
            }

            if (openAction != null) {
                if (!matchesGeneratedCode(openAction.type, t)) {
                    locked = true;
                } else {
                    openActionLines++;
                }
                continue;
            }

            if (t.equals("end") && !branchStack.isEmpty() && branchStack.peek().inOption) {
                BranchContext ctx = branchStack.peek();
                ctx.inOption = false;
                ctx.pendingOption = null;
                continue;
            }

            if (t.equals("end") && blockDepth > 0) {
                blockDepth--;
                continue;
            }

            if (t.startsWith("function seg_") || t.equals("while _jmp_ do")) {
                blockDepth++;
                continue;
            }

            if (t.startsWith("function choice_") && !branchStack.isEmpty()) {
                BranchContext ctx = branchStack.peek();
                Matcher matcher = CHOICE.matcher(t);
                if (!matcher.matches() || !matcher.group(2).equals(ctx.node.id)) {
                    locked = true;
                    continue;
                }
                int index;
                try {
                    index = Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    locked = true;
                    continue;
                }
                if (index < 1 || index > ctx.node.options.size()) {
                    locked = true;
                    continue;
                }
                ctx.pendingOption = ctx.node.options.get(index - 1);
                ctx.pendingOption.headId = null;
                ctx.inOption = true;
                continue;
            }

            if (isFrame(t) || isDispatcher(t)) {
                continue;
            }

            locked = true;
        }

        if (openAction != null || !branchStack.isEmpty() || blockDepth != 0) {
            locked = true;
        }

        if (locked) {
            script.state = EventUiState.GREY_LOCKED_CUSTOM;
            script.graph = null;
            script.rawSource = String.join("\n", blockLines).stripTrailing();
            return script;
        }

        if (graph.nodes.isEmpty()) {
            script.graph = graph;
            script.state = EventUiState.GREEN_SYNCED;
            return script;
        }
        if (graph.startId == null) {
            graph.startId = graph.nodes.get(0).id;
        }
        if (graph.startId != null) {
            GraphNode start = graph.node(graph.startId);
            if (start != null) {
                graph.nodes.remove(start);
                graph.nodes.add(0, start);
            }
        }

        script.graph = graph;
        script.state = EventUiState.GREEN_SYNCED;
        return script;
    }

    private static void assignHead(Deque<BranchContext> branchStack, GraphNode node) {
        if (!branchStack.isEmpty()) {
            BranchContext ctx = branchStack.peek();
            if (ctx.inOption && ctx.pendingOption != null && ctx.pendingOption.headId == null) {
                ctx.pendingOption.headId = node.id;
            }
        }
    }

    private static void applyParams(GraphNode node, Map<String, String> attrs) {
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            if (SPECIAL.contains(entry.getKey())) {
                continue;
            }
            node.params.put(entry.getKey(), ScriptMarkers.value(entry.getValue()));
        }
    }

    private static GraphNode.TradeOffer readTradeOffer(Map<String, String> attrs) {
        GraphNode.TradeOffer offer = new GraphNode.TradeOffer();
        offer.in1 = ScriptMarkers.value(attrs.get("in1"));
        offer.in1count = parseInt(attrs.get("in1count"), 1);
        offer.in2 = ScriptMarkers.value(attrs.get("in2"));
        offer.in2count = parseInt(attrs.get("in2count"), 1);
        offer.out = ScriptMarkers.value(attrs.get("out"));
        offer.outcount = parseInt(attrs.get("outcount"), 1);
        offer.priceMultiplier = parseFloat(attrs.get("priceMultiplier"), 1.0f);
        offer.demand = parseInt(attrs.get("demand"), 0);
        offer.maxTrades = parseInt(attrs.get("maxTrades"), -1);
        offer.resetTime = parseInt(attrs.get("resetTime"), -1);
        offer.requiredLevel = parseInt(attrs.get("requiredLevel"), 0);
        return offer;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(ScriptMarkers.value(raw).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float parseFloat(String raw, float fallback) {
        try {
            return Float.parseFloat(ScriptMarkers.value(raw).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean isFrame(String t) {
        if (t.startsWith("envoys.on(") && t.contains("function(player")) {
            return true;
        }
        if (t.equals("local player = nil")) {
            return true;
        }
        if (t.startsWith("local _jmp_") || t.startsWith("return seg_")) {
            return true;
        }
        return t.equals("end)") || EVENT_CLOSE.matcher(t).matches();
    }

    private static boolean matchesGeneratedCode(String type, String t) {
        return switch (type == null ? "" : type) {
            case ScriptNodeTypes.SAY -> t.startsWith("envoys.say(");
            case ScriptNodeTypes.WAIT -> t.startsWith("envoys.wait(");
            case ScriptNodeTypes.MOVE -> t.startsWith("envoys.move(");
            case ScriptNodeTypes.COMMAND -> t.startsWith("envoys.command(");
            case ScriptNodeTypes.TRADE -> t.startsWith("envoys.trade(");
            case ScriptNodeTypes.QUEST_START -> t.startsWith("envoys.quest.start(");
            case ScriptNodeTypes.QUEST_ADVANCE -> t.startsWith("envoys.quest.advance(");
            case ScriptNodeTypes.QUEST_COMPLETE -> t.startsWith("envoys.quest.complete(");
            case ScriptNodeTypes.LOOK_AT -> t.startsWith("envoys.lookAt(");
            case ScriptNodeTypes.SAVE_POINT -> t.startsWith("envoys.checkpoint.set(")
                    || t.startsWith("return seg_") || t.equals("return");
            case ScriptNodeTypes.LOAD_POINT -> t.startsWith("local _cp_") || t.startsWith("if _cp_")
                    || t.startsWith("elseif _cp_") || t.equals("end");
            default -> false;
        };
    }

    private static boolean isDispatcher(String t) {
        return t.startsWith("local _ans_") || t.startsWith("local r_") || t.startsWith("local _ret_")
                || t.startsWith("if ") || t.startsWith("elseif ");
    }

    private static String nullIf(String value) {
        return value == null || value.isBlank() || "null".equals(value) ? null : value;
    }

    private static final class BranchContext {
        private final GraphNode node;
        private GraphNode.BranchOption pendingOption;
        private boolean inOption;

        private BranchContext(GraphNode node) {
            this.node = node;
        }
    }
}
