package com.frost.envoys;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.config.NPCConfigManager;
import com.frost.envoys.lua.LuaScriptStore;
import com.frost.envoys.network.NetworkTestSupport;
import com.frost.envoys.npc.entity.BaseNPC;
import com.frost.envoys.platform.EnvoysPlatform;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnvoysCommandTest {

    @TempDir
    Path tempDir;

    private CommandDispatcher<CommandSourceStack> dispatcher;
    private MinecraftServer server;
    private ServerLevel level;

    @BeforeEach
    void setUp() {
        NetworkTestSupport.bootStrap();
        TestSupport.installEnvironment(tempDir);
        TestSupport.resetNpcConfigMigration();
        TestSupport.resetQuestState();
        TestSupport.resetLuaEngines();
        EnvoysPlatform.config().luaEnabled = true;

        level = NetworkTestSupport.level();
        when(level.getAllEntities()).thenReturn(List.of());
        server = NetworkTestSupport.serverWithLevels(level);

        dispatcher = new CommandDispatcher<>();
        EnvoysCommand.register(dispatcher);
    }

    @AfterEach
    void tearDown() {
        TestSupport.resetLuaEngines();
        EnvoysPlatform.config().luaEnabled = true;
        TestSupport.clearEnvironment();
    }

    @Test
    void rootRequiresOpFourAndRejectsNormalPlayer() {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild("envoys");
        assertNotNull(root);

        CommandSourceStack op = mockSource(true, server);
        assertTrue(root.canUse(op));
        verify(op).hasPermission(4);

        CommandSourceStack normal = mockSource(false, server);
        assertFalse(root.canUse(normal));
        verify(normal).hasPermission(4);
    }

    @Test
    void nonOpCannotExecuteServerCommands() {
        CommandSourceStack normal = mockSource(false, server);

        assertThrows(CommandSyntaxException.class,
                () -> dispatcher.execute("envoys lua reload", normal));
        assertThrows(CommandSyntaxException.class,
                () -> dispatcher.execute("envoys npc resetpoints " + TestSupport.uuid(7) + " start", normal));
    }

    @Test
    void luaReloadSendsSuccessFeedback() throws CommandSyntaxException {
        UUID id = TestSupport.uuid(1);
        assertTrue(LuaScriptStore.writeScript(id, "return 1"));

        CommandSourceStack op = mockSource(true, server);
        int result = dispatcher.execute("envoys lua reload", op);

        assertEquals(1, result);
        assertEquals("envoys.cmd.lua.reload_done", successKey(op));
    }

    @Test
    void luaStopRequiresTargetArgument() {
        CommandSourceStack op = mockSource(true, server);

        assertThrows(CommandSyntaxException.class,
                () -> dispatcher.execute("envoys lua stop", op));
    }

    @Test
    void luaStopUnknownTargetSendsFailure() throws CommandSyntaxException {
        CommandSourceStack op = mockSource(true, NetworkTestSupport.serverWithLevels());

        int result = dispatcher.execute("envoys lua stop " + UUID.randomUUID(), op);

        assertEquals(0, result);
        assertEquals("envoys.cmd.npc.not_found", failureKey(op));
    }

    @Test
    void resetPointsRequiresBranchArgument() {
        CommandSourceStack op = mockSource(true, server);

        assertThrows(CommandSyntaxException.class,
                () -> dispatcher.execute("envoys npc resetpoints " + TestSupport.uuid(8), op));
    }

    @Test
    void resetPointsUnknownNpcSendsFailure() throws CommandSyntaxException {
        CommandSourceStack op = mockSource(true, server);

        int result = dispatcher.execute("envoys npc resetpoints " + TestSupport.uuid(9) + " start", op);

        assertEquals(0, result);
        assertEquals("envoys.cmd.npc.not_found", failureKey(op));
    }

    @Test
    void regenRequiresNpcArgument() {
        CommandSourceStack op = mockSource(true, server);

        assertThrows(CommandSyntaxException.class,
                () -> dispatcher.execute("envoys regen", op));
    }

    @Test
    void suggestNpcTargetsContainsAimAndLiveNpc() {
        UUID id = TestSupport.uuid(3);
        BaseNPC npc = mock(BaseNPC.class);
        when(npc.getUUID()).thenReturn(id);
        when(level.getAllEntities()).thenReturn(List.of(npc));

        CommandSourceStack op = mockSource(true, server);

        List<String> all = completionTexts("envoys lua stop ", op);
        assertTrue(all.contains("aim"));
        assertTrue(all.contains(id.toString()));

        List<String> filtered = completionTexts("envoys lua stop 00", op);
        assertFalse(filtered.contains("aim"));
        assertTrue(filtered.contains(id.toString()));
    }

    @Test
    void suggestSavedNpcUuidsContainsPersistedNpc() {
        UUID id = TestSupport.uuid(4);
        NPCInteractManager manager = new NPCInteractManager(id);
        NPCConfigManager.save(manager);

        CommandSourceStack op = mockSource(true, server);

        assertTrue(completionTexts("envoys regen ", op).contains(id.toString()));
    }

    private static CommandSourceStack mockSource(boolean op, MinecraftServer server) {
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.hasPermission(4)).thenReturn(op);
        when(source.getServer()).thenReturn(server);
        return source;
    }

    private List<String> completionTexts(String input, CommandSourceStack source) {
        try {
            Suggestions suggestions = dispatcher
                    .getCompletionSuggestions(dispatcher.parse(input, source))
                    .get();
            return suggestions.getList().stream().map(Suggestion::getText).toList();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static String successKey(CommandSourceStack source) {
        ArgumentCaptor<Supplier<Component>> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(source).sendSuccess(captor.capture(), eq(true));
        return translationKey(captor.getValue().get());
    }

    private static String failureKey(CommandSourceStack source) {
        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(source).sendFailure(captor.capture());
        return translationKey(captor.getValue());
    }

    private static String translationKey(Component component) {
        assertNotNull(component);
        if (component.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return component.getString();
    }
}
