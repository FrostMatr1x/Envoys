package com.frost.envoys.action.serialization;

import com.frost.envoys.TestSupport;
import com.frost.envoys.action.model.AbstractActionData;
import com.frost.envoys.action.model.ActionChat;
import com.frost.envoys.action.model.ActionCommand;
import com.frost.envoys.action.model.ActionDelay;
import com.frost.envoys.action.model.ActionDialog;
import com.frost.envoys.action.model.ActionLoadPoint;
import com.frost.envoys.action.model.ActionMerchantLevelUp;
import com.frost.envoys.action.model.ActionMove;
import com.frost.envoys.action.model.ActionQuestAdvanceStep;
import com.frost.envoys.action.model.ActionQuestCheck;
import com.frost.envoys.action.model.ActionQuestGive;
import com.frost.envoys.action.model.ActionQuestMarkCompleted;
import com.frost.envoys.action.model.ActionRandomizer;
import com.frost.envoys.action.model.ActionSavePoint;
import com.frost.envoys.action.model.ActionStart;
import com.frost.envoys.action.model.ActionTrade;
import com.frost.envoys.action.model.EntityActionData;
import com.frost.envoys.network.NetworkTestSupport;
import com.frost.envoys.npc.NPCTrade;
import com.frost.envoys.platform.TestAttachments;
import com.frost.envoys.util.ColorUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityActionSerializationTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void bootstrap() {
        NetworkTestSupport.bootStrap();
    }

    @BeforeEach
    void setUp() {
        TestSupport.installEnvironment(tempDir, new TestAttachments());
    }

    @AfterEach
    void tearDown() {
        TestSupport.clearEnvironment();
    }

    @ParameterizedTest
    @MethodSource("models")
    void deepRoundTripPreservesModelFields(EntityActionData model, String expectedType) {
        com.google.gson.JsonElement tree = EntityActionAdapter.GSON.toJsonTree(model);

        EntityActionData restored = EntityActionAdapter.GSON.fromJson(tree, EntityActionData.class);

        assertNotNull(restored);
        assertEquals(model.getClass(), restored.getClass());
        assertEquals(expectedType, restored.getType());
        assertEquals("act-1", restored.getId());
        assertEquals("next-1", ((AbstractActionData) restored).nextActionId);
        assertEquals(tree, EntityActionAdapter.GSON.toJsonTree(restored));
    }

    @Test
    void actionTradeRoundTripPreservesNestedTrades() {
        ActionTrade action = new ActionTrade("act-1");
        action.nextActionId = "next-1";
        action.baseSlots = 3;
        action.trades.add(new NPCTrade(
                new ItemStack(Items.STONE, 5),
                new ItemStack(Items.DIRT, 2),
                new ItemStack(Items.EMERALD, 1)));
        NPCTrade limited = new NPCTrade(
                new ItemStack(Items.GOLD_INGOT, 4),
                null,
                new ItemStack(Items.DIAMOND, 1));
        limited.maxTrades = 7;
        limited.priceMultiplier = 1.25f;
        limited.demand = 2;
        limited.resetTime = 60;
        action.trades.add(limited);

        ActionTrade restored = (ActionTrade) EntityActionAdapter.GSON.fromJson(
                EntityActionAdapter.GSON.toJsonTree(action), EntityActionData.class);

        assertNotNull(restored);
        assertEquals(3, restored.baseSlots);
        assertEquals(2, restored.trades.size());
        NPCTrade first = restored.trades.get(0);
        assertEquals(Items.STONE, first.input1.getItem());
        assertEquals(5, first.input1.getCount());
        assertEquals(Items.DIRT, first.input2.getItem());
        assertEquals(2, first.input2.getCount());
        assertEquals(Items.EMERALD, first.output.getItem());
        NPCTrade second = restored.trades.get(1);
        assertEquals(7, second.maxTrades);
        assertEquals(1.25f, second.priceMultiplier);
        assertEquals(2, second.demand);
        assertEquals(60, second.resetTime);
        assertTrue(second.input2 == null || second.input2.isEmpty());
    }

    @Test
    void actionDialogRoundTripPreservesLiteralMessageAndAnswers() {
        ActionDialog dialog = new ActionDialog("act-1", "Bob");
        dialog.nextActionId = "next-1";
        dialog.npcMessage = Component.literal("Hello there");
        dialog.addAnswer("Yes", "a1");
        dialog.addAnswer("No", "a2");

        ActionDialog restored = (ActionDialog) EntityActionAdapter.GSON.fromJson(
                EntityActionAdapter.GSON.toJsonTree(dialog), EntityActionData.class);

        assertNotNull(restored);
        assertEquals("Bob", restored.NPCName);
        assertEquals("Hello there", restored.npcMessage.getString());
        assertEquals("a1", restored.answers.get("Yes"));
        assertEquals("a2", restored.answers.get("No"));
    }

    @Test
    void actionDialogReadsLegacyColorString() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "dialog");
        json.addProperty("id", "act-1");
        json.addProperty("NPCName", "Bob");
        json.add("answers", new JsonObject());
        json.addProperty("npcMessage", "&aGreen");

        ActionDialog restored = (ActionDialog) EntityActionAdapter.GSON.fromJson(json, EntityActionData.class);

        assertNotNull(restored);
        assertNotNull(restored.npcMessage);
        assertEquals("Green", restored.npcMessage.getString());
        assertTrue(ColorUtils.toFormattedString(restored.npcMessage).contains("&#55FF55"));
    }

    @Test
    void actionSavePointKeepsCheckpointUuidAndExitFlag() {
        ActionSavePoint action = new ActionSavePoint("act-1");
        action.nextActionId = "next-1";
        action.saveId = "save-1";
        action.checkpointUuid = "00000000-0000-0000-0000-000000000001";
        action.exitOnSave = true;

        ActionSavePoint restored = (ActionSavePoint) EntityActionAdapter.GSON.fromJson(
                EntityActionAdapter.GSON.toJsonTree(action), EntityActionData.class);

        assertNotNull(restored);
        assertEquals("save-1", restored.saveId);
        assertEquals("00000000-0000-0000-0000-000000000001", restored.checkpointUuid);
        assertTrue(restored.exitOnSave);
    }

    @Test
    void adapterRejectsBlankType() {
        assertThrows(JsonParseException.class,
                () -> EntityActionAdapter.GSON.fromJson("{\"type\":\"\"}", EntityActionData.class));
    }

    @Test
    void adapterRejectsNonStringType() {
        assertThrows(JsonParseException.class,
                () -> EntityActionAdapter.GSON.fromJson("{\"type\":42}", EntityActionData.class));
    }

    private static Stream<Arguments> models() {
        ActionChat chat = new ActionChat("act-1");
        chat.nextActionId = "next-1";
        chat.message = "hello";
        chat.isGlobal = true;

        ActionCommand command = new ActionCommand("act-1");
        command.nextActionId = "next-1";
        command.commands.addAll(List.of("say one", "say two"));

        ActionDelay delay = new ActionDelay("act-1");
        delay.nextActionId = "next-1";
        delay.duration = 42;
        delay.timeUnit = 'm';

        ActionDialog dialog = new ActionDialog("act-1", "Bob");
        dialog.nextActionId = "next-1";
        dialog.npcMessage = Component.literal("Hello");
        dialog.addAnswer("Yes", "a1").addAnswer("No", "a2");

        ActionLoadPoint loadPoint = new ActionLoadPoint("act-1");
        loadPoint.nextActionId = "next-1";
        loadPoint.saveId = "save-1";

        ActionMerchantLevelUp merchantLevelUp = new ActionMerchantLevelUp("act-1");
        merchantLevelUp.nextActionId = "next-1";

        ActionMove move = new ActionMove("act-1");
        move.nextActionId = "next-1";
        move.targetX = 1.5f;
        move.targetY = 2.5f;
        move.targetZ = 3.5f;
        move.waitUntilReached = true;

        ActionQuestAdvanceStep advanceStep = new ActionQuestAdvanceStep("act-1");
        advanceStep.nextActionId = "next-1";
        advanceStep.questTarget = "quest-a";
        advanceStep.completionId = "step-a";

        ActionQuestCheck check = new ActionQuestCheck("act-1");
        check.nextActionId = "next-1";
        check.questTarget = "quest-a";
        check.actionIfCompleted = "done";
        check.actionIfNotCompleted = "pending";

        ActionQuestGive give = new ActionQuestGive("act-1");
        give.nextActionId = "next-1";
        give.questTarget = "quest-a";

        ActionQuestMarkCompleted markCompleted = new ActionQuestMarkCompleted("act-1");
        markCompleted.nextActionId = "next-1";
        markCompleted.questTarget = "quest-a";

        ActionRandomizer randomizer = new ActionRandomizer("act-1");
        randomizer.nextActionId = "next-1";
        randomizer.options.clear();
        randomizer.options.addAll(List.of("alpha", "beta"));

        ActionSavePoint savePoint = new ActionSavePoint("act-1");
        savePoint.nextActionId = "next-1";
        savePoint.saveId = "save-1";
        savePoint.checkpointUuid = "00000000-0000-0000-0000-000000000001";
        savePoint.exitOnSave = true;

        ActionStart start = new ActionStart("act-1");
        start.nextActionId = "next-1";

        ActionTrade trade = new ActionTrade("act-1");
        trade.nextActionId = "next-1";
        trade.baseSlots = 2;
        trade.trades.add(new NPCTrade(
                new ItemStack(Items.STONE, 5),
                new ItemStack(Items.DIRT, 2),
                new ItemStack(Items.EMERALD, 1)));

        return Stream.of(
                Arguments.of(chat, "chat"),
                Arguments.of(command, "command"),
                Arguments.of(delay, "delay"),
                Arguments.of(dialog, "dialog"),
                Arguments.of(loadPoint, "load_point"),
                Arguments.of(merchantLevelUp, "merchant_level_up"),
                Arguments.of(move, "move"),
                Arguments.of(advanceStep, "quest_advance_step"),
                Arguments.of(check, "quest_check"),
                Arguments.of(give, "quest_give"),
                Arguments.of(markCompleted, "quest_mark_completed"),
                Arguments.of(randomizer, "randomizer"),
                Arguments.of(savePoint, "save_point"),
                Arguments.of(start, "start"),
                Arguments.of(trade, "trade"));
    }
}
