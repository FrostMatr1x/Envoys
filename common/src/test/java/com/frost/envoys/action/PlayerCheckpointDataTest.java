package com.frost.envoys.action;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCheckpointDataTest {

    @Test
    void putAndGet() {
        PlayerCheckpointData data = new PlayerCheckpointData();
        assertNull(data.get("npc", "save"));

        data.put("npc", "save", "cp");
        assertEquals("cp", data.get("npc", "save"));
        assertFalse(data.keys("npc").isEmpty());
    }

    @Test
    void blankValuesAreIgnored() {
        PlayerCheckpointData data = new PlayerCheckpointData();
        data.put(null, "save", "cp");
        data.put("npc", " ", "cp");
        data.put("npc", "save", "");
        assertNull(data.get("npc", "save"));
        assertNull(data.get(null, "save"));
        assertNull(data.get("npc", null));
        assertTrue(data.keys(null).isEmpty());
        assertTrue(data.keys(" ").isEmpty());
    }

    @Test
    void removeAndKeys() {
        PlayerCheckpointData data = new PlayerCheckpointData();
        data.put("npc", "a", "cp1");
        data.put("npc", "b", "cp2");

        Set<String> keys = data.keys("npc");
        assertEquals(Set.of("a", "b"), keys);
        keys.clear();
        assertEquals(Set.of("a", "b"), data.keys("npc"));

        assertTrue(data.remove("npc", "a"));
        assertFalse(data.remove("npc", "a"));
        assertFalse(data.remove("missing", "a"));
        assertTrue(data.remove("npc", "b"));
        assertTrue(data.keys("npc").isEmpty());
        assertTrue(data.asMap().isEmpty());
    }

    @Test
    void constructorCopiesNestedMaps() {
        Map<String, String> inner = new HashMap<>();
        inner.put("save", "cp");
        Map<String, Map<String, String>> source = new HashMap<>();
        source.put("npc", inner);

        PlayerCheckpointData data = new PlayerCheckpointData(source);
        inner.put("save", "changed");
        source.clear();

        assertEquals("cp", data.get("npc", "save"));
    }

    @Test
    void codecRoundTrip() {
        PlayerCheckpointData data = new PlayerCheckpointData();
        data.put("npc", "save", "cp");

        JsonElement encoded = PlayerCheckpointData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        PlayerCheckpointData decoded = PlayerCheckpointData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals("cp", decoded.get("npc", "save"));
    }
}
