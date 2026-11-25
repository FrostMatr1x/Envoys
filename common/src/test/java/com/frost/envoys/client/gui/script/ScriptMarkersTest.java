package com.frost.envoys.client.gui.script;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptMarkersTest {

    @Test
    void escapeAndUnescapeRoundTrip() {
        String raw = "a\"b\\c\nd\re\tf";
        assertEquals(raw, ScriptMarkers.unescape(ScriptMarkers.escape(raw)));
        assertEquals("", ScriptMarkers.escape(null));
        assertEquals("", ScriptMarkers.unescape(null));
        assertEquals("\"a b\"", ScriptMarkers.quote("a b"));
    }

    @Test
    void formatAndParseList() {
        assertEquals("\"A\",\"B\"", ScriptMarkers.formatList(List.of("A", "B")));
        assertEquals(List.of("A", "B"), ScriptMarkers.parseList("\"A\",\"B\""));
        assertEquals(List.of("a,b", "c d"), ScriptMarkers.parseList("\"a,b\", \"c d\""));
        assertEquals("\"a,b\",\"c d\"", ScriptMarkers.formatList(List.of("a,b", "c d")));
        assertEquals(List.of("a\"b"), ScriptMarkers.parseList(ScriptMarkers.formatList(List.of("a\"b"))));
        assertEquals(List.of(""), ScriptMarkers.parseList("\"\""));
        assertEquals(List.of("a", "b", "c"), ScriptMarkers.parseList("a, b , c"));
        assertEquals(List.of(), ScriptMarkers.parseList(""));
        assertEquals(List.of(), ScriptMarkers.parseList(null));
    }

    @Test
    void parseAttrsKeepsQuotedValues() {
        Map<String, String> attrs = ScriptMarkers.parseAttrs("a=1 b=\"x y\" z=");
        assertEquals("1", attrs.get("a"));
        assertEquals("\"x y\"", attrs.get("b"));
        assertEquals("x y", ScriptMarkers.value(attrs.get("b")));
        assertEquals("", attrs.get("z"));
        assertEquals("", ScriptMarkers.value(null));
        assertTrue(ScriptMarkers.parseAttrs(null).isEmpty());
        assertTrue(ScriptMarkers.parseAttrs("   ").isEmpty());
    }

    @Test
    void kindAndAttrsOnlyForMarkers() {
        assertEquals("action:start", ScriptMarkers.kind("-- @gui:action:start id=1"));
        assertEquals("1", ScriptMarkers.attrs("-- @gui:action:start id=1").get("id"));
        assertNull(ScriptMarkers.kind("not a marker"));
        assertTrue(ScriptMarkers.attrs("not a marker").isEmpty());
        assertFalse(ScriptMarkers.isMarker("not a marker"));
    }

    @Test
    void actionStartLine() {
        GraphNode node = new GraphNode("1", ScriptNodeTypes.SAY);
        node.nextId = "2";
        node.params.put("text", "Hello world");

        assertEquals("-- @gui:action:start id=1 type=say next=2 text=\"Hello world\"",
                ScriptMarkers.actionStart(node));

        node.nextId = null;
        assertTrue(ScriptMarkers.actionStart(node).contains("next=null"));
    }

    @Test
    void actionStartTradeHidesCommandsAndAddsOffers() {
        GraphNode node = new GraphNode("t", ScriptNodeTypes.TRADE);
        node.params.put("commands", "a\nb");
        node.offers.add(new GraphNode.TradeOffer());
        node.offers.add(new GraphNode.TradeOffer());

        String line = ScriptMarkers.actionStart(node);
        assertTrue(line.contains("offers=2"));
        assertFalse(line.contains("commands="));
    }

    @Test
    void branchStartDialogueRandomQuestCheck() {
        GraphNode dialogue = new GraphNode("d", ScriptNodeTypes.DIALOGUE);
        dialogue.params.put("text", "Hi");
        dialogue.options.add(new GraphNode.BranchOption("a", "A", "1"));
        dialogue.options.add(new GraphNode.BranchOption("b", "B", "2"));
        assertEquals("-- @gui:branch:start id=d type=dialogue_choice next=null text=Hi "
                        + "options=\"A\",\"B\" keys=\"a\",\"b\"",
                ScriptMarkers.branchStart(dialogue));

        GraphNode random = new GraphNode("r", ScriptNodeTypes.RANDOM);
        random.options.add(new GraphNode.BranchOption("1", "Option 1", null));
        random.options.add(new GraphNode.BranchOption("2", "Option 2", null));
        assertEquals("-- @gui:branch:start id=r type=random_choice next=null count=2",
                ScriptMarkers.branchStart(random));

        GraphNode quest = new GraphNode("q", ScriptNodeTypes.QUEST_CHECK);
        quest.params.put("quest", "quest1");
        assertEquals("-- @gui:branch:start id=q type=quest_check next=null quest=quest1",
                ScriptMarkers.branchStart(quest));
    }

    @Test
    void tradeItemLine() {
        GraphNode.TradeOffer offer = new GraphNode.TradeOffer();
        offer.in1 = "minecraft:emerald";
        offer.in2 = "";
        offer.out = "minecraft:diamond";

        assertEquals("-- @gui:trade:item in1=minecraft:emerald in1count=1 in2=\"\" in2count=1 "
                        + "out=minecraft:diamond outcount=1 priceMultiplier=1.0 demand=0 "
                        + "maxTrades=-1 resetTime=-1 requiredLevel=0",
                ScriptMarkers.tradeItem(offer));
    }

    @Test
    void eventAndCommandLines() {
        assertEquals("-- @gui:event:start name=click", ScriptMarkers.eventStart("click", "", true));
        assertEquals("-- @gui:event:start name=range arg=5 enabled=false",
                ScriptMarkers.eventStart("range", "5", false));
        assertEquals("-- @gui:event:end name=range", ScriptMarkers.eventEnd("range"));
        assertEquals("-- @gui:command:line text=\"say hi\"", ScriptMarkers.commandLine("say hi"));
    }
}
