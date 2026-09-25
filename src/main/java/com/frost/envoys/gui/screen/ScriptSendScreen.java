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
        super(Component.literal("Отправка сценария"));
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
                Component.literal("Выберите способ сохранения:\n"
                        + "• Отправить и перезапустить — записать main.lua и перезагрузить NPC.\n"
                        + "• Только отправить — записать main.lua без перезапуска.\n"
                        + "• Только локально — сохранить файл на клиенте (без отправки)."),
                this.font).setMaxWidth(300));

        int y = this.height / 2;
        this.addRenderableWidget(Button.builder(Component.literal("Отправить и перезапустить"),
                button -> send(true)).bounds(centerX - 150, y - 30, 300, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Только отправить"),
                button -> send(false)).bounds(centerX - 150, y, 300, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Только локально"),
                button -> saveLocal()).bounds(centerX - 150, y + 30, 300, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Отмена"),
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
                Minecraft.getInstance().player.sendSystemMessage(Component.literal(
                        "§a[Envoys] Сценарий сохранён локально: envoys/local/" + this.localFileName));
            }
            if (this.onSent != null) {
                this.onSent.run();
            }
        } else if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(
                    "§c[Envoys] Не удалось сохранить файл " + this.localFileName));
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
