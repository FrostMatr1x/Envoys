package com.frost.envoys.lua;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.LuaValue;

import java.nio.charset.StandardCharsets;

public final class LuaStrings {

    private LuaStrings() {
    }

    public static String toJava(LuaString string) {
        int length = string.length();
        byte[] bytes = new byte[length];
        string.copyTo(0, bytes, 0, length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String toJava(LuaValue value) throws LuaError {
        return toJava(value.checkLuaString());
    }

    public static LuaString toLua(String string) {
        return LuaString.valueOf((string == null ? "" : string).getBytes(StandardCharsets.UTF_8));
    }
}
