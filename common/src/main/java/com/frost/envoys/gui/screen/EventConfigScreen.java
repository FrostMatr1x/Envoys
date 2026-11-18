package com.frost.envoys.gui.screen;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.client.gui.script.ActionGraph;
import com.frost.envoys.client.gui.script.EventScript;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EventConfigScreen extends Screen {

    private final Screen parentScreen;
    private final NPCInteractManager manager;
    private final EventType type;
    private final EventScript script;
    private final boolean isCreativeTuner;
    private final Runnable onModified;

    private Checkbox enabledCheckbox;
    private EditBox argEditBox;
    private Button actionsButton;

    private boolean enabled;
    private String arg;

    public EventConfigScreen(Screen parentScreen, NPCInteractManager manager, EventType type, EventScript script,
                             boolean isCreativeTuner, Runnable onModified) {
        super(Component.translatable("envoys.gui.event_config.title"));
        this.parentScreen = parentScreen;
        this.manager = manager;
        this.type = type;
        this.script = script;
        this.isCreativeTuner = isCreativeTuner;
        this.onModified = onModified;
        ActionGraph graph = script.graphOrEmpty(type.jsonKey());
        this.enabled = graph.enabled;
        this.arg = graph.arg == null ? "" : graph.arg;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        boolean hasArg = this.type == EventType.UPDATE || this.type == EventType.RANGE;

        this.enabledCheckbox = Checkbox.builder(Component.translatable("envoys.gui.event_config.enabled"), this.font)
                .pos(centerX - 160, this.height / 2 - 50)
                .selected(this.enabled)
                .onValueChange((checkbox, selected) -> this.enabled = selected)
                .build();
        this.addRenderableWidget(this.enabledCheckbox);

        if (hasArg) {
            this.argEditBox = new EditBox(this.font, centerX + 10, this.height / 2 - 18, 80, 20,
                    Component.literal("arg"));
            this.argEditBox.setValue(this.arg);
            this.argEditBox.setFilter(text -> text.chars().allMatch(Character::isDigit));
            this.argEditBox.setResponder(text -> this.arg = text);
            this.addRenderableWidget(this.argEditBox);
        }

        this.actionsButton = Button.builder(actionLabel(), button ->
                Minecraft.getInstance().setScreen(new NPCScriptScreen(EventConfigScreen.this, this.manager,
                        this.script.graphOrEmpty(this.type.jsonKey()), this.onModified)))
                .bounds(centerX - 100, this.height / 2 + 20, 200, 20).build();
        this.addRenderableWidget(this.actionsButton);

        this.addRenderableWidget(Button.builder(Component.translatable("envoys.gui.common.back"), button -> {
            this.saveToGraph();
            if (this.onModified != null) {
                this.onModified.run();
            }
            if (this.parentScreen != null) {
                Minecraft.getInstance().setScreen(this.parentScreen);
            } else {
                this.onClose();
            }
        }).bounds(centerX - 100, this.height - 35, 200, 20).build());
    }

    private Component actionLabel() {
        ActionGraph graph = this.script.graphOrEmpty(this.type.jsonKey());
        return Component.translatable("envoys.gui.event_config.actions", graph.nodes.size());
    }

    private void saveToGraph() {
        ActionGraph graph = this.script.graphOrEmpty(this.type.jsonKey());
        graph.eventType = this.type.jsonKey();
        graph.enabled = this.enabled;
        graph.present = true;
        if (this.type == EventType.UPDATE || this.type == EventType.RANGE) {
            graph.arg = this.arg == null ? "" : this.arg.trim();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("envoys.gui.event_config.type",
                EventCreationScreen.eventDisplayName(this.type), this.type.jsonKey()),
                centerX - 160, this.height / 2 - 68, 0xA0A0A0);

        if (this.type == EventType.UPDATE) {
            guiGraphics.drawString(this.font, Component.translatable("envoys.gui.event_config.interval"), centerX - 160, this.height / 2 - 13, 0xA0A0A0);
        } else if (this.type == EventType.RANGE) {
            guiGraphics.drawString(this.font, Component.translatable("envoys.gui.event_config.radius"), centerX - 160, this.height / 2 - 13, 0xA0A0A0);
        }

        this.actionsButton.setMessage(actionLabel());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
