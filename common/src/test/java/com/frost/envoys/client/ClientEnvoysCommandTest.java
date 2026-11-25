package com.frost.envoys.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ClientEnvoysCommandTest {

    private CommandDispatcher<CommandSourceStack> dispatcher;
    private CommandNode<CommandSourceStack> envoys;

    @BeforeEach
    void setUp() {
        dispatcher = new CommandDispatcher<>();
        dispatcher.register(ClientEnvoysCommand.root());
        envoys = dispatcher.getRoot().getChild("envoys");
    }

    @Test
    void bareEnvoysIsExecutable() {
        assertNotNull(envoys);
        assertNotNull(envoys.getCommand());
    }

    @Test
    void greedyServerArgsFallbackIsWired() {
        CommandNode<CommandSourceStack> serverArgs = envoys.getChild("serverArgs");
        assertNotNull(serverArgs);
        assertInstanceOf(ArgumentCommandNode.class, serverArgs);

        ArgumentCommandNode<CommandSourceStack, ?> argument =
                (ArgumentCommandNode<CommandSourceStack, ?>) serverArgs;
        assertInstanceOf(StringArgumentType.class, argument.getType());
        assertEquals(StringArgumentType.StringType.GREEDY_PHRASE,
                ((StringArgumentType) argument.getType()).getType());
        assertNotNull(argument.getCommand());
    }

    @Test
    void rootHasNoPermissionRequirement() {
        assertTrue(ClientEnvoysCommand.root().build().getRequirement().test(mock(CommandSourceStack.class)));
    }
}
