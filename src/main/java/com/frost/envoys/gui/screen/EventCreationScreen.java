package com.frost.envoys.gui.screen;

import java.util.ArrayList;
import java.util.List;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.client.ClientLuaScriptBridge;
import com.frost.envoys.client.ScriptNames;
import com.frost.envoys.client.gui.backup.ClientBackupManager;
import com.frost.envoys.client.gui.script.ActionGraph;
import com.frost.envoys.client.gui.script.EventScript;
import com.frost.envoys.client.gui.script.EventUiState;
import com.frost.envoys.client.gui.script.ScenarioCompileException;
import com.frost.envoys.client.gui.script.ScenarioCompiler;
import com.frost.envoys.client.gui.script.ScenarioDecompiler;
import com.frost.envoys.client.gui.script.ScriptProject;
import com.frost.envoys.lua.LuaSandbox;
import com.frost.envoys.network.payload.NpcLuaScriptResponsePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.compiler.CompileException;

public class EventCreationScreen extends Screen {

    private static final int LOAD_TIMEOUT_TICKS = 200;

    private final Screen parentScreen;
    private final NPCInteractManager manager;
    private final boolean isCreativeTuner;

    private ScriptProject project;
    private String serverSource = "";
    private String baselineSource;
    private String lastCompiledSource = "";
    private boolean fetchRequested;
    private boolean loading;
    private Component errorText;
    private int loadingTicks;

    private EventList eventList;

    public EventCreationScreen(Screen parentScreen, NPCInteractManager manager, boolean isCreativeTuner) {
        super(Component.translatable("envoys.gui.event_creation.title"));
        this.parentScreen = parentScreen;
        this.manager = manager;
        this.isCreativeTuner = isCreativeTuner;
    }

    @Override
    protected void init() {
        super.init();

        if (project == null) {
            ScriptProject restored = ClientBackupManager.consumePendingRestore(manager.npcUUID);
            if (restored != null) {
                this.project = restored;
                this.project.dirty = true;
                for (EventScript script : project.events.values()) {
                    if (script != null && !script.locked() && script.graph != null && script.graph.present) {
                        script.state = EventUiState.YELLOW_MODIFIED;
                    }
                }
            } else if (!fetchRequested) {
                this.fetchRequested = true;
                this.loading = true;
                ClientLuaScriptBridge.requestForScreen(manager.npcUUID, this::onScriptResponse);
            }
        }

        if (project != null) {
            ClientBackupManager.markActive(manager.npcUUID, project);
        }

        int centerX = this.width / 2;

        if (loading) {
            this.addRenderableWidget(Button.builder(Component.translatable("envoys.gui.common.cancel"), button -> this.onClose())
                    .bounds(centerX - 100, this.height - 35, 200, 20).build());
            return;
        }

        if (project != null) {
            this.eventList = new EventList(this.minecraft, this.width, this.height - 120, 40, 26);
            this.addRenderableWidget(this.eventList);
            for (EventType type : EventType.values()) {
                this.eventList.addEvent(type, project.event(type.jsonKey()));
            }
        }

        int navY = this.height - 60;
        int rowStart = centerX - 150;
        this.addRenderableWidget(Button.builder(Component.translatable("envoys.gui.event_creation.quests"), button ->
                Minecraft.getInstance().setScreen(new QuestManagementScreen(this, this.manager, this.isCreativeTuner)))
                .bounds(rowStart, navY, 90, 20).build());

        Button process = Button.builder(Component.translatable("envoys.gui.event_creation.process"), button -> processScenario())
                .bounds(rowStart + 95, navY, 110, 20).build();
        process.setTooltip(Tooltip.create(Component.translatable("envoys.gui.event_creation.process_tooltip")));
        process.active = project != null && hasModified();
        this.addRenderableWidget(process);

        this.addRenderableWidget(Button.builder(Component.translatable("envoys.gui.common.back"), button -> this.onClose())
                .bounds(rowStart + 210, navY, 90, 20).build());

    }

