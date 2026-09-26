package com.frost.envoys.gui.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NodeTypeSelectScreen extends Screen {

    private final Screen parentScreen;
    private final NPCScriptScreen.NodeType current;
    private final Consumer<NPCScriptScreen.NodeType> onSelect;

    private TypeList typeList;

    public NodeTypeSelectScreen(Screen parentScreen, NPCScriptScreen.NodeType current,
                                Consumer<NPCScriptScreen.NodeType> onSelect) {
        super(Component.translatable("envoys.gui.node_type.title"));
        this.parentScreen = parentScreen;
        this.current = current;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        super.init();

        this.typeList = new TypeList(this.minecraft, this.width, this.height - 85, 40, 24);
        this.addRenderableWidget(this.typeList);
        for (NPCScriptScreen.NodeType type : NPCScriptScreen.NodeType.values()) {
            this.typeList.addType(type);
        }

        this.addRenderableWidget(Button.builder(Component.translatable("envoys.gui.common.back"), button -> back())
                .bounds(this.width / 2 - 100, this.height - 35, 200, 20).build());
    }

    private void choose(NPCScriptScreen.NodeType type) {
        if (this.onSelect != null) {
            this.onSelect.accept(type);
        }
        back();
    }

    private void back() {
        if (this.parentScreen != null) {
            Minecraft.getInstance().setScreen(this.parentScreen);
        } else {
            this.onClose();
        }
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

    class TypeList extends ContainerObjectSelectionList<TypeEntry> {
        public TypeList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public void addType(NPCScriptScreen.NodeType type) {
            this.addEntry(new TypeEntry(type));
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

    class TypeEntry extends ContainerObjectSelectionList.Entry<TypeEntry> {
        private final Button button;
        private final List<GuiEventListener> children = new ArrayList<>();

        TypeEntry(NPCScriptScreen.NodeType type) {
            boolean activeType = type == current;
            Component label = (activeType ? Component.literal("\u25B6 ") : Component.literal("  "))
                    .copy().append(type.display());
            this.button = Button.builder(label, b -> choose(type))
                    .bounds(0, 0, 300, 20).build();
            this.children.add(this.button);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.button);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            this.button.setX(left + (width - 300) / 2);
            this.button.setY(top);
            this.button.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
