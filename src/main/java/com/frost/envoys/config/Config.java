package com.frost.envoys.config;

import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();


    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    public static final ModConfigSpec.BooleanValue LUA_ENABLED = BUILDER
            .comment("Enable the Lua scripting engine for NPCs")
            .define("lua.enabled", true);

    public static final ModConfigSpec.IntValue LUA_INSTRUCTION_LIMIT = BUILDER
            .comment("Maximum Lua instructions executed per coroutine resume before it is interrupted")
            .defineInRange("lua.instructionLimit", 50000, 1000, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue LUA_ALLOW_COMMANDS = BUILDER
            .comment("Allow NPC Lua scripts to run server commands via envoys.command (permission level 4)")
            .define("lua.allowCommands", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
