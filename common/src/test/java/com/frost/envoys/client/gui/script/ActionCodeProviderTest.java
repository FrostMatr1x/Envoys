package com.frost.envoys.client.gui.script;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionCodeProviderTest {

    @Test
    void luaStringEscapesSpecialCharacters() {
        String raw = "a\"b\\c\nd\re\tf";
        assertEquals("\"a\\\"b\\\\c\\nd\\re\\tf\"", ActionCodeProvider.luaString(raw));
        assertEquals("\"\"", ActionCodeProvider.luaString(null));
    }

    @Test
    void floatLiteralHandlesNonFiniteValues() {
        assertEquals("0.0", ActionCodeProvider.floatLiteral(Float.NaN));
        assertEquals("0.0", ActionCodeProvider.floatLiteral(Float.POSITIVE_INFINITY));
        assertEquals("0.0", ActionCodeProvider.floatLiteral(Float.NEGATIVE_INFINITY));
        assertEquals("1.5", ActionCodeProvider.floatLiteral(1.5f));
    }

    @Test
    void codeLinesForLinearNodes() {
        assertEquals(List.of("envoys.say(\"Hi\")"), lines(ScriptNodeTypes.SAY, "text", "Hi"));
        assertEquals(List.of("envoys.wait(20)"), lines(ScriptNodeTypes.WAIT, "ticks", " 20 "));
        assertEquals(List.of("envoys.move(1.5, 2.0, 3.0)"),
                lines(ScriptNodeTypes.MOVE, "x", "1.5", "y", "2", "z", "3"));
        assertEquals(List.of("envoys.quest.start(\"q\", player)"),
                lines(ScriptNodeTypes.QUEST_START, "quest", "q"));
        assertEquals(List.of("envoys.quest.advance(\"q\", 2, player)"),
                lines(ScriptNodeTypes.QUEST_ADVANCE, "quest", "q", "amount", "2"));
        assertEquals(List.of("envoys.quest.complete(\"q\", player)"),
                lines(ScriptNodeTypes.QUEST_COMPLETE, "quest", "q"));
    }

    @Test
    void codeLinesForCommandSkipsEmptyLines() {
        GraphNode node = new GraphNode("c", ScriptNodeTypes.COMMAND);
        node.params.put("commands", "say a\n\nsay b");
        assertEquals(List.of("envoys.command(\"say a\")", "envoys.command(\"say b\")"),
                ActionCodeProvider.codeLines(node));
    }

    @Test
    void codeLinesForLookAt() {
        GraphNode player = new GraphNode("l", ScriptNodeTypes.LOOK_AT);
        assertEquals(List.of("envoys.lookAt(player)"), ActionCodeProvider.codeLines(player));

        GraphNode coords = new GraphNode("l", ScriptNodeTypes.LOOK_AT);
        coords.params.put("mode", "coords");
        coords.params.put("x", "1");
        coords.params.put("y", "2");
        coords.params.put("z", "3");
        assertEquals(List.of("envoys.lookAt({x=1.0, y=2.0, z=3.0})"),
                ActionCodeProvider.codeLines(coords));
    }

    @Test
    void codeLinesForNonCodeNodes() {
        assertTrue(ActionCodeProvider.codeLines(new GraphNode("s", ScriptNodeTypes.START)).isEmpty());
        assertTrue(ActionCodeProvider.codeLines(new GraphNode("d", ScriptNodeTypes.DIALOGUE)).isEmpty());
        assertTrue(ActionCodeProvider.codeLines(new GraphNode("r", ScriptNodeTypes.RANDOM)).isEmpty());
        assertTrue(ActionCodeProvider.codeLines(new GraphNode("s", ScriptNodeTypes.SAVE_POINT)).isEmpty());
        assertTrue(ActionCodeProvider.codeLines(new GraphNode("l", ScriptNodeTypes.LOAD_POINT)).isEmpty());
    }

    @Test
    void tradeCallSkipsEmptySecondInputAndClampsCounts() {
        GraphNode node = new GraphNode("t", ScriptNodeTypes.TRADE);
        GraphNode.TradeOffer offer = new GraphNode.TradeOffer();
        offer.in1 = "minecraft:emerald";
        offer.in1count = 0;
        offer.in2 = "";
        offer.in2count = 5;
        offer.out = "minecraft:diamond";
        offer.outcount = 0;
        offer.priceMultiplier = 1.25f;
        offer.demand = 2;
        node.offers.add(offer);

        String call = ActionCodeProvider.tradeCall(node);
        assertTrue(call.contains("in1count=1"));
        assertTrue(call.contains("outcount=1"));
        assertFalse(call.contains("in2="));
        assertTrue(call.contains("priceMultiplier=1.25"));
        assertFalse(call.contains("requiredLevel="));

        offer.in2 = "minecraft:book";
        offer.in2count = 0;
        offer.requiredLevel = 3;
        call = ActionCodeProvider.tradeCall(node);
        assertTrue(call.contains("in2=\"minecraft:book\""));
        assertTrue(call.contains("in2count=1"));
        assertTrue(call.contains("requiredLevel=3"));
    }

    @Test
    void dialogueDispatcher() {
        GraphNode node = new GraphNode("d", ScriptNodeTypes.DIALOGUE);
        node.params.put("text", "Hi");
        node.options.add(new GraphNode.BranchOption("a", "A", null));
        node.options.add(new GraphNode.BranchOption("b", "B", null));

        String code = ActionCodeProvider.branchDispatcher(node);
        assertTrue(code.contains("local _ans_d = envoys.dialogue(\"Hi\", {[\"A\"] = \"a\", [\"B\"] = \"b\"}, player)"));
        assertTrue(code.contains("if _ans_d == \"a\" then choice_1_d(player)"));
        assertTrue(code.contains("elseif _ans_d == \"b\" then choice_2_d(player)"));
    }

    @Test
    void randomDispatcher() {
        GraphNode node = new GraphNode("r", ScriptNodeTypes.RANDOM);
        node.options.add(new GraphNode.BranchOption("1", "Option 1", null));
        node.options.add(new GraphNode.BranchOption("2", "Option 2", null));

        String code = ActionCodeProvider.branchDispatcher(node);
        assertTrue(code.contains("local r_r = math.random(2)"));
        assertTrue(code.contains("if r_r == 1 then choice_1_r(player)"));
        assertTrue(code.contains("elseif r_r == 2 then choice_2_r(player)"));
    }

    @Test
    void questCheckDispatcherResetsCompletedQuest() {
        GraphNode node = new GraphNode("q", ScriptNodeTypes.QUEST_CHECK);
        node.params.put("quest", "quest1");

        String code = ActionCodeProvider.branchDispatcher(node);
        assertTrue(code.contains("envoys.quest.status(\"quest1\", player) == \"COMPLETED\""));
        assertTrue(code.contains("envoys.quest.reset(\"quest1\", player)"));
        assertTrue(code.contains("choice_1_q(player)"));
        assertTrue(code.contains("choice_2_q(player)"));
    }

    @Test
    void jumpDispatchersReturnResult() {
        GraphNode dialogue = new GraphNode("d", ScriptNodeTypes.DIALOGUE);
        dialogue.params.put("text", "Hi");
        dialogue.options.add(new GraphNode.BranchOption("a", "A", null));
        assertTrue(ActionCodeProvider.branchDispatcherJump(dialogue).contains("local _ret_d"));
        assertTrue(ActionCodeProvider.branchDispatcherJump(dialogue).contains("return _ret_d"));

        GraphNode random = new GraphNode("r", ScriptNodeTypes.RANDOM);
        random.options.add(new GraphNode.BranchOption("1", "Option 1", null));
        assertTrue(ActionCodeProvider.branchDispatcherJump(random).contains("local _ret_r"));

        GraphNode quest = new GraphNode("q", ScriptNodeTypes.QUEST_CHECK);
        assertTrue(ActionCodeProvider.branchDispatcherJump(quest).contains("local _ret_q"));
    }

    private static List<String> lines(String type, String... keyValues) {
        GraphNode node = new GraphNode("1", type);
        for (int i = 0; i < keyValues.length; i += 2) {
            node.params.put(keyValues[i], keyValues[i + 1]);
        }
        return ActionCodeProvider.codeLines(node);
    }
}
