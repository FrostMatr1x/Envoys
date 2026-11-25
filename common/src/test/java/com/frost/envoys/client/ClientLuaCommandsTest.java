package com.frost.envoys.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClientLuaCommandsTest {

    private CommandDispatcher<CommandSourceStack> dispatcher;
    private CommandNode<CommandSourceStack> lua;

    @BeforeEach
    void setUp() {
        dispatcher = new CommandDispatcher<>();
        ClientLuaCommands.register(dispatcher);
        lua = dispatcher.getRoot().getChild("envoys").getChild("lua");
    }

    @Test
    void registersLuaSubcommands() {
        assertNotNull(lua);
        assertNotNull(lua.getChild("list"));
        assertNotNull(lua.getChild("send"));
        assertNotNull(lua.getChild("pull"));
        assertNotNull(lua.getChild("serverArgs"));
    }

    @Test
    void sendHasRequiredTargetAndFileArguments() {
        CommandNode<CommandSourceStack> target = lua.getChild("send").getChild("target");
        assertWord(target);

        CommandNode<CommandSourceStack> file = target.getChild("file");
        assertWord(file);
        assertNotNull(file.getCommand());
    }

    @Test
    void pullHasRequiredTargetWithSuggestions() {
        ArgumentCommandNode<CommandSourceStack, ?> target = argumentNode(lua.getChild("pull").getChild("target"));

        assertWord(target);
        assertNotNull(target.getCustomSuggestions());
    }

    @Test
    void listIsExecutable() {
        assertNotNull(lua.getChild("list").getCommand());
    }

    @Test
    void serverArgsFallbackIsGreedyString() {
        ArgumentCommandNode<CommandSourceStack, ?> serverArgs = argumentNode(lua.getChild("serverArgs"));

        assertInstanceOf(StringArgumentType.class, serverArgs.getType());
        assertEquals(StringArgumentType.StringType.GREEDY_PHRASE,
                ((StringArgumentType) serverArgs.getType()).getType());
        assertNotNull(serverArgs.getCommand());
    }

    @Test
    void fileSuggestionsComeFromLocalStore() throws Exception {
        ArgumentCommandNode<CommandSourceStack, ?> file = argumentNode(
                lua.getChild("send").getChild("target").getChild("file"));

        try (MockedStatic<ClientLocalScriptStore> store = Mockito.mockStatic(ClientLocalScriptStore.class)) {
            store.when(ClientLocalScriptStore::listScripts).thenReturn(List.of("a.lua", "b.lua"));

            assertEquals(List.of("a.lua", "b.lua"), suggestionTexts(file));
        }
    }

    @Test
    void targetArgumentHasSuggestions() {
        ArgumentCommandNode<CommandSourceStack, ?> target = argumentNode(
                lua.getChild("send").getChild("target"));

        assertNotNull(target.getCustomSuggestions());
    }

    private static void assertWord(CommandNode<CommandSourceStack> node) {
        ArgumentCommandNode<CommandSourceStack, ?> argument = argumentNode(node);
        assertInstanceOf(StringArgumentType.class, argument.getType());
        assertEquals(StringArgumentType.StringType.SINGLE_WORD,
                ((StringArgumentType) argument.getType()).getType());
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCommandNode<CommandSourceStack, ?> argumentNode(CommandNode<CommandSourceStack> node) {
        assertNotNull(node);
        assertInstanceOf(ArgumentCommandNode.class, node);
        return (ArgumentCommandNode<CommandSourceStack, ?>) node;
    }

    private static List<String> suggestionTexts(ArgumentCommandNode<CommandSourceStack, ?> argument) throws Exception {
        SuggestionProvider<CommandSourceStack> provider = argument.getCustomSuggestions();
        assertNotNull(provider);
        return provider.getSuggestions(null, new SuggestionsBuilder("", 0))
                .get()
                .getList()
                .stream()
                .map(Suggestion::getText)
                .toList();
    }
}
