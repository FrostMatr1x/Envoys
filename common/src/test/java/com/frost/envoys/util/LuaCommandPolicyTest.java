package com.frost.envoys.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuaCommandPolicyTest {

    private CommandDispatcher<CommandSourceStack> dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new CommandDispatcher<>();
        registerRoots();
    }

    @Test
    void emptyAndLongCommandsAreDenied() {
        assertFalse(allow(null));
        assertFalse(allow(""));
        assertFalse(allow("   "));
        assertFalse(allow("say " + "a".repeat(2048)));
    }

    @Test
    void forbiddenRootsAreDenied() {
        for (String root : new String[]{"op", "deop", "ban", "ban-ip", "banlist", "pardon", "pardon-ip",
                "whitelist", "stop", "reload", "datapack", "function", "schedule", "save-all", "save-off",
                "save-on", "setidletimeout", "transfer", "publish", "debug", "jfr", "perf"}) {
            assertFalse(allow(root), root);
        }
        assertTrue(allow("ping"), "safe bare root must parse");
    }

    @Test
    void namespaceIsStripped() {
        assertFalse(allow("minecraft:op Steve"));
        assertFalse(allow("  /minecraft:op Steve"));
        assertFalse(allow("  /OP Steve"));
    }

    @Test
    void executeSideEffectsBeforeRunAreDenied() {
        assertFalse(allow("execute summon zombie run say hi"));
        assertFalse(allow("execute store result score @s x run say hi"));
    }

    @Test
    void executeAllowsSafeRootsAfterRun() {
        assertTrue(allow("execute run summon zombie"));
        assertTrue(allow("execute as @e run summon zombie"));
        assertTrue(allow("return run say hi"));
    }

    @Test
    void forbiddenRootsAfterRunAreDenied() {
        assertFalse(allow("execute as @e run op Steve"));
        assertFalse(allow("execute run op Steve"));
        assertFalse(allow("return run op Steve"));
    }

    @Test
    void safeCommandsAreAllowed() {
        assertTrue(allow("tp @s 0 100 0"));
        assertTrue(allow("give @s minecraft:diamond 1"));
        assertTrue(allow("say \"run op\""));
    }

    @Test
    void unparsedTailIsDenied() {
        assertFalse(allow("ping extra"));
    }

    private boolean allow(String command) {
        return LuaCommandPolicy.isAllowed(dispatcher, null, command);
    }

    private void registerRoots() {
        LiteralArgumentBuilder<CommandSourceStack> run = lit("run")
                .then(lit("summon").then(arg("entity", StringArgumentType.word())))
                .then(lit("say").then(arg("message", StringArgumentType.greedyString())))
                .then(lit("op").then(arg("target", StringArgumentType.word())));

        dispatcher.register(lit("execute")
                .then(lit("as").then(lit("@e").then(run)))
                .then(lit("summon").then(arg("entity", StringArgumentType.word()).then(run)))
                .then(lit("store").then(arg("rest", StringArgumentType.greedyString())))
                .then(run));

        dispatcher.register(lit("return").then(run));

        for (String root : new String[]{"op", "deop", "ban", "ban-ip", "banlist", "pardon", "pardon-ip",
                "whitelist", "stop", "reload", "datapack", "function", "schedule", "save-all", "save-off",
                "save-on", "setidletimeout", "transfer", "publish", "debug", "jfr", "perf"}) {
            dispatcher.register(lit(root));
        }
        dispatcher.register(lit("minecraft:op").then(arg("target", StringArgumentType.word())));

        dispatcher.register(lit("tp").then(arg("args", StringArgumentType.greedyString())));
        dispatcher.register(lit("give").then(arg("args", StringArgumentType.greedyString())));
        dispatcher.register(lit("say").then(arg("message", StringArgumentType.greedyString())));
        dispatcher.register(lit("ping"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> lit(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> arg(String name, StringArgumentType type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
}
