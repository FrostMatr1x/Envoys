package com.frost.envoys.client.gui.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioDecompilerTest {

    @Test
    void linearRoundTrip() throws Exception {
        ActionGraph graph = graph("click");
        GraphNode say = new GraphNode("1", ScriptNodeTypes.SAY);
        say.params.put("text", "Hello");
        say.nextId = "2";
        GraphNode wait = new GraphNode("2", ScriptNodeTypes.WAIT);
        wait.params.put("ticks", "20");
        graph.addNode(say);
        graph.addNode(wait);

        String first = compile(graph);
        ScriptProject parsed = ScenarioDecompiler.decompile(first, ScriptProject.EVENT_ORDER);
        String second = ScenarioCompiler.compile(parsed, ScriptProject.EVENT_ORDER);

        assertEquals(first, second);
    }

    @Test
    void dialogueIsRestored() throws Exception {
        ActionGraph graph = graph("click");
        GraphNode dialogue = new GraphNode("d", ScriptNodeTypes.DIALOGUE);
        dialogue.params.put("text", "Hi");
        dialogue.options.add(new GraphNode.BranchOption("a", "A", "1"));
        dialogue.options.add(new GraphNode.BranchOption("b", "B", "2"));
        graph.addNode(dialogue);
        graph.addNode(new GraphNode("1", ScriptNodeTypes.SAY));
        graph.addNode(new GraphNode("2", ScriptNodeTypes.SAY));

        EventScript script = decompileEvent(compile(graph), "click");
        assertEquals(EventUiState.GREEN_SYNCED, script.state);
        GraphNode parsed = script.graph.node("d");
        assertNotNull(parsed);
        assertEquals(2, parsed.options.size());
        assertEquals("a", parsed.options.get(0).key);
        assertEquals("A", parsed.options.get(0).label);
        assertEquals("1", parsed.options.get(0).headId);
        assertEquals("b", parsed.options.get(1).key);
        assertEquals("B", parsed.options.get(1).label);
        assertEquals("2", parsed.options.get(1).headId);
    }

    @Test
    void randomIsRestored() throws Exception {
        ActionGraph graph = graph("click");
        GraphNode random = new GraphNode("r", ScriptNodeTypes.RANDOM);
        random.options.add(new GraphNode.BranchOption("1", "Option 1", null));
        random.options.add(new GraphNode.BranchOption("2", "Option 2", null));
        graph.addNode(random);

        EventScript script = decompileEvent(compile(graph), "click");
        GraphNode parsed = script.graph.node("r");
        assertNotNull(parsed);
        assertEquals(2, parsed.options.size());
    }

    @Test
    void questCheckIsRestored() throws Exception {
        ActionGraph graph = graph("click");
        GraphNode quest = new GraphNode("q", ScriptNodeTypes.QUEST_CHECK);
        quest.params.put("quest", "quest1");
        quest.options.add(new GraphNode.BranchOption("completed", "Completed", null));
        quest.options.add(new GraphNode.BranchOption("not_completed", "Not completed", null));
        graph.addNode(quest);

        EventScript script = decompileEvent(compile(graph), "click");
        GraphNode parsed = script.graph.node("q");
        assertNotNull(parsed);
        assertEquals(2, parsed.options.size());
        assertEquals("completed", parsed.options.get(0).key);
        assertEquals("not_completed", parsed.options.get(1).key);
    }

    @Test
    void tradeOffersAreRestored() throws Exception {
        ActionGraph graph = graph("click");
        GraphNode trade = new GraphNode("t", ScriptNodeTypes.TRADE);
        GraphNode.TradeOffer offer = new GraphNode.TradeOffer();
        offer.in1 = "minecraft:emerald";
        offer.in1count = 3;
        offer.in2 = "minecraft:book";
        offer.in2count = 2;
        offer.out = "minecraft:diamond";
        offer.outcount = 1;
        offer.priceMultiplier = 1.5f;
        offer.demand = 1;
        offer.maxTrades = 5;
        offer.resetTime = 10;
        offer.requiredLevel = 2;
        trade.offers.add(offer);
        graph.addNode(trade);

        EventScript script = decompileEvent(compile(graph), "click");
        GraphNode parsed = script.graph.node("t");
        assertNotNull(parsed);
        assertEquals(1, parsed.offers.size());
        GraphNode.TradeOffer restored = parsed.offers.get(0);
        assertEquals("minecraft:emerald", restored.in1);
        assertEquals(3, restored.in1count);
        assertEquals("minecraft:book", restored.in2);
        assertEquals(2, restored.in2count);
        assertEquals("minecraft:diamond", restored.out);
        assertEquals(1, restored.outcount);
        assertEquals(1.5f, restored.priceMultiplier);
        assertEquals(1, restored.demand);
        assertEquals(5, restored.maxTrades);
        assertEquals(10, restored.resetTime);
        assertEquals(2, restored.requiredLevel);
    }

    @Test
    void multilineCommandIsRestored() throws Exception {
        ActionGraph graph = graph("click");
        GraphNode command = new GraphNode("c", ScriptNodeTypes.COMMAND);
        command.params.put("commands", "say a\nsay b\n");
        graph.addNode(command);

        EventScript script = decompileEvent(compile(graph), "click");
        GraphNode parsed = script.graph.node("c");
        assertNotNull(parsed);
        assertEquals("say a\nsay b", parsed.params.get("commands"));
    }

    @Test
    void allEventsAreCreatedFromOrder() {
        ScriptProject project = ScenarioDecompiler.decompile("", ScriptProject.EVENT_ORDER);
        assertEquals(ScriptProject.EVENT_ORDER.size(), project.events.size());
        for (String event : ScriptProject.EVENT_ORDER) {
            EventScript script = project.event(event);
            assertNotNull(script);
            assertEquals(EventUiState.GREEN_SYNCED, script.state);
            assertNotNull(script.graph);
            assertEquals(event, script.graph.eventType);
        }
    }

    @Test
    void unknownEventGoesToGlobalCustom() {
        String source = String.join("\n",
                "-- @gui:event:start name=foo",
                "envoys.on(\"foo\", function(player)",
                "end)",
                "-- @gui:event:end name=foo");

        ScriptProject project = ScenarioDecompiler.decompile(source, ScriptProject.EVENT_ORDER);
        assertTrue(project.globalCustom);
        assertNull(project.event("foo"));
        assertTrue(project.globalCustomSource.contains("event:start name=foo"));
    }

    @Test
    void unknownMarkerLocksEvent() {
        String source = String.join("\n",
                "-- @gui:event:start name=click",
                "-- @gui:weird:x",
                "-- @gui:event:end name=click");

        ScriptProject project = ScenarioDecompiler.decompile(source, ScriptProject.EVENT_ORDER);
        EventScript script = project.event("click");
        assertEquals(EventUiState.GREY_LOCKED_CUSTOM, script.state);
        assertNull(script.graph);
        assertTrue(script.rawSource.contains("weird:x"));
    }

    @Test
    void branchEndWithoutStartLocksEvent() {
        String source = String.join("\n",
                "-- @gui:event:start name=click",
                "-- @gui:branch:end id=1",
                "-- @gui:event:end name=click");

        EventScript script = ScenarioDecompiler.decompile(source, ScriptProject.EVENT_ORDER).event("click");
        assertEquals(EventUiState.GREY_LOCKED_CUSTOM, script.state);
        assertNull(script.graph);
    }

    @Test
    void commentsOutsideEventsAreNotCustom() {
        ScriptProject project = ScenarioDecompiler.decompile("-- just a comment\n", ScriptProject.EVENT_ORDER);
        assertFalse(project.globalCustom);
        assertEquals("-- just a comment", project.globalCustomSource);
    }

    @Test
    void unclosedEventBecomesCustom() {
        String source = String.join("\n",
                "-- @gui:event:start name=click",
                "envoys.on(\"click\", function(player)");

        ScriptProject project = ScenarioDecompiler.decompile(source, ScriptProject.EVENT_ORDER);
        assertTrue(project.globalCustom);
    }

    @Test
    void disabledEventIsRestored() throws Exception {
        ActionGraph graph = graph("click");
        graph.enabled = false;
        GraphNode say = new GraphNode("1", ScriptNodeTypes.SAY);
        say.params.put("text", "Hello");
        graph.addNode(say);

        EventScript script = decompileEvent(compile(graph), "click");
        assertEquals(EventUiState.GREEN_SYNCED, script.state);
        assertNotNull(script.graph);
        assertFalse(script.graph.enabled);
        assertNotNull(script.graph.node("1"));
    }

    @Test
    void globalCustomSourceIsPreserved() {
        ScriptProject project = ScenarioDecompiler.decompile("local x = 1\n", ScriptProject.EVENT_ORDER);
        assertTrue(project.globalCustom);
        assertEquals("local x = 1", project.globalCustomSource);
    }

    private static EventScript decompileEvent(String source, String event) {
        ScriptProject project = ScenarioDecompiler.decompile(source, ScriptProject.EVENT_ORDER);
        EventScript script = project.event(event);
        assertNotNull(script);
        return script;
    }

    private static String compile(ActionGraph graph) throws ScenarioCompileException {
        ScriptProject project = new ScriptProject();
        project.events.put(graph.eventType, new EventScript(graph));
        return ScenarioCompiler.compile(project, ScriptProject.EVENT_ORDER);
    }

    private static ActionGraph graph(String eventType) {
        ActionGraph graph = new ActionGraph(eventType);
        graph.present = true;
        graph.enabled = true;
        return graph;
    }
}
