package com.frost.envoys.gui.screen;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.NPCScriptData;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.action.event.NpcEventData;
import com.frost.envoys.action.event.NpcRangeEvent;
import com.frost.envoys.action.serialization.EntityActionAdapter;
import com.frost.envoys.network.payload.SaveNPCScriptPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class EventConfigScreen extends Screen {

    private final Screen parentScreen;
    private final NPCInteractManager manager;
    private final NpcEventData event;
    private final boolean isCreativeTuner;

    private Checkbox enabledCheckbox;
    private EditBox rangeDistanceEditBox;
    private Button actionsButton;

    private boolean enabled;
    private float rangeDistance = 0.0f;

    public EventConfigScreen(Screen parentScreen, NPCInteractManager manager, NpcEventData event, boolean isCreativeTuner) {
        super(Component.literal("Настройка события"));
        this.parentScreen = parentScreen;
        this.manager = manager;
        this.event = event;
        this.isCreativeTuner = isCreativeTuner;
        this.enabled = event.enabled();
        if (event instanceof NpcRangeEvent rangeEvent) {
            this.rangeDistance = rangeEvent.rangeDistance;
        }
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        this.enabledCheckbox = Checkbox.builder(Component.literal("Событие включено"), this.font)
            .pos(centerX - 160, this.height / 2 - 50)
            .selected(this.enabled)
            .onValueChange((checkbox, selected) -> this.enabled = selected)
            .build();
        this.addRenderableWidget(this.enabledCheckbox);

        if (this.event instanceof NpcRangeEvent) {
            this.rangeDistanceEditBox = new EditBox(this.font, centerX - 160, this.height / 2 - 15, 80, 20, Component.literal("rangeDistance"));
            this.rangeDistanceEditBox.setValue(Float.toString(this.rangeDistance));
            this.rangeDistanceEditBox.setResponder(text -> this.rangeDistance = parseOrDefaultFloat(text, this.rangeDistance));
            this.addRenderableWidget(this.rangeDistanceEditBox);
        }

        this.actionsButton = Button.builder(
            Component.literal("Сценарий действий (" + actionCount() + ")"),
            button -> Minecraft.getInstance().setScreen(new NPCScriptScreen(EventConfigScreen.this, this.manager, this.event, this.isCreativeTuner))
        ).bounds(centerX - 100, this.height / 2 + 20, 200, 20).build();
        this.addRenderableWidget(this.actionsButton);

        this.addRenderableWidget(Button.builder(
            Component.literal("Назад"),
            button -> {
                this.saveToEvent();
                this.saveAndSync();
                if (this.parentScreen != null) {
                    Minecraft.getInstance().setScreen(this.parentScreen);
                } else {
                    this.onClose();
                }
            }
        ).bounds(centerX - 100, this.height - 35, 200, 20).build());
    }

    private int actionCount() {
        return this.event.actions() != null ? this.event.actions().size() : 0;
    }

    private void saveToEvent() {
        this.event.setEnabled(this.enabled);
        if (this.event instanceof NpcRangeEvent rangeEvent) {
            rangeEvent.rangeDistance = Math.max(0.0f, this.rangeDistance);
        }
    }

    private void saveAndSync() {
        NPCScriptData scriptData = NPCScriptData.fromManager(this.manager);
        String json = EntityActionAdapter.GSON.toJson(scriptData);

        PacketDistributor.sendToServer(
            new SaveNPCScriptPayload(this.manager.npcUUID, json)
        );
    }

    private float parseOrDefaultFloat(String text, float defaultValue) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);

        EventType type = this.event.type();
        String typeLabel = type != null ? EventCreationScreen.eventDisplayName(type) + " (" + type.jsonKey() + ")" : "?";
        guiGraphics.drawString(this.font, "Тип события: " + typeLabel, centerX - 160, this.height / 2 - 68, 0xA0A0A0);

        if (this.event instanceof NpcRangeEvent) {
            guiGraphics.drawString(this.font, "Дистанция срабатывания:", centerX - 160, this.height / 2 - 13, 0xA0A0A0);
        }

        this.actionsButton.setMessage(Component.literal("Сценарий действий (" + actionCount() + ")"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
