package com.frost.envoys.gui.screen;

import com.frost.envoys.client.EmoteIntegration;
import com.frost.envoys.client.anim.ClientAnimStore;
import com.frost.envoys.npc.entity.MannequinEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NPCEmoteScreen extends Screen {

    private static final int ROW_HEIGHT = 14;

    private final Screen parentScreen;
    private final MannequinEntity mannequin;
    private final Consumer<String> onSelect;

    private final List<String> emoteNames = new ArrayList<>();
    private String selectedEmote;
    private int scrollOffset = 0;
    private int lastServerListVersion;
    private long lastLocalAnimVersion;

    private Button selectButton;
    private float mannequinYaw = 0.0F;
    private boolean isDraggingMannequin = false;

    public NPCEmoteScreen(Screen parentScreen, MannequinEntity mannequin, Consumer<String> onSelect) {
        super(Component.translatable("envoys.gui.emote.title"));
        this.parentScreen = parentScreen;
        this.mannequin = mannequin;
        this.onSelect = onSelect;
        this.lastServerListVersion = EmoteIntegration.serverListVersion();
        this.lastLocalAnimVersion = ClientAnimStore.version();
        this.emoteNames.addAll(EmoteIntegration.clientEmoteNames());
        EmoteIntegration.requestServerList();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int bottomY = this.height - 30;

        this.selectButton = Button.builder(Component.translatable("envoys.gui.emote.select"), button -> {
            if (this.selectedEmote != null) {
                this.onSelect.accept(this.selectedEmote);
            }
            this.onClose();
        }).bounds(centerX - 105, bottomY, 100, 20).build();
        this.selectButton.active = false;
        this.addRenderableWidget(this.selectButton);

        this.addRenderableWidget(Button.builder(Component.translatable("envoys.gui.common.cancel"), button -> this.onClose())
                .bounds(centerX + 5, bottomY, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 10, 0xFFFFFFFF);

        int previewX1 = centerX - 180;
        int previewY1 = startY - 10;
        int previewX2 = centerX - 30;
        int previewY2 = startY + 150;

        guiGraphics.fill(previewX1, previewY1, previewX2, previewY2, 0xFF000000);
        guiGraphics.renderOutline(previewX1, previewY1, previewX2 - previewX1, previewY2 - previewY1, 0xFF555555);

        if (this.mannequin != null) {
            this.mannequin.setYRot(this.mannequinYaw);
            this.mannequin.yRotO = this.mannequinYaw;

            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics,
                    previewX1, previewY1,
                    previewX2, previewY2,
                    40, 0.0625f,
                    mouseX, mouseY,
                    this.mannequin
            );
        }

        int listX = centerX - 20;
        int listY = listY();
        int maxVisible = visibleRows();

        for (int i = 0; i < maxVisible; i++) {
            int index = i + this.scrollOffset;
            if (index >= this.emoteNames.size()) break;

            String name = this.emoteNames.get(index);
            int rowY = listY + i * ROW_HEIGHT;
            boolean selected = name.equals(this.selectedEmote);

            if (selected) {
                guiGraphics.fill(listX - 2, rowY - 1, listX + 198, rowY + ROW_HEIGHT - 1, 0x33FFFFFF);
            }
            guiGraphics.drawString(this.font, name, listX, rowY + 3, selected ? 0xFFFFFFA0 : 0xFFFFFFFF);
        }

        if (this.emoteNames.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("envoys.gui.emote.empty"), centerX + 50, listY + 40, 0xFFFF5555);
        }

        this.selectButton.active = (this.selectedEmote != null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;
        int previewX1 = centerX - 180;
        int previewY1 = startY - 10;
        int previewX2 = centerX - 30;
        int previewY2 = startY + 150;

        if (mouseX >= previewX1 && mouseX <= previewX2 && mouseY >= previewY1 && mouseY <= previewY2) {
            this.isDraggingMannequin = true;
            return true;
        }

        int listX = centerX - 20;
        int listY = listY();
        int maxVisible = visibleRows();

        for (int i = 0; i < maxVisible; i++) {
            int index = i + this.scrollOffset;
            if (index >= this.emoteNames.size()) break;

            int rowY = listY + i * ROW_HEIGHT;
            if (mouseX >= listX && mouseX < listX + 200 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                selectEmote(this.emoteNames.get(index));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isDraggingMannequin = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDraggingMannequin) {
            this.mannequinYaw += (float) dragX * 2.5F;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int signum = (int) Math.signum(scrollY);
        if (signum == 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int maxScroll = Math.max(0, this.emoteNames.size() - visibleRows());
        if (maxScroll > 0) {
            this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - signum));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void tick() {
        EmoteIntegration.tickPreview(this.mannequin);
        long localVersion = ClientAnimStore.version();
        if (EmoteIntegration.serverListVersion() != this.lastServerListVersion
                || localVersion != this.lastLocalAnimVersion) {
            this.lastServerListVersion = EmoteIntegration.serverListVersion();
            this.lastLocalAnimVersion = localVersion;
            this.emoteNames.clear();
            this.emoteNames.addAll(EmoteIntegration.clientEmoteNames());
        }
    }

    @Override
    public void onClose() {
        EmoteIntegration.stopPreview(this.mannequin);
        if (this.parentScreen != null && this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void selectEmote(String name) {
        this.selectedEmote = name;
        EmoteIntegration.playPreview(this.mannequin, name);
    }

    private int listY() {
        return this.height / 2 - 75;
    }

    private int visibleRows() {
        return Math.max(1, (this.height - 35 - listY()) / ROW_HEIGHT);
    }
}
