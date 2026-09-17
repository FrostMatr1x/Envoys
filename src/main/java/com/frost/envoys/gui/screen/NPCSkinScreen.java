package com.frost.envoys.gui.screen;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.NPCPassportData;
import com.frost.envoys.network.payload.SaveNPCPassportPayload;
import com.frost.envoys.network.payload.SaveNPCSkinPayload;
import com.frost.envoys.npc.entity.BaseNPC;
import com.frost.envoys.npc.entity.MannequinEntity;
import com.frost.envoys.skin.gui.SkinGuiPreview;
import com.frost.envoys.skin.model.SkinIndexData;
import com.frost.envoys.skin.service.SkinLocalService;
import com.frost.envoys.skin.service.SkinSyncService;
import com.frost.envoys.util.ClientPathManager;
import com.frost.envoys.util.PathManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NPCSkinScreen extends Screen {

    public enum SkinType {
        NICKNAME("По нику игрока"),
        URL("По ссылке (URL)"),
        FILE("Из файла (.png)");

        private final String displayName;
        SkinType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
        public SkinType next() {
            SkinType[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    private final UUID npcUuid;
    private final Screen parentScreen;
    private SkinType currentSkinType = SkinType.NICKNAME;
    private String currentSkinValue = "";

    private Button typeToggleButton;
    private EditBox inputField;
    private FileListWidget fileListWidget;
    private Button updateButton;
    private Button modelToggleButton;
    private Button emoteButton;

    private String currentEmote = "";

    private final List<String> localSkinFiles = new ArrayList<>();

    private MannequinEntity mannequin;
    private float mannequinYaw = 0.0F;
    private boolean isDraggingMannequin = false;
    private ResourceLocation currentTexture = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    private String currentModel = "classic";

    private boolean isLoading = false;
    private String errorMessage = null;
    private String confirmedHash = null;

    public NPCSkinScreen(Screen parentScreen, UUID npcUuid) {
        super(Component.literal("Редактор скина NPC"));
        this.parentScreen = parentScreen;
        this.npcUuid = npcUuid;

        NPCInteractManager manager = NPCInteractManager.byUUID(npcUuid)
                .orElseGet(() -> new NPCInteractManager(npcUuid));
        if (manager.passport.emote != null) {
            this.currentEmote = manager.passport.emote;
        }

        if (Minecraft.getInstance().level != null) {
            for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
                if (entity.getUUID().equals(npcUuid) && entity instanceof BaseNPC baseNPC) {
                    if (baseNPC.getEmoteType() != null) {
                        this.currentEmote = baseNPC.getEmoteType();
                    }
                    break;
                }
            }
        }

        currentSkinValue = manager.passport.skinValue;
        currentSkinType = SkinType.valueOf(manager.passport.skinType);
        currentModel = manager.passport.skinModel;
    }

    @Override
    protected void init() {
        super.init();

        scanSkinFolder();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int startY = centerY - 70;
        int rightColumnX = centerX - 20;
        int fieldWidth = 200;

        this.typeToggleButton = Button.builder(
                Component.literal("Тип: " + currentSkinType.getDisplayName()),
                button -> {
                    this.currentSkinType = this.currentSkinType.next();
                    this.typeToggleButton.setMessage(Component.literal("Тип: " + currentSkinType.getDisplayName()));
                    updateVisibility();
                }
        ).bounds(rightColumnX, startY - 5, fieldWidth, 20).build();
        this.addRenderableWidget(this.typeToggleButton);

        this.inputField = new EditBox(this.font, rightColumnX, startY + 30, fieldWidth, 20, Component.literal("Skin Input"));
        this.inputField.setMaxLength(256);
        this.inputField.setValue(this.currentSkinValue);
        this.inputField.setResponder(text -> {
            if (currentSkinType != SkinType.FILE) {
                this.currentSkinValue = text;
            }
        });
        this.addRenderableWidget(this.inputField);

        this.fileListWidget = new FileListWidget(this.minecraft, fieldWidth, 75, startY + 30, 20);
        this.fileListWidget.setX(rightColumnX);
        this.addRenderableWidget(this.fileListWidget);

        startY -= 25;

        this.modelToggleButton = Button.builder(
                Component.literal("Модель: " + currentModel.toUpperCase()),
                button -> {
                    this.currentModel = this.currentModel.equals("classic") ? "slim" : "classic";
                    this.modelToggleButton.setMessage(Component.literal("Модель: " + currentModel.toUpperCase()));
                    if (mannequin != null) mannequin.setSlim(this.currentModel.equals("slim"));
                }
        ).bounds(rightColumnX, startY + 125, fieldWidth, 20).build();
        this.addRenderableWidget(this.modelToggleButton);

        this.emoteButton = Button.builder(
                Component.literal("Анимация: " + (this.currentEmote.isBlank() ? "не выбрана" : this.currentEmote)),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new NPCEmoteScreen(this, this.mannequin, name -> {
                            this.currentEmote = name;
                            this.emoteButton.setMessage(Component.literal("Анимация: " + (name.isBlank() ? "не выбрана" : name)));
                        }));
                    }
                }
        ).bounds(rightColumnX, startY + 150, fieldWidth, 20).build();
        this.addRenderableWidget(this.emoteButton);

        this.updateButton = Button.builder(
                Component.literal("Обновить"),
                button -> onRefreshSkin()
        ).bounds(rightColumnX, startY + 175, fieldWidth, 20).build();
        this.addRenderableWidget(this.updateButton);

        int bottomY = this.height - 30;

        this.addRenderableWidget(Button.builder(
                Component.literal("Сохранить"),
                button -> {
                    saveSkinData();
                    this.onClose();
                }
        ).bounds(centerX - 105, bottomY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Назад"),
                button -> {
                    if (this.parentScreen != null) {
                        Minecraft.getInstance().setScreen(this.parentScreen);
                    } else {
                        this.onClose();
                    }
                }
        ).bounds(centerX + 5, bottomY, 100, 20).build());

        updateVisibility();

        if (this.minecraft != null && this.minecraft.level != null) {
            this.mannequin = new MannequinEntity(this.minecraft.level);
            onRefreshSkin();
        }
    }

    private void updateVisibility() {
        boolean isFile = (currentSkinType == SkinType.FILE);
        this.inputField.setVisible(!isFile);
        this.inputField.setFocused(!isFile);
        this.fileListWidget.setVisible(isFile);
    }

    private void onRefreshSkin() {
        if (currentSkinType == SkinType.FILE) {
            FileListWidget.FileEntry selected = this.fileListWidget.getSelected();
            if (selected != null) {
                this.currentSkinValue = selected.getFileName();
            }
        } else {
            this.currentSkinValue = this.inputField.getValue();
        }

        if (this.currentSkinValue == null || this.currentSkinValue.isBlank()) {
            this.errorMessage = "Поле ввода пусто!";
            return;
        }

        this.isLoading = true;
        this.errorMessage = null;

        SkinSyncService.loadSkinAsync(currentSkinValue, currentSkinType.name(), true)
                .thenAccept(result -> {
                    Minecraft.getInstance().execute(() -> {
                        this.isLoading = false;
                        if (result.isSuccess()) {
                            this.currentTexture = SkinGuiPreview.registerDynamicSkin(currentSkinValue, result.pngData());
                            this.currentModel = result.model();
                            this.modelToggleButton.setMessage(Component.literal("Модель: " + currentModel.toUpperCase()));

                            if (this.mannequin != null) {
                                this.mannequin.setCustomTexture(this.currentTexture);
                                this.mannequin.setSlim("slim".equalsIgnoreCase(currentModel));
                            }
                        } else {
                            this.errorMessage = result.errorMsg();
                        }
                    });
                });
    }

    public void onSkinConfirmed(String hash) {
        this.confirmedHash = hash;
        this.errorMessage = null;
    }

    public void onSkinDataReceived(byte[] pngData) {
        this.currentTexture = SkinGuiPreview.registerDynamicSkin("server_skin", pngData);
    }

    private void scanSkinFolder() {
        this.localSkinFiles.clear();
        File skinsDir = ClientPathManager.getClientEnvoysDir().toFile();
        if (!skinsDir.exists()) skinsDir.mkdirs();

        SkinIndexData index = SkinLocalService.readIndex(ClientPathManager.getClientIndexFile());
        index.skins.forEach(e -> this.localSkinFiles.add(e.path));
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
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x80101010, 0x99101010);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, Math.max(10, startY - 25), 0xFFFFFFFF);

        int previewX1 = centerX - 180;
        int previewY1 = startY - 10;
        int previewX2 = centerX - 30;
        int previewY2 = startY + 150;

        guiGraphics.fill(previewX1, previewY1, previewX2, previewY2, 0xFF000000);
        guiGraphics.renderOutline(previewX1, previewY1, previewX2 - previewX1, previewY2 - previewY1, 0xFF555555);

        if (mannequin != null) {
            mannequin.setYRot(mannequinYaw);
            mannequin.yRotO = mannequinYaw;

            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics,
                    previewX1, previewY1,
                    previewX2, previewY2,
                    40, 0.0625f,
                    mouseX, mouseY,
                    mannequin
            );
        }

        if (isLoading) {
            guiGraphics.drawCenteredString(this.font, "Загрузка...", (previewX1 + previewX2) / 2, (previewY1 + previewY2) / 2, 0xFFFFFF00);
        } else if (errorMessage != null) {
            guiGraphics.drawString(this.font, "Ошибка: " + errorMessage, centerX - 20, startY + 185, 0xFFFF5555);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void saveSkinData() {
        if (this.currentSkinValue != null && !this.currentSkinValue.isBlank()) {
            PacketDistributor.sendToServer(
                    new SaveNPCSkinPayload(
                            this.npcUuid,
                            this.currentSkinType.name(),
                            this.currentSkinValue,
                            this.currentModel
                    )
            );
        }

        NPCInteractManager manager = NPCInteractManager.byUUID(this.npcUuid)
                .orElseGet(() -> new NPCInteractManager(this.npcUuid));
        
        NPCPassportData p = manager.passport;
        p.emote = this.currentEmote;

        PacketDistributor.sendToServer(
                new SaveNPCPassportPayload(
                        this.npcUuid,
                        p.npcName != null ? p.npcName : "Steve",
                        p.size,
                        p.speed,
                        p.hp,
                        new Vec3(p.holdX, p.holdY, p.holdZ),
                        p.isVisible,
                        p.isHoldPosEnabled,
                        p.canTakeDamage,
                        p.useGravity,
                        p.creativeTunerOnly,
                        p.lookLocked,
                        this.currentEmote
                )
        );
    }

    private class FileListWidget extends ObjectSelectionList<FileListWidget.FileEntry> {
        public FileListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
            refreshList();
        }

        public void refreshList() {
            this.clearEntries();
            for (String fileName : localSkinFiles) {
                this.addEntry(new FileEntry(fileName));
            }
        }

        public void setVisible(boolean visible) {}

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (currentSkinType == SkinType.FILE) {
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        public class FileEntry extends ObjectSelectionList.Entry<FileEntry> {
            private final String fileName;
            public FileEntry(String fileName) { this.fileName = fileName; }
            public String getFileName() { return fileName; }
            @Override public Component getNarration() { return Component.literal(fileName); }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
                int textColor = isHovered ? 0xFFFFFFA0 : 0xFFFFFFFF;
                guiGraphics.drawString(font, fileName, left + 5, top + 3, textColor);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                setSelected(this);
                currentSkinValue = fileName;
                return true;
            }
        }
    }
}