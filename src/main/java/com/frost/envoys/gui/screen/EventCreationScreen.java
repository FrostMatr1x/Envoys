package com.frost.envoys.gui.screen;

import java.util.ArrayList;
import java.util.List;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.NPCScriptData;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.action.event.NpcEventData;
import com.frost.envoys.action.serialization.EntityActionAdapter;
import com.frost.envoys.network.payload.SaveNPCScriptPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class EventCreationScreen extends Screen {

    private final Screen parentScreen;
    private final NPCInteractManager manager;
    private final boolean isCreativeTuner;

    private EventList eventList;

    public EventCreationScreen(Screen parentScreen, NPCInteractManager manager, boolean isCreativeTuner) {
        super(Component.literal("События NPC"));
        this.parentScreen = parentScreen;
        this.manager = manager;
        this.isCreativeTuner = isCreativeTuner;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 100;
        int centerX = this.width / 2;

        for (EventType type : EventType.values()) {
            NpcEventData event = this.manager.getEvent(type);
            if (event == null) {
                event = type.createEvent();
                event.setEnabled(false);
                this.manager.putEvent(type, event);
            }
        }

        int listHeight = this.height - 80;
        this.eventList = new EventList(this.minecraft, this.width, listHeight, 40, 26);
        this.addRenderableWidget(this.eventList);

        for (EventType type : EventType.values()) {
            NpcEventData event = this.manager.getEvent(type);
            if (event != null) {
                this.eventList.addEvent(type, event);
            }
        }

        this.addRenderableWidget(Button.builder(
            Component.literal("Назад"),
            button -> {
                this.saveAndSync();

                if (this.parentScreen != null) {
                    Minecraft.getInstance().setScreen(this.parentScreen);
                } else {
                    this.onClose();
                }
            }
        ).bounds(centerX - 100, this.height - 35, 200, 20).build());
    }

    private void saveAndSync() {
        NPCScriptData scriptData = NPCScriptData.fromManager(this.manager);
        String json = EntityActionAdapter.GSON.toJson(scriptData);

        PacketDistributor.sendToServer(
            new SaveNPCScriptPayload(this.manager.npcUUID, json)
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static String eventDisplayName(EventType type) {
        return switch (type) {
            case UPDATE -> "Цикл";
            case CLICK -> "Клик";
            case KICK -> "Удар";
            case RANGE -> "Радиус";
        };
    }

    class EventList extends ContainerObjectSelectionList<EventEntry> {
        public EventList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public void addEvent(EventType type, NpcEventData event) {
            this.addEntry(new EventEntry(type, event));
        }

        @Override
        public int getRowWidth() {
            return 320;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width / 2 + 170;
        }
    }

    class EventEntry extends ContainerObjectSelectionList.Entry<EventEntry> {
        private final EventType type;
        private final NpcEventData event;
        private final Button configureButton;
        private final List<GuiEventListener> children = new ArrayList<>();

        public EventEntry(EventType type, NpcEventData event) {
            this.type = type;
            this.event = event;

            this.configureButton = Button.builder(Component.literal("Настроить"), button -> {
                Minecraft.getInstance().setScreen(new EventConfigScreen(EventCreationScreen.this, EventCreationScreen.this.manager, this.event, EventCreationScreen.this.isCreativeTuner));
            }).bounds(0, 0, 75, 20).build();

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
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            String state = this.event.enabled() ? "включено" : "выключено";
            String label = eventDisplayName(this.type) + " (" + this.type.jsonKey() + ") — " + state
                    + " · действий: " + (this.event.actions() != null ? this.event.actions().size() : 0);
            guiGraphics.drawString(Minecraft.getInstance().font, label, left + 5, top + (height - 8) / 2, 0xFFFFFF, false);

            this.configureButton.setX(left + width - 80);
            this.configureButton.setY(top);

            this.configureButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
