package com.frost.envoys.client.anim;

import com.frost.envoys.client.EmoteIntegration;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientAnimCommandsTest {

    private CommandDispatcher<CommandSourceStack> dispatcher;
    private CommandNode<CommandSourceStack> anim;

    @BeforeEach
    void setUp() {
        dispatcher = new CommandDispatcher<>();
        ClientAnimCommands.register(dispatcher);
        anim = dispatcher.getRoot().getChild("envoys").getChild("anim");
    }

    @Test
    void registersAnimSubcommands() {
        assertNotNull(anim);
        assertNotNull(anim.getChild("reload"));
        assertNotNull(anim.getChild("push"));
        assertNotNull(anim.getChild("delete"));
    }

    @Test
    void reloadIsExecutable() {
        assertNotNull(anim.getChild("reload").getCommand());
    }

    @Test
    void pushAllowsOptionalWordName() {
        CommandNode<CommandSourceStack> push = anim.getChild("push");
        assertNotNull(push.getCommand());

        ArgumentCommandNode<CommandSourceStack, ?> name = argumentNode(push.getChild("name"));
        assertInstanceOf(StringArgumentType.class, name.getType());
        assertEquals(StringArgumentType.StringType.SINGLE_WORD,
                ((StringArgumentType) name.getType()).getType());
    }

    @Test
    void deleteNameIsGreedyString() {
        ArgumentCommandNode<CommandSourceStack, ?> name = argumentNode(anim.getChild("delete").getChild("name"));

        assertInstanceOf(StringArgumentType.class, name.getType());
        assertEquals(StringArgumentType.StringType.GREEDY_PHRASE,
                ((StringArgumentType) name.getType()).getType());
        assertNotNull(name.getCommand());
    }

    @Test
    void localSuggestionsComeFromClientAnimStore() throws Exception {
        ArgumentCommandNode<CommandSourceStack, ?> name = argumentNode(anim.getChild("push").getChild("name"));

        try (MockedStatic<ClientAnimStore> store = Mockito.mockStatic(ClientAnimStore.class)) {
            store.when(ClientAnimStore::listNames).thenReturn(List.of("walk", "wave"));

            assertEquals(List.of("walk", "wave"), suggestionTexts(name));
        }
    }

    @Test
    void serverSuggestionsComeFromEmoteIntegration() throws Exception {
        ArgumentCommandNode<CommandSourceStack, ?> name = argumentNode(anim.getChild("delete").getChild("name"));

        try (MockedStatic<EmoteIntegration> integration = Mockito.mockStatic(EmoteIntegration.class)) {
            integration.when(EmoteIntegration::serverAnimNames).thenReturn(List.of("Test Emote", "§aGreen"));

            List<String> suggestions = suggestionTexts(name);

            assertTrue(suggestions.contains("Test Emote"));
            assertTrue(suggestions.contains("§aGreen"));
        }
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