    private void onScriptResponse(NpcLuaScriptResponsePayload payload) {
        if (payload.exists()) {
            this.serverSource = payload.source() == null ? "" : payload.source();
            this.project = ScenarioDecompiler.decompile(this.serverSource, ScriptProject.EVENT_ORDER);
        } else {
            this.serverSource = "";
            this.project = emptyProject();
        }
        this.loading = false;
        this.errorText = null;
        if (!payload.exists() && payload.message() != null && !payload.message().getString().isBlank()
                && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(payload.message());
        }
        this.baselineSource = this.serverSource;
        this.project.dirty = false;
        for (EventScript script : this.project.events.values()) {
            if (script != null && !script.locked()) {
                script.state = EventUiState.GREEN_SYNCED;
            }
        }
        if (Minecraft.getInstance().screen == this) {
            this.rebuildWidgets();
        }
    }

    private ScriptProject emptyProject() {
        ScriptProject empty = new ScriptProject();
        for (EventType type : EventType.values()) {
            EventScript script = new EventScript(new ActionGraph(type.jsonKey()));
            script.state = EventUiState.GREEN_SYNCED;
            empty.events.put(type.jsonKey(), script);
        }
        return empty;
    }

    private boolean hasModified() {
        return project != null && project.dirty;
    }

    private void markModified(String eventKey) {
        EventScript script = project.event(eventKey);
        if (script == null || script.locked()) {
            return;
        }
        script.state = EventUiState.YELLOW_MODIFIED;
        refreshDirty();
        if (project.dirty) {
            ClientBackupManager.save(manager.npcUUID, project);
        } else {
            revertYellow();
        }
    }

    private void refreshDirty() {
        if (baselineSource == null) {
            project.dirty = true;
            return;
        }
        try {
            String current = ScenarioCompiler.compile(project, ScriptProject.EVENT_ORDER, false);
            project.dirty = !current.equals(baselineSource);
        } catch (Exception e) {
            project.dirty = true;
        }
    }

    private void revertYellow() {
        for (EventScript script : project.events.values()) {
            if (script != null && script.state == EventUiState.YELLOW_MODIFIED) {
                script.state = EventUiState.GREEN_SYNCED;
            }
        }
    }

    private void processScenario() {
        try {
            String source = ScenarioCompiler.compile(project, ScriptProject.EVENT_ORDER);
            this.lastCompiledSource = source;
            LuaSandbox.compileOnly(source);

            String npcName = manager.passport != null && manager.passport.npcName != null
                    && !manager.passport.npcName.isBlank() ? manager.passport.npcName : "npc";
            String localFileName = ScriptNames.fileName(npcName, manager.npcUUID);
            Minecraft.getInstance().setScreen(new ScriptSendScreen(this, manager.npcUUID, source, localFileName, this::onSent));
        } catch (ScenarioCompileException e) {
            showError(e.component());
        } catch (CompileException e) {
            showError(Component.translatable("envoys.gui.event_creation.compile_error", String.valueOf(e.getMessage())));
        } catch (LuaError e) {
            showError(Component.translatable("envoys.gui.event_creation.lua_error", String.valueOf(e.getMessage())));
        } catch (RuntimeException e) {
            showError(Component.translatable("envoys.gui.event_creation.internal_error", String.valueOf(e)));
        }
    }

    private void onSent() {
        for (EventScript script : project.events.values()) {
            if (script != null && script.state == EventUiState.YELLOW_MODIFIED) {
                script.state = EventUiState.GREEN_SYNCED;
            }
        }
        project.dirty = false;
        this.baselineSource = this.lastCompiledSource;
        ClientBackupManager.delete(manager.npcUUID);
        if (Minecraft.getInstance().screen == this) {
            this.rebuildWidgets();
        }
    }

