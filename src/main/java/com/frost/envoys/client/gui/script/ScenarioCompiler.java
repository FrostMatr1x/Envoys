package com.frost.envoys.client.gui.script;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ScenarioCompiler {

    private ScenarioCompiler() {
    }

    public static String compile(ScriptProject project, List<String> eventOrder) throws ScenarioCompileException {
        return compile(project, eventOrder, true);
    }

    public static String compile(ScriptProject project, List<String> eventOrder, boolean validate)
            throws ScenarioCompileException {
        StringBuilder out = new StringBuilder();
        if (project.globalCustom && project.globalCustomSource != null && !project.globalCustomSource.isBlank()) {
            out.append(project.globalCustomSource.stripTrailing()).append('\n');
        }

        for (String key : eventOrder) {
            EventScript script = project.event(key);
            if (script == null) {
                continue;
            }
            if (script.locked()) {
                if (script.rawSource != null && !script.rawSource.isBlank()) {
                    out.append(script.rawSource.stripTrailing()).append('\n');
                }
                continue;
            }
            ActionGraph graph = script.graph;
            if (graph == null || !graph.present || graph.isEmpty()) {
                continue;
            }
            try {
                appendEvent(out, graph, validate);
            } catch (ScenarioCompileException e) {
                if (validate) {
                    throw e;
                }
            }
        }
        return out.toString();
    }

    private static void appendEvent(StringBuilder out, ActionGraph graph, boolean validate)
            throws ScenarioCompileException {
        String name = graph.eventType;
        boolean update = "update".equals(name);
        boolean range = "range".equals(name);

        boolean enabled = graph.enabled;
        String entry = graph.entryId();
        if (validate) {
            // For disabled events the update-player restriction is irrelevant until re-enabled.
            validate(graph, update && enabled);
        } else if (entry == null || graph.node(entry) == null) {
            return;
        }

        String arg = graph.arg == null ? "" : graph.arg.trim();
        if (validate && enabled && (update || range) && arg.isBlank()) {
            throw new ScenarioCompileException("Событие '" + name + "' требует параметр (интервал/радиус).");
        }

        boolean hasArg = (update || range) && !arg.isBlank();
        boolean comment = !enabled;
        out.append(ScriptMarkers.eventStart(name, hasArg ? arg : null, enabled)).append('\n');
        emitCode(out, "envoys.on(" + ActionCodeProvider.luaString(name) + ", function(player)", 0, comment);
        if (update) {
            emitCode(out, "local player = nil", 1, comment);
        }
        if (hasJumpNodes(graph)) {
            emitJumpBody(out, graph, entry, name, update, 1, comment);
        } else {
            emitChain(out, graph, entry, update, 1, new HashSet<>(), comment);
        }
        emitCode(out, hasArg ? "end, " + parseArg(arg) + ")" : "end)", 0, comment);
        out.append(ScriptMarkers.eventEnd(name)).append('\n');
    }

    private static boolean hasJumpNodes(ActionGraph graph) {
        for (GraphNode node : graph.nodes) {
            if (ScriptNodeTypes.isJump(node.type)) {
                return true;
            }
        }
        return false;
    }

    private static void emitJumpBody(StringBuilder out, ActionGraph graph, String entry, String eventName,
                                     boolean update, int indent, boolean comment) throws ScenarioCompileException {
        Set<String> visited = new HashSet<>();
        String startFunc = "seg_" + eventName + "_start";
        emitSegment(out, graph, startFunc, entry, eventName, update, indent, visited, comment);

        List<String> labels = new ArrayList<>();
        labels.add("start");
        List<String> functions = new ArrayList<>();
        functions.add(startFunc);
        for (GraphNode node : graph.nodes) {
            if (!ScriptNodeTypes.isSavePoint(node.type)) {
                continue;
            }
            String label = segmentLabel(eventName, node);
            emitSegment(out, graph, label, node.nextId, eventName, update, indent, visited, comment);
            labels.add(label);
            functions.add(label);
        }

        emitCode(out, "local _jmp_ = \"start\"", indent, comment);
        emitCode(out, "while _jmp_ do", indent, comment);
        StringBuilder dispatcher = new StringBuilder();
        for (int i = 0; i < labels.size(); i++) {
            dispatcher.append(i == 0 ? "if " : "elseif ")
                    .append("_jmp_ == ").append(ActionCodeProvider.luaString(labels.get(i)))
                    .append(" then _jmp_ = ").append(functions.get(i)).append("(player) ");
        }
        dispatcher.append("end");
        emitCode(out, dispatcher.toString(), indent, comment);
        emitCode(out, "end", indent, comment);
    }

    static String segmentLabel(String eventName, GraphNode savePoint) {
        return "seg_" + eventName + "_" + savePoint.param("name", "") + "_" + savePoint.id;
    }

    private static void emitSegment(StringBuilder out, ActionGraph graph, String functionName, String headId,
                                    String eventName, boolean update, int indent, Set<String> visited, boolean comment)
            throws ScenarioCompileException {
        emitCode(out, "function " + functionName + "(player)", indent, comment);
        emitFlow(out, graph, headId, eventName, update, indent + 1, visited, comment);
        emitCode(out, "end", indent, comment);
    }

    private static void emitFlow(StringBuilder out, ActionGraph graph, String headId, String eventName,
                                 boolean update, int indent, Set<String> visited, boolean comment)
            throws ScenarioCompileException {
        String nodeId = headId;
        while (nodeId != null) {
            GraphNode node = graph.node(nodeId);
            if (node == null || !visited.add(nodeId)) {
                throw new ScenarioCompileException("Повторный или отсутствующий узел: '" + nodeId + "'.");
            }

            if (ScriptNodeTypes.isSavePoint(node.type)) {
                boolean exit = node.boolParam("exit", false);
                out.append(indent(indent)).append(ScriptMarkers.actionStart(node)).append('\n');
                emitCode(out, "envoys.checkpoint.set(" + ActionCodeProvider.luaString(node.param("name", ""))
                        + ", " + ActionCodeProvider.luaString(node.param("cp", "")) + ")", indent, comment);
                if (exit) {
                    emitCode(out, "return", indent, comment);
                } else {
                    emitCode(out, "return " + segmentLabel(eventName, node) + "(player)", indent, comment);
                }
                out.append(indent(indent)).append(ScriptMarkers.actionEnd(node.id)).append('\n');
                return;
            }
            if (ScriptNodeTypes.isLoadPoint(node.type)) {
                out.append(indent(indent)).append(ScriptMarkers.actionStart(node)).append('\n');
                emitCode(out, "local _cp_" + node.id + " = envoys.checkpoint.get("
                        + ActionCodeProvider.luaString(node.param("target", "")) + ")", indent, comment);
                StringBuilder dispatch = new StringBuilder();
                int index = 0;
                for (GraphNode savePoint : graph.nodes) {
                    if (!ScriptNodeTypes.isSavePoint(savePoint.type)
                            || !savePoint.param("name", "").equals(node.param("target", ""))) {
                        continue;
                    }
                    dispatch.append(index == 0 ? "if " : "elseif ")
                            .append("_cp_").append(node.id).append(" == ")
                            .append(ActionCodeProvider.luaString(savePoint.param("cp", "")))
                            .append(" then return ").append(ActionCodeProvider.luaString(segmentLabel(eventName, savePoint)))
                            .append(" ");
                    index++;
                }
                if (index > 0) {
                    dispatch.append("end");
                    emitCode(out, dispatch.toString(), indent, comment);
                }
                out.append(indent(indent)).append(ScriptMarkers.actionEnd(node.id)).append('\n');
                nodeId = node.nextId;
                continue;
            }
            if (node.isBranch()) {
                out.append(indent(indent)).append(ScriptMarkers.branchStart(node)).append('\n');
                for (int i = 0; i < node.options.size(); i++) {
                    GraphNode.BranchOption option = node.options.get(i);
                    emitCode(out, "function choice_" + (i + 1) + "_" + node.id + "(player)", indent, comment);
                    if (option.headId != null) {
                        emitFlow(out, graph, option.headId, eventName, update, indent + 1, visited, comment);
                    }
                    emitCode(out, "end", indent, comment);
                }
                for (String line : ActionCodeProvider.branchDispatcherJump(node).split("\n", -1)) {
                    if (!line.isEmpty()) {
                        emitCode(out, line, indent, comment);
                    }
                }
                out.append(indent(indent)).append(ScriptMarkers.branchEnd(node.id)).append('\n');
                nodeId = node.nextId;
                continue;
            }

            out.append(indent(indent)).append(ScriptMarkers.actionStart(node)).append('\n');
            emitSubMarkers(out, node, indent);
            for (String line : ActionCodeProvider.codeLines(node)) {
                emitCode(out, line, indent, comment);
            }
            out.append(indent(indent)).append(ScriptMarkers.actionEnd(node.id)).append('\n');
            nodeId = node.nextId;
        }
    }

    private static String parseArg(String arg) throws ScenarioCompileException {
        try {
            return Integer.toString(Math.max(0, Integer.parseInt(arg.trim())));
        } catch (NumberFormatException e) {
            throw new ScenarioCompileException("Параметр события должен быть целым числом: '" + arg + "'");
        }
    }

    private static void validate(ActionGraph graph, boolean update) throws ScenarioCompileException {
        String entry = graph.entryId();
        if (entry == null || graph.node(entry) == null) {
            throw new ScenarioCompileException("У события '" + graph.eventType + "' не задано начало цепочки.");
        }

        for (GraphNode node : graph.nodes) {
            if (!ScriptNodeTypes.isKnown(node.type)) {
                throw new ScenarioCompileException("Неизвестный тип узла: '" + node.type + "'.");
            }
        }

        Set<String> visited = new HashSet<>();
        validateChain(graph, entry, update, visited, new ArrayList<>(), new ArrayList<>());

        Map<String, GraphNode> saveByKey = validateJumps(graph);
        checkJumpCycles(graph, saveByKey);
    }

    private static Map<String, GraphNode> validateJumps(ActionGraph graph) throws ScenarioCompileException {
        Set<String> keyCpPairs = new HashSet<>();
        for (GraphNode node : graph.nodes) {
            if (!ScriptNodeTypes.isSavePoint(node.type)) {
                continue;
            }
            String key = node.param("name", "").trim();
            if (key.isEmpty() || !key.matches("[A-Za-z0-9_]{1,64}")) {
                throw new ScenarioCompileException("Недопустимый ключ точки сохранения '" + key
                        + "' (узел " + node.id + "): разрешены [a-zA-Z0-9_]{1,64}.");
            }
            if ("start".equals(key)) {
                throw new ScenarioCompileException("Ключ 'start' зарезервирован (узел " + node.id + ").");
            }
            String cp = node.param("cp", "").trim();
            if (cp.isEmpty()) {
                throw new ScenarioCompileException("У точки сохранения (узел " + node.id + ") не задан внутренний UUID.");
            }
            if (!keyCpPairs.add(key + "\u0000" + cp)) {
                throw new ScenarioCompileException("Дублирующаяся пара ключ/UUID ('" + key + "') (узел " + node.id + ").");
            }
        }

        Map<String, GraphNode> saveByKey = new LinkedHashMap<>();
        for (GraphNode node : graph.nodes) {
            if (ScriptNodeTypes.isSavePoint(node.type)) {
                saveByKey.putIfAbsent(node.param("name", "").trim(), node);
            }
        }

        for (GraphNode node : graph.nodes) {
            if (!ScriptNodeTypes.isLoadPoint(node.type)) {
                continue;
            }
            String target = node.param("target", "").trim();
            if (target.isEmpty() || !target.matches("[A-Za-z0-9_]{1,64}")) {
                throw new ScenarioCompileException("Недопустимый ключ загрузки '" + target
                        + "' (узел " + node.id + "): разрешены [a-zA-Z0-9_]{1,64}.");
            }
            if (!saveByKey.containsKey(target)) {
                throw new ScenarioCompileException("Загрузка точки (узел " + node.id
                        + ") ссылается на несуществующий ключ '" + target + "'.");
            }
        }
        return saveByKey;
    }

    private static void checkJumpCycles(ActionGraph graph, Map<String, GraphNode> saveByKey) throws ScenarioCompileException {
        Map<String, List<String>> adjacency = new LinkedHashMap<>();
        for (GraphNode node : graph.nodes) {
            if (isSuspending(node.type)) {
                continue;
            }
            List<String> successors = new ArrayList<>();
            if (ScriptNodeTypes.isSavePoint(node.type)) {
                if (!node.boolParam("exit", false) && node.nextId != null) {
                    successors.add(node.nextId);
                }
            } else if (ScriptNodeTypes.isLoadPoint(node.type)) {
                if (node.nextId != null) {
                    successors.add(node.nextId);
                }
                String target = node.param("target", "").trim();
                for (GraphNode savePoint : graph.nodes) {
                    if (ScriptNodeTypes.isSavePoint(savePoint.type)
                            && savePoint.param("name", "").trim().equals(target)
                            && savePoint.nextId != null) {
                        successors.add(savePoint.nextId);
                    }
                }
            } else if (node.isBranch()) {
                for (GraphNode.BranchOption option : node.options) {
                    if (option.headId != null) {
                        successors.add(option.headId);
                    }
                }
                if (node.nextId != null) {
                    successors.add(node.nextId);
                }
            } else if (node.nextId != null) {
                successors.add(node.nextId);
            }
            adjacency.put(node.id, successors);
        }

        Map<String, Integer> color = new LinkedHashMap<>();
        for (String id : adjacency.keySet()) {
            if (color.getOrDefault(id, 0) == 0 && hasCycle(id, adjacency, color)) {
                throw new ScenarioCompileException("Обнаружен цикл без ожидающих действий "
                        + "(диалог/торговля/ожидание/движение): он зависнет на лимите инструкций.");
            }
        }
    }

    private static boolean hasCycle(String id, Map<String, List<String>> adjacency, Map<String, Integer> color) {
        color.put(id, 1);
        for (String next : adjacency.getOrDefault(id, List.of())) {
            if (!adjacency.containsKey(next)) {
                continue;
            }
            int state = color.getOrDefault(next, 0);
            if (state == 1) {
                return true;
            }
            if (state == 0 && hasCycle(next, adjacency, color)) {
                return true;
            }
        }
        color.put(id, 2);
        return false;
    }

    private static boolean isSuspending(String type) {
        return ScriptNodeTypes.DIALOGUE.equals(type)
                || ScriptNodeTypes.TRADE.equals(type)
                || ScriptNodeTypes.WAIT.equals(type)
                || ScriptNodeTypes.MOVE.equals(type);
    }

    private static void validateChain(ActionGraph graph, String headId, boolean update,
                                      Set<String> visited, List<String> branchTypes, List<String> path)
            throws ScenarioCompileException {
        String nodeId = headId;
        while (nodeId != null) {
            GraphNode node = graph.node(nodeId);
            if (node == null) {
                throw new ScenarioCompileException("Узел '" + nodeId + "' не найден в событии '" + graph.eventType + "'.");
            }
            if (!ScriptNodeTypes.isKnown(node.type)) {
                throw new ScenarioCompileException("Неизвестный тип узла: '" + node.type + "'.");
            }
            if (!visited.add(nodeId)) {
                throw new ScenarioCompileException("Цикл или fan-in: узел '" + nodeId + "' используется более одного раза.");
            }
            if (update && ScriptNodeTypes.requiresPlayer(node.type)) {
                throw new ScenarioCompileException("Действие '" + ScriptNodeTypes.displayName(node.type)
                        + "' нельзя использовать в событии 'update' (нет игрока).");
            }
            if (node.isBranch()) {
                if (node.options.isEmpty()) {
                    throw new ScenarioCompileException("Развилка '" + node.id + "' не имеет вариантов.");
                }
                if (node.type.equals(ScriptNodeTypes.QUEST_CHECK) && node.options.size() != 2) {
                    throw new ScenarioCompileException("Развилка-проверка квеста '" + node.id
                            + "' должна иметь ровно 2 ветки (выполнен / не выполнен).");
                }
                for (GraphNode.BranchOption option : node.options) {
                    if (node.type.equals(ScriptNodeTypes.DIALOGUE) && (option.label == null || option.label.isBlank())) {
                        throw new ScenarioCompileException("У развилки-диалога '" + node.id + "' пустой текст варианта.");
                    }
                    if (option.headId != null) {
                        validateChain(graph, option.headId, update, visited, branchTypes, path);
                    }
                }
            }
            nodeId = node.nextId;
        }
    }

    private static void emitChain(StringBuilder out, ActionGraph graph, String headId, boolean update,
                                  int indent, Set<String> visited, boolean comment) throws ScenarioCompileException {
        String nodeId = headId;
        while (nodeId != null) {
            GraphNode node = graph.node(nodeId);
            if (node == null || !visited.add(nodeId)) {
                throw new ScenarioCompileException("Повторный или отсутствующий узел: '" + nodeId + "'.");
            }

            if (node.isBranch()) {
                out.append(indent(indent)).append(ScriptMarkers.branchStart(node)).append('\n');
                for (int i = 0; i < node.options.size(); i++) {
                    GraphNode.BranchOption option = node.options.get(i);
                    emitCode(out, "function choice_" + (i + 1) + "_" + node.id + "(player)", indent, comment);
                    if (option.headId != null) {
                        emitChain(out, graph, option.headId, update, indent + 1, visited, comment);
                    }
                    emitCode(out, "end", indent, comment);
                }
                for (String line : ActionCodeProvider.branchDispatcher(node).split("\n", -1)) {
                    if (!line.isEmpty()) {
                        emitCode(out, line, indent, comment);
                    }
                }
                out.append(indent(indent)).append(ScriptMarkers.branchEnd(node.id)).append('\n');
            } else {
                out.append(indent(indent)).append(ScriptMarkers.actionStart(node)).append('\n');
                emitSubMarkers(out, node, indent);
                for (String line : ActionCodeProvider.codeLines(node)) {
                    emitCode(out, line, indent, comment);
                }
                out.append(indent(indent)).append(ScriptMarkers.actionEnd(node.id)).append('\n');
            }

            nodeId = node.nextId;
        }
    }

    private static void emitCode(StringBuilder out, String line, int indent, boolean comment) {
        out.append(indent(indent)).append(comment ? "-- " : "").append(line).append('\n');
    }

    private static void emitSubMarkers(StringBuilder out, GraphNode node, int indent) {
        if (ScriptNodeTypes.COMMAND.equals(node.type)) {
            String commands = node.param("commands", "");
            if (!commands.isEmpty()) {
                for (String command : commands.split("\n", -1)) {
                    if (!command.isEmpty()) {
                        out.append(indent(indent)).append(ScriptMarkers.commandLine(command)).append('\n');
                    }
                }
            }
        }
        if (ScriptNodeTypes.TRADE.equals(node.type)) {
            for (GraphNode.TradeOffer offer : node.offers) {
                out.append(indent(indent)).append(ScriptMarkers.tradeItem(offer)).append('\n');
            }
        }
    }

    static String indent(int level) {
        return "    ".repeat(Math.max(0, level));
    }
}
