package com.frost.envoys.gui.screen;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.NPCPassportData;
import com.frost.envoys.network.payload.SaveNPCPassportPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class NPCPassportScreen extends Screen {
    private final Screen parentScreen;
    private final NPCInteractManager manager;
    private final boolean isCreativeTuner;

    private String npcName;
    private float size;
    private float speed;
    private float hp;
    private Vec3 holdPosition;

    private boolean isVisible;
    private boolean isHoldPosEnabled;
    private boolean canTakeDamage;
    private boolean useGravity;
    private boolean creativeTunerOnly;
    private boolean lookLocked;

    private EditBox nameField;
    private EditBox sizeField;
    private EditBox speedField;
    private EditBox hpField;
    private EditBox posXField;
    private EditBox posYField;
    private EditBox posZField;

    public NPCPassportScreen(Screen parentScreen, NPCInteractManager manager, boolean isCreativeTuner) {
        super(Component.literal("Личность NPC"));
        this.parentScreen = parentScreen;
        this.manager = manager;
        this.isCreativeTuner = isCreativeTuner;

        NPCPassportData p = manager.passport;
        this.npcName = p.npcName != null ? p.npcName : "Steve";
        this.size = p.size;
        this.speed = p.speed;
        this.hp = p.hp;
        this.holdPosition = new Vec3(p.holdX, p.holdY, p.holdZ);
        this.isVisible = p.isVisible;
        this.isHoldPosEnabled = p.isHoldPosEnabled;
        this.canTakeDamage = p.canTakeDamage;
        this.useGravity = p.useGravity;
        this.creativeTunerOnly = p.creativeTunerOnly;
        this.lookLocked = p.lookLocked;
    }

    @Override
    protected void init() {
        super.init();

        int fieldWidth = 140;
        int fieldHeight = 20;
        int centerX = this.width / 2;

        int leftX = centerX - 150;
        int rightX = centerX + 10;

        this.nameField = new EditBox(this.font, leftX, 40, fieldWidth, fieldHeight, Component.literal("Имя"));
        this.nameField.setMaxLength(32);
        this.nameField.setValue(this.npcName);
        this.nameField.setResponder(text -> this.npcName = text);
        this.addRenderableWidget(this.nameField);

        this.sizeField = new EditBox(this.font, leftX, 80, fieldWidth, fieldHeight, Component.literal("Размер"));
        this.sizeField.setValue(String.valueOf(this.size));
        this.sizeField.setResponder(text -> this.size = parseOrDefaultFloat(text, this.size));
        this.addRenderableWidget(this.sizeField);

        this.speedField = new EditBox(this.font, leftX, 120, fieldWidth, fieldHeight, Component.literal("Скорость"));
        this.speedField.setValue(String.valueOf(this.speed));
        this.speedField.setResponder(text -> this.speed = parseOrDefaultFloat(text, this.speed));
        this.speedField.setEditable(this.isCreativeTuner);
        this.addRenderableWidget(this.speedField);

        this.hpField = new EditBox(this.font, leftX, 160, fieldWidth, fieldHeight, Component.literal("ХП"));
        this.hpField.setValue(String.valueOf(this.hp));
        this.hpField.setResponder(text -> this.hp = parseOrDefaultFloat(text, this.hp));
        this.hpField.setEditable(this.isCreativeTuner);
        this.addRenderableWidget(this.hpField);

        int posWidth = 44;
        this.posXField = new EditBox(this.font, rightX, 40, posWidth, fieldHeight, Component.literal("X"));
        this.posXField.setValue(String.valueOf(this.holdPosition.x));
        this.posXField.setResponder(text -> updateHoldPosition());
        this.posXField.setEditable(this.isCreativeTuner);
        this.addRenderableWidget(this.posXField);

        this.posYField = new EditBox(this.font, rightX + 48, 40, posWidth, fieldHeight, Component.literal("Y"));
        this.posYField.setValue(String.valueOf(this.holdPosition.y));
        this.posYField.setResponder(text -> updateHoldPosition());
        this.posYField.setEditable(this.isCreativeTuner);
        this.addRenderableWidget(this.posYField);

        this.posZField = new EditBox(this.font, rightX + 96, 40, posWidth, fieldHeight, Component.literal("Z"));
        this.posZField.setValue(String.valueOf(this.holdPosition.z));
        this.posZField.setResponder(text -> updateHoldPosition());
        this.posZField.setEditable(this.isCreativeTuner);
        this.addRenderableWidget(this.posZField);

        Checkbox visibleCheckbox = Checkbox.builder(Component.literal("Отображать модель (Видимость)"), this.font)
                .pos(rightX, 65)
                .selected(this.isVisible)
                .onValueChange((checkbox, selected) -> {
                    if (this.isCreativeTuner) this.isVisible = selected;
                })
                .build();
        visibleCheckbox.active = this.isCreativeTuner;
        this.addRenderableWidget(visibleCheckbox);

        Checkbox holdPosCheckbox = Checkbox.builder(Component.literal("Удерживать позицию"), this.font)
                .pos(rightX, 85)
                .selected(this.isHoldPosEnabled)
                .onValueChange((checkbox, selected) -> this.isHoldPosEnabled = selected)
                .build();
        holdPosCheckbox.active = true;
        this.addRenderableWidget(holdPosCheckbox);

        Checkbox damageCheckbox = Checkbox.builder(Component.literal("Получает урон"), this.font)
                .pos(rightX, 105)
                .selected(this.canTakeDamage)
                .onValueChange((checkbox, selected) -> {
                    if (this.isCreativeTuner) this.canTakeDamage = selected;
                })
                .build();
        damageCheckbox.active = this.isCreativeTuner;
        this.addRenderableWidget(damageCheckbox);

        Checkbox gravityCheckbox = Checkbox.builder(Component.literal("Подчиняется гравитации"), this.font)
                .pos(rightX, 125)
                .selected(this.useGravity)
                .onValueChange((checkbox, selected) -> {
                    if (this.isCreativeTuner) this.useGravity = selected;
                })
                .build();
        gravityCheckbox.active = this.isCreativeTuner;
        this.addRenderableWidget(gravityCheckbox);

        Checkbox creativeOnlyCheckbox = Checkbox.builder(Component.literal("Только Креатив-Тюнер"), this.font)
                .pos(rightX, 145)
                .selected(this.creativeTunerOnly)
                .onValueChange((checkbox, selected) -> {
                    if (this.isCreativeTuner) this.creativeTunerOnly = selected;
                })
                .build();
        creativeOnlyCheckbox.active = this.isCreativeTuner;
        this.addRenderableWidget(creativeOnlyCheckbox);

        Checkbox lookLockedCheckbox = Checkbox.builder(Component.literal("Заблокировать автоповорот"), this.font)
                .pos(rightX, 165)
                .selected(this.lookLocked)
                .onValueChange((checkbox, selected) -> this.lookLocked = selected)
                .build();
        lookLockedCheckbox.active = true;
        this.addRenderableWidget(lookLockedCheckbox);

        int buttonY = this.height - 35;

        this.addRenderableWidget(Button.builder(
                Component.literal("Сохранить"),
                button -> {
                    saveNpcData();
                    this.onClose();
                }
        ).bounds(centerX - 105, buttonY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Назад"),
                button -> {
                    if (this.parentScreen != null) {
                        Minecraft.getInstance().setScreen(this.parentScreen);
                    } else {
                        this.onClose();
                    }
                }
        ).bounds(centerX + 5, buttonY, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        int centerX = this.width / 2;
        int leftX = centerX - 150;
        int rightX = centerX + 10;

        guiGraphics.drawString(this.font, "Имя NPC:", leftX, 28, 0xFFA0A0A0);
        guiGraphics.drawString(this.font, "Размер:", leftX, 68, 0xFFA0A0A0);
        guiGraphics.drawString(this.font, "Скорость:", leftX, 108, this.isCreativeTuner ? 0xFFA0A0A0 : 0xFF555555);
        guiGraphics.drawString(this.font, "Здоровье (HP):", leftX, 148, this.isCreativeTuner ? 0xFFA0A0A0 : 0xFF555555);

        guiGraphics.drawString(this.font, "Позиция удержания (X / Y / Z):", rightX, 28, this.isCreativeTuner ? 0xFFA0A0A0 : 0xFF555555);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void updateHoldPosition() {
        if (!this.isCreativeTuner) return;
        double x = parseOrDefaultDouble(this.posXField.getValue(), this.holdPosition.x);
        double y = parseOrDefaultDouble(this.posYField.getValue(), this.holdPosition.y);
        double z = parseOrDefaultDouble(this.posZField.getValue(), this.holdPosition.z);
        this.holdPosition = new Vec3(x, y, z);
    }

    private float parseOrDefaultFloat(String text, float defaultValue) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double parseOrDefaultDouble(String text, double defaultValue) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void saveNpcData() {
        if (this.manager != null) {
            NPCPassportData p = this.manager.passport;
            p.npcName = this.npcName;
            p.isHoldPosEnabled = this.isHoldPosEnabled;
            p.size = this.size;
            p.lookLocked = this.lookLocked;

            if (this.isCreativeTuner) {
                p.speed = this.speed;
                p.hp = this.hp;
                p.holdX = this.holdPosition.x;
                p.holdY = this.holdPosition.y;
                p.holdZ = this.holdPosition.z;
                p.isVisible = this.isVisible;
                p.canTakeDamage = this.canTakeDamage;
                p.useGravity = this.useGravity;
                p.creativeTunerOnly = this.creativeTunerOnly;
            }

            PacketDistributor.sendToServer(new SaveNPCPassportPayload(
                this.manager.npcUUID,
                p.npcName,
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
                p.emote != null ? p.emote : ""
            ));
        }
    }
}