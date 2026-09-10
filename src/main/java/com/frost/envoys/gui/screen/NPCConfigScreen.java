package com.frost.envoys.gui.screen;

import com.frost.envoys.action.NPCInteractManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NPCConfigScreen extends Screen {

    private final NPCInteractManager npcManager;
    private final boolean isCreativeTuner;

    protected final int imageWidth = 176;
    protected final int imageHeight = 166;
    
    protected int leftPos;
    protected int topPos;

    public NPCConfigScreen(NPCInteractManager npcManager, boolean isCreativeTuner) {
        super(Component.literal("Настройка NPC"));
        this.npcManager = npcManager;
        this.isCreativeTuner = isCreativeTuner;
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        int buttonWidth = 160;
        int buttonHeight = 20;

        int centerX = this.leftPos + (this.imageWidth - buttonWidth) / 2;

        int startY = this.topPos + 40;

        this.addRenderableWidget(Button.builder(
            Component.literal("Редактировать личность"), 
            button -> Minecraft.getInstance().setScreen(new NPCPassportScreen(this, this.npcManager, this.isCreativeTuner))
        ).bounds(centerX, startY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Редактировать скин"), 
            button -> Minecraft.getInstance().setScreen(new NPCSkinScreen(this, this.npcManager.npcUUID))
        ).bounds(centerX, startY + 30, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Редактировать сценарий"), 
            button -> Minecraft.getInstance().setScreen(new EventCreationScreen(this, this.npcManager, this.isCreativeTuner))
        ).bounds(centerX, startY + 60, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Закрыть"), 
            button -> this.onClose()
        ).bounds(centerX, startY + 110, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}