    private void showError(Component message) {
        this.errorText = message;
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.translatable("envoys.gui.error", message));
        }
        this.rebuildWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        if (loading && ++loadingTicks > LOAD_TIMEOUT_TICKS) {
            this.loading = false;
            this.errorText = Component.translatable("envoys.gui.event_creation.fetch_timeout");
            this.rebuildWidgets();
        }
    }

    @Override
    public void onClose() {
        if (project != null && ClientBackupManager.hasDirty(project)) {
            ClientBackupManager.save(manager.npcUUID, project);
        }
        ClientBackupManager.clearActive(manager.npcUUID);
        if (parentScreen != null) {
            Minecraft.getInstance().setScreen(parentScreen);
        } else {
            super.onClose();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        if (loading) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("envoys.gui.event_creation.loading"), this.width / 2, this.height / 2 - 10, 0xFFFF55);
        } else if (errorText != null) {
            guiGraphics.drawCenteredString(this.font, errorText, this.width / 2, this.height / 2 - 10, 0xFF5555);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static Component eventDisplayName(EventType type) {
        return Component.translatable("envoys.event." + type.jsonKey());
    }

    class EventList extends ContainerObjectSelectionList<EventEntry> {
        public EventList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public void addEvent(EventType type, EventScript script) {
            this.addEntry(new EventEntry(type, script));
        }

        @Override
        public int getRowWidth() {
            return 340;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width / 2 + 180;
        }
    }

    class EventEntry extends ContainerObjectSelectionList.Entry<EventEntry> {
        private final EventType type;
        private final EventScript script;
        private final Button configureButton;
        private final List<GuiEventListener> children = new ArrayList<>();

        public EventEntry(EventType type, EventScript script) {
            this.type = type;
            this.script = script;
            boolean locked = script == null || script.locked();

            this.configureButton = Button.builder(Component.translatable("envoys.gui.common.configure"), button ->
                    Minecraft.getInstance().setScreen(new EventConfigScreen(EventCreationScreen.this,
                            EventCreationScreen.this.manager, this.type, this.script,
                            EventCreationScreen.this.isCreativeTuner,
                            () -> EventCreationScreen.this.markModified(this.type.jsonKey()))))
                    .bounds(0, 0, 75, 20).build();
            this.configureButton.active = !locked;
            if (locked) {
                this.configureButton.setTooltip(Tooltip.create(
                        Component.translatable("envoys.gui.event_creation.locked_tooltip")));
            }
            this.children.add(this.configureButton);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.configureButton);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            EventUiState state = script == null ? EventUiState.GREY_LOCKED_CUSTOM : script.state;
            int color = switch (state) {
                case GREEN_SYNCED -> 0x55FF55;
                case YELLOW_MODIFIED -> 0xFFFF55;
                case GREY_LOCKED_CUSTOM -> 0xAAAAAA;
            };
            Component status = switch (state) {
                case GREEN_SYNCED -> Component.empty();
                case YELLOW_MODIFIED -> Component.translatable("envoys.gui.event_creation.status_modified");
                case GREY_LOCKED_CUSTOM -> Component.translatable("envoys.gui.event_creation.status_custom");
            };
            int nodeCount = script != null && script.graph != null && !script.locked() ? script.graph.nodes.size() : 0;
            Component label = Component.translatable("envoys.gui.event_creation.entry",
                    eventDisplayName(this.type), this.type.jsonKey(), status);

            guiGraphics.drawString(Minecraft.getInstance().font, "●", left + 5, top + (height - 8) / 2, color, false);
            guiGraphics.drawString(Minecraft.getInstance().font, label, left + 20, top + (height - 8) / 2, 0xFFFFFF, false);
            if (nodeCount > 0) {
                guiGraphics.drawString(Minecraft.getInstance().font,
                        Component.translatable("envoys.gui.event_creation.nodes", nodeCount),
                        left + 210, top + (height - 8) / 2, 0xFFFF55, false);
            }

            this.configureButton.setX(left + width - 80);
            this.configureButton.setY(top);
            this.configureButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
