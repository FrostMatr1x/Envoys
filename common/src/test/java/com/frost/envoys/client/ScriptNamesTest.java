package com.frost.envoys.client;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptNamesTest {

    @Test
    void transliteratesRussian() {
        assertEquals("Ivan", ScriptNames.sanitize("Иван"));
        assertEquals("Privet_mir", ScriptNames.sanitize("Привет, мир!"));
        assertEquals("ezh", ScriptNames.sanitize("ёж"));
    }

    @Test
    void sanitizesUnsafeCharacters() {
        assertEquals("a_b", ScriptNames.sanitize("a  b"));
        assertEquals("npc", ScriptNames.sanitize("!!!"));
        assertEquals("npc", ScriptNames.sanitize(null));
        assertEquals("A-b_c", ScriptNames.sanitize("A-b_c"));
        assertEquals(32, ScriptNames.sanitize("a".repeat(40)).length());
    }

    @Test
    void fileNameUsesDeterministicSuffix() {
        UUID uuid = new UUID(0x123456789abcdef0L, 0L);
        assertEquals("Ivan_1234.lua", ScriptNames.fileName("Иван", uuid));
        assertEquals("npc_0000.lua", ScriptNames.fileName(null, null));
        assertEquals("a_0000.lua", ScriptNames.fileName("a", null));
    }
}
