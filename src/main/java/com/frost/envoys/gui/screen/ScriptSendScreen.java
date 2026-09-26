package com.frost.envoys.gui.screen;

import com.frost.envoys.client.ClientLocalScriptStore;
import com.frost.envoys.network.payload.SaveNpcLuaScriptPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class ScriptSendScreen extends Screen {

    private final Screen parentScreen;
    private final UUID npcUuid;
    private final String source;
    private final String localFileName;
    private final Runnable onSent;

    public ScriptSendScreen(Screen parentScreen, UUID npcUuid, String source, String localFileName, Runnable onSent) {
        super(Component.translatable("envoys.gui.script_send.title"));
        this.parentScreen = parentScreen;
        this.npcUuid = npcUuid;
        this.source = source;
        this.localFileName = localFileName;
        this.onSent = onSent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        this.addRenderableWidget(new MultiLineTextWidget(
                centerX - 150, 45,
                Component.translatable("envoys.gui.script_send.info"),
                this.font).setMaxWidth(300));

        int y = this.height / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("envoys.gui.script_send.send_restart"),
                button -> send(true)).bounds(centerX - 150, y - 30, 300, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("envoys.gui.script_send.send_only"),
                button -> send(false)).bounds(centerX - 150, y, 300, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("envoys.gui.script_send.local_only"),
                button -> saveLocal()).bounds(centerX - 150, y + 30, 300, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("envoys.gui.common.cancel"),
                button -> back()).bounds(centerX - 150, y + 70, 300, 20).build());
    }

    private void send(boolean restart) {
        PacketDistributor.sendToServer(new SaveNpcLuaScriptPayload(this.npcUuid, "main.lua", this.source, restart));
        if (this.onSent != null) {
            this.onSent.run();
        }
        back();
    }

    private void saveLocal() {
        boolean ok = ClientLocalScriptStore.writeScript(this.localFileName, this.source);
        if (ok) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(
                        Component.translatable("envoys.cmd.lua.saved_local", this.localFileName));
            }
            if (this.onSent != null) {
                this.onSent.run();
            }
        } else if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(
                    Component.translatable("envoys.cmd.lua.write_failed", this.localFileName));
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
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
