package com.frost.envoys.action.model;

import com.frost.envoys.action.serialization.EntityActionAdapter;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionModelSmokeTest {

    @ParameterizedTest
    @MethodSource("actionModels")
    void modelSerializesAndDeserializes(EntityActionData model, String expectedType) {
        assertEquals("id", model.getId());
        assertEquals(expectedType, model.getType());

        AbstractActionData data = (AbstractActionData) model;
        data.nextActionId = "next";

        JsonElement tree = EntityActionAdapter.GSON.toJsonTree(model);
        assertEquals(expectedType, tree.getAsJsonObject().get("type").getAsString());

        EntityActionData restored = EntityActionAdapter.GSON.fromJson(tree, EntityActionData.class);

        assertNotNull(restored);
        assertEquals(model.getClass(), restored.getClass());
        assertEquals(expectedType, restored.getType());
        assertEquals("id", restored.getId());
        assertEquals("next", ((AbstractActionData) restored).nextActionId);
    }

    @Test
    void actionTypesAreNonBlankAndUnique() {
        Set<String> types = new HashSet<>();
        for (EntityActionData model : allModels()) {
            assertFalse(model.getType().isBlank());
            assertTrue(types.add(model.getType()), model.getType());
        }
        assertEquals(15, types.size());
    }

    @Test
    void adapterReturnsNullForJsonNull() {
        assertNull(EntityActionAdapter.GSON.fromJson("null", EntityActionData.class));
    }

    @Test
    void adapterRejectsNonObjectPayload() {
        assertThrows(JsonParseException.class,
                () -> EntityActionAdapter.GSON.fromJson("\"text\"", EntityActionData.class));
    }

    @Test
    void adapterRejectsMissingType() {
        assertThrows(JsonParseException.class,
                () -> EntityActionAdapter.GSON.fromJson("{}", EntityActionData.class));
        assertThrows(JsonParseException.class,
                () -> EntityActionAdapter.GSON.fromJson("{\"type\":null}", EntityActionData.class));
    }

    @Test
    void adapterRejectsUnknownType() {
        assertThrows(JsonParseException.class,
                () -> EntityActionAdapter.GSON.fromJson("{\"type\":\"nope\"}", EntityActionData.class));
    }

    private static Stream<Arguments> actionModels() {
        return allModels().stream().map(model -> Arguments.of(model, model.getType()));
    }

    private static List<EntityActionData> allModels() {
        return List.of(
                new ActionChat("id"),
                new ActionCommand("id"),
                new ActionDelay("id"),
                new ActionDialog("id", "NPC"),
                new ActionLoadPoint("id"),
                new ActionMerchantLevelUp("id"),
                new ActionMove("id"),
                new ActionQuestAdvanceStep("id"),
                new ActionQuestCheck("id"),
                new ActionQuestGive("id"),
                new ActionQuestMarkCompleted("id"),
                new ActionRandomizer("id"),
                new ActionSavePoint("id"),
                new ActionStart("id"),
                new ActionTrade("id"));
    }
}
