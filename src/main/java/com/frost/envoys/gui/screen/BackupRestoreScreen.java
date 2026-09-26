package com.frost.envoys.gui.screen;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.client.gui.backup.ClientBackupManager;
import com.frost.envoys.client.gui.script.ScriptProject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BackupRestoreScreen extends Screen {

    private final Screen parentScreen;
    private final NPCInteractManager manager;
    private final boolean isCreativeTuner;

    public BackupRestoreScreen(Screen parentScreen, NPCInteractManager manager, boolean isCreativeTuner) {
        super(Component.translatable("envoys.gui.backup.title"));
        this.parentScreen = parentScreen;
        this.manager = manager;
        this.isCreativeTuner = isCreativeTuner;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int y = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("envoys.gui.backup.restore"), button -> {
            ScriptProject project = ClientBackupManager.load(this.manager.npcUUID);
            if (project != null) {
                ClientBackupManager.stashPendingRestore(this.manager.npcUUID, project);
            }
            ClientBackupManager.delete(this.manager.npcUUID);
            openEditor();
        }).bounds(centerX - 100, y - 10, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("envoys.gui.backup.discard"), button -> {
            ClientBackupManager.delete(this.manager.npcUUID);
            openEditor();
        }).bounds(centerX - 100, y + 20, 200, 20).build());
    }

    private void openEditor() {
        Minecraft.getInstance().setScreen(new EventCreationScreen(this.parentScreen, this.manager, this.isCreativeTuner));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 20, 0xFFFFFF);
        guiGraphics.drawWordWrap(this.font, Component.translatable("envoys.gui.backup.message"),
                centerX - 160, 50, 320, 0xFFFF55);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
