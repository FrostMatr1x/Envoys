package com.frost.envoys.lua;

import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LuaStringsTest {

    @Test
    void utf8RoundTrip() {
        assertEquals("Привет, мир", LuaStrings.toJava(LuaStrings.toLua("Привет, мир")));
    }

    @Test
    void nullBecomesEmptyString() {
        assertEquals("", LuaStrings.toJava(LuaStrings.toLua(null)));
    }

    @Test
    void nonStringValueThrowsLuaError() {
        LuaValue value = Constants.TRUE;
        assertThrows(LuaError.class, () -> LuaStrings.toJava(value));
    }
}
