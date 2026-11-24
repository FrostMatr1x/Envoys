package com.frost.envoys.client.gui.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioCompilerTest {

    @Test
    void goldenLinearOutput() throws Exception {
        ActionGraph graph = graph("click");
        GraphNode say = new GraphNode("1", ScriptNodeTypes.SAY);
        say.params.put("text", "Hello");
        say.nextId = "2";
        GraphNode wait = new GraphNode("2", ScriptNodeTypes.WAIT);
        wait.params.put("ticks", "20");
        graph.addNode(say);
        graph.addNode(wait);

        String out = compile(graph);
        String expected = String.join("\n",
                "-- @gui:event:start name=click",
                "envoys.on(\"click\", function(player)",
                "    -- @gui:action:start id=1 type=say next=2 text=Hello",
                "    envoys.say(\"Hello\")",
                "    -- @gui:action:end id=1",
                "    -- @gui:action:start id=2 type=wait next=null ticks=20",
                "    envoys.wait(20)",
                "    -- @gui:action:end id=2",
                "end)",
                "-- @gui:event:end name=click") + "\n";

        assertEquals(expected, out);
    }

    @Test
    void disabledEventCommentsOnlyCode() throws Exception {
        ActionGraph graph = graph("click");
        graph.enabled = false;
        GraphNode say = new GraphNode("1", ScriptNodeTypes.SAY);
        say.params.put("text", "Hello");
        graph.addNode(say);

        String out = compile(graph);
        assertTrue(out.contains("-- @gui:event:start name=click enabled=false"));
        assertTrue(out.contains("-- envoys.on(\"click\", function(player)"));
        assertTrue(out.contains("    -- @gui:action:start id=1 type=say next=null text=Hello"));
        assertTrue(out.contains("    -- envoys.say(\"Hello\")"));
        assertTrue(out.contains("-- @gui:action:end id=1"));
        assertTrue(out.contains("-- @gui:event:end name=click"));
    }

    @Test
    void updateAndRangeRequireArg() {
        ActionGraph update = graph("update");
        update.addNode(new GraphNode("1", ScriptNodeTypes.SAY));
        assertEquals("envoys.compile.event_arg_required", errorKey(update));

        ActionGraph range = graph("range");
        range.addNode(new GraphNode("1", ScriptNodeTypes.SAY));
        assertEquals("envoys.compile.event_arg_required", errorKey(range));
    }

    @Test
    void invalidArgFails() {
        ActionGraph graph = graph("update");
        graph.arg = "abc";
        graph.addNode(new GraphNode("1", ScriptNodeTypes.SAY));
        assertEquals("envoys.compile.event_arg_invalid", errorKey(graph));
    }

    @Test
    void updateAddsNilPlayerAndParsedArg() throws Exception {
        ActionGraph graph = graph("update");
        graph.arg = " 10 ";
        graph.addNode(new GraphNode("1", ScriptNodeTypes.SAY));

        String out = compile(graph);
        assertTrue(out.contains("arg=10"));
        assertTrue(out.contains("local player = nil"));
        assertTrue(out.contains("end, 10)"));
    }

    @Test
    void missingStartFails() {
        ActionGraph graph = graph("click");
        graph.addNode(new GraphNode("1", ScriptNodeTypes.SAY));
        graph.startId = null;
        assertEquals("envoys.compile.event_no_start", errorKey(graph));
    }

    @Test
    void unknownNodeTypeFails() {
        ActionGraph graph = graph("click");
        graph.addNode(new GraphNode("1", "wat"));
        assertEquals("envoys.compile.unknown_node_type", errorKey(graph));
    }

    @Test
    void fanInCycleFails() {
        ActionGraph graph = graph("click");
        GraphNode branch = new GraphNode("b", ScriptNodeTypes.DIALOGUE);
        branch.options.add(new GraphNode.BranchOption("a", "A", "x"));
        branch.options.add(new GraphNode.BranchOption("b", "B", "y"));
        GraphNode x = new GraphNode("x", ScriptNodeTypes.SAY);
        x.nextId = "z";
        GraphNode y = new GraphNode("y", ScriptNodeTypes.SAY);
        y.nextId = "z";
        GraphNode z = new GraphNode("z", ScriptNodeTypes.SAY);
        graph.addNode(branch);
        graph.addNode(x);
        graph.addNode(y);
        graph.addNode(z);

        assertEquals("envoys.compile.cycle_fanin", errorKey(graph));
    }

    @Test
    void playerActionInUpdateFails() {
        ActionGraph graph = graph("update");
        graph.arg = "1";
        GraphNode dialogue = new GraphNode("d", ScriptNodeTypes.DIALOGUE);
        dialogue.options.add(new GraphNode.BranchOption("a", "A", null));
        dialogue.options.add(new GraphNode.BranchOption("b", "B", null));
        graph.addNode(dialogue);

        assertEquals("envoys.compile.update_player_action", errorKey(graph));
    }

    @Test
    void emptyBranchFails() {
        ActionGraph graph = graph("click");
        graph.addNode(new GraphNode("b", ScriptNodeTypes.DIALOGUE));
        assertEquals("envoys.compile.branch_empty", errorKey(graph));
    }

    @Test
    void questCheckNeedsTwoBranches() {
        ActionGraph graph = graph("click");
        GraphNode quest = new GraphNode("q", ScriptNodeTypes.QUEST_CHECK);
        quest.options.add(new GraphNode.BranchOption("completed", "Completed", null));
        graph.addNode(quest);
        assertEquals("envoys.compile.quest_check_branches", errorKey(graph));
    }

    @Test
    void dialogueWithEmptyLabelFails() {
        ActionGraph graph = graph("click");
        GraphNode dialogue = new GraphNode("d", ScriptNodeTypes.DIALOGUE);
        dialogue.options.add(new GraphNode.BranchOption("a", "", null));
        dialogue.options.add(new GraphNode.BranchOption("b", "B", null));
        graph.addNode(dialogue);
        assertEquals("envoys.compile.dialogue_empty_option", errorKey(graph));
    }

    @Test
    void savePointValidationErrors() {
        assertEquals("envoys.compile.savepoint_key_invalid", errorKey(savePoint("bad key", "1")));
        assertEquals("envoys.compile.savepoint_key_reserved", errorKey(savePoint("start", "1")));
        assertEquals("envoys.compile.savepoint_cp_missing", errorKey(savePoint("spot", "")));

        ActionGraph duplicate = graph("click");
        duplicate.addNode(savePointValue("s1", "spot", "1"));
        duplicate.addNode(savePointValue("s2", "spot", "1"));
        assertEquals("envoys.compile.savepoint_duplicate", errorKey(duplicate));
    }

    @Test
    void loadPointValidationErrors() {
        assertEquals("envoys.compile.loadpoint_key_invalid", errorKey(loadPoint("bad key")));
        assertEquals("envoys.compile.loadpoint_target_missing", errorKey(loadPoint("spot")));
    }

    @Test
    void danglingNextFails() {
        ActionGraph graph = graph("click");
        GraphNode node = new GraphNode("1", ScriptNodeTypes.SAY);
        node.nextId = "missing";
        graph.addNode(node);
        assertEquals("envoys.compile.node_not_in_event", errorKey(graph));
    }

    @Test
    void disconnectedJumpCycleFails() {
        ActionGraph graph = graph("click");
        graph.addNode(new GraphNode("e", ScriptNodeTypes.SAY));

        GraphNode save = savePointValue("s", "spot", "1");
        save.nextId = "l";
        GraphNode load = new GraphNode("l", ScriptNodeTypes.LOAD_POINT);
        load.params.put("target", "spot");
        load.nextId = "s";
        graph.addNode(save);
        graph.addNode(load);

        assertEquals("envoys.compile.spin_cycle", errorKey(graph));
    }

    @Test
    void jumpSegmentsAreGenerated() throws Exception {
        ActionGraph graph = graph("click");
        GraphNode branch = new GraphNode("b", ScriptNodeTypes.DIALOGUE);
        branch.options.add(new GraphNode.BranchOption("save", "Save", "s"));
        branch.options.add(new GraphNode.BranchOption("load", "Load", "l"));

        GraphNode save = savePointValue("s", "spot", "1");
        save.nextId = "2";
        GraphNode two = new GraphNode("2", ScriptNodeTypes.SAY);

        GraphNode load = new GraphNode("l", ScriptNodeTypes.LOAD_POINT);
        load.params.put("target", "spot");
        load.nextId = "3";
        GraphNode three = new GraphNode("3", ScriptNodeTypes.SAY);

        graph.addNode(branch);
        graph.addNode(save);
        graph.addNode(two);
        graph.addNode(load);
        graph.addNode(three);

        String out = compile(graph);
        assertTrue(out.contains("function seg_click_start(player)"));
        assertTrue(out.contains("function seg_click_spot_s(player)"));
        assertTrue(out.contains("local _jmp_ = \"start\""));
        assertTrue(out.contains("while _jmp_ do"));
        assertTrue(out.contains("envoys.checkpoint.set(\"spot\", \"1\")"));
        assertTrue(out.contains("envoys.checkpoint.get(\"spot\")"));
        assertTrue(out.contains("return \"seg_click_spot_s\""));
    }

    @Test
    void validateFalseSwallowsNodeErrors() {
        ActionGraph graph = graph("click");
        GraphNode node = new GraphNode("1", ScriptNodeTypes.SAY);
        node.nextId = "missing";
        graph.addNode(node);

        assertDoesNotThrow(() -> ScenarioCompiler.compile(project(graph), ScriptProject.EVENT_ORDER, false));
    }

    @Test
    void globalCustomSourceIsPrepended() throws Exception {
        ScriptProject project = project(graph("click"));
        project.globalCustom = true;
        project.globalCustomSource = "-- custom\nlocal x = 1";

        String out = ScenarioCompiler.compile(project, ScriptProject.EVENT_ORDER);
        assertTrue(out.startsWith("-- custom\nlocal x = 1\n"));
    }

    @Test
    void lockedEventUsesRawSource() throws Exception {
        ScriptProject project = new ScriptProject();
        EventScript script = new EventScript();
        script.state = EventUiState.GREY_LOCKED_CUSTOM;
        script.rawSource = "-- raw";
        project.events.put("click", script);

        assertEquals("-- raw\n", ScenarioCompiler.compile(project, ScriptProject.EVENT_ORDER));
    }

    @Test
    void segmentLabelAndIndent() {
        GraphNode save = savePointValue("s", "spot", "1");
        assertEquals("seg_click_spot_s", ScenarioCompiler.segmentLabel("click", save));
        assertEquals("        ", ScenarioCompiler.indent(2));
    }

    private static String compile(ActionGraph graph) throws ScenarioCompileException {
        return ScenarioCompiler.compile(project(graph), ScriptProject.EVENT_ORDER);
    }

    private static String errorKey(ActionGraph graph) {
        ScenarioCompileException error = assertThrows(ScenarioCompileException.class, () -> compile(graph));
        return error.getMessage();
    }

    private static ScriptProject project(ActionGraph graph) {
        ScriptProject project = new ScriptProject();
        project.events.put(graph.eventType, new EventScript(graph));
        return project;
    }

    private static ActionGraph graph(String eventType) {
        ActionGraph graph = new ActionGraph(eventType);
        graph.present = true;
        graph.enabled = true;
        return graph;
    }

    private static GraphNode savePointValue(String id, String name, String cp) {
        GraphNode node = new GraphNode(id, ScriptNodeTypes.SAVE_POINT);
        node.params.put("name", name);
        node.params.put("cp", cp);
        return node;
    }

    private static ActionGraph savePoint(String name, String cp) {
        ActionGraph graph = graph("click");
        graph.addNode(savePointValue("s", name, cp));
        return graph;
    }

    private static ActionGraph loadPoint(String target) {
        ActionGraph graph = graph("click");
        GraphNode node = new GraphNode("l", ScriptNodeTypes.LOAD_POINT);
        node.params.put("target", target);
        graph.addNode(node);
        return graph;
    }
}
