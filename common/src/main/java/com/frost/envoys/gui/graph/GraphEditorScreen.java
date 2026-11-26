package com.frost.envoys.gui.graph;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GraphEditorScreen extends Screen {

    private static final int PANEL_WIDTH = 180;
    private static final int NODE_WIDTH = 160;
    private static final int NODE_HEIGHT = 64;
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 2.0;
    private static final double ZOOM_STEP = 0.1;
    private static final int GRID = 24;
    private static final int PALETTE_ROW_HEIGHT = 28;
    private static final int PALETTE_ROW_GAP = 8;
    private static final int PALETTE_TOP = 40;

    private final Screen parentScreen;

    private final List<GraphViewNode> nodes = new ArrayList<>();
    private int nextNodeId = 1;
    private boolean demoInitialized;

    private double zoom = 1.0;
    private double panX = 0;
    private double panY = 0;

    private GraphViewNode draggedNode;
    private double dragOffsetX;
    private double dragOffsetY;

    private boolean panning;
    private double lastMouseX;
    private double lastMouseY;

    private GraphNodeKind paletteDragKind;
    private boolean paletteDragMoved;
    private double palettePressX;
    private double palettePressY;

    public GraphEditorScreen(Screen parentScreen) {
        super(Component.translatable("envoys.gui.graph.title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        if (!this.demoInitialized) {
            this.demoInitialized = true;
            int canvasW = this.width - PANEL_WIDTH;
            int x = canvasW / 2 - NODE_WIDTH / 2;
            int startY = this.height / 2 - (3 * NODE_HEIGHT + 2 * 24) / 2;
            this.nodes.add(new GraphViewNode("n" + this.nextNodeId++, GraphNodeKind.START, x, startY));
            this.nodes.add(new GraphViewNode("n" + this.nextNodeId++, GraphNodeKind.DIALOGUE,
                    x, startY + NODE_HEIGHT + 24));
            this.nodes.add(new GraphViewNode("n" + this.nextNodeId++, GraphNodeKind.QUEST_CHECK,
                    x, startY + 2 * (NODE_HEIGHT + 24)));
        }

        this.addRenderableWidget(Button.builder(Component.translatable("envoys.gui.graph.exit"), button -> this.onClose())
                .bounds(this.width - PANEL_WIDTH + 15, this.height - 32, PANEL_WIDTH - 30, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int canvasW = this.width - PANEL_WIDTH;
        guiGraphics.fill(0, 0, canvasW, this.height, 0xFF1A1A1A);
        this.renderGrid(guiGraphics, canvasW);

        guiGraphics.enableScissor(0, 0, canvasW, this.height);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.panX, this.panY, 0);
        guiGraphics.pose().scale((float) this.zoom, (float) this.zoom, 1);
        this.renderNodes(guiGraphics, mouseX, mouseY);
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();

        this.renderPanel(guiGraphics, mouseX, mouseY);

        for (GuiEventListener listener : this.children()) {
            if (listener instanceof Renderable renderable) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        this.renderPaletteGhost(guiGraphics, mouseX, mouseY);
    }

    private void renderGrid(GuiGraphics guiGraphics, int canvasW) {
        double step = GRID * this.zoom;
        if (step < 2) {
            return;
        }
        double ox = this.panX % step;
        if (ox < 0) {
            ox += step;
        }
        double oy = this.panY % step;
        if (oy < 0) {
            oy += step;
        }

        int index = 0;
        for (double x = ox; x < canvasW; x += step, index++) {
            int color = index % 5 == 0 ? 0xFF303030 : 0xFF242424;
            guiGraphics.fill((int) x, 0, (int) x + 1, this.height, color);
        }
        index = 0;
        for (double y = oy; y < this.height; y += step, index++) {
            int color = index % 5 == 0 ? 0xFF303030 : 0xFF242424;
            guiGraphics.fill(0, (int) y, canvasW, (int) y + 1, color);
        }
    }

    private void renderNodes(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        double worldX = this.screenToWorldX(mouseX);
        double worldY = this.screenToWorldY(mouseY);
        for (GraphViewNode node : this.nodes) {
            this.renderNode(guiGraphics, node, worldX, worldY);
        }
    }

    private void renderNode(GuiGraphics guiGraphics, GraphViewNode node, double worldX, double worldY) {
        boolean hover = worldX >= node.x && worldX <= node.x + node.width
                && worldY >= node.y && worldY <= node.y + node.height;
        int border = hover ? 0xFFAAAAAA : 0xFF555555;

        this.fillRoundedRect(guiGraphics, node.x, node.y, node.width, node.height, 4, 0xFF2A2A2A);
        this.fillRoundedRect(guiGraphics, node.x, node.y, node.width, 20, 4, node.kind.color());
        guiGraphics.renderOutline(node.x, node.y, node.width, node.height, border);

        guiGraphics.drawString(this.font, node.kind.display(), node.x + 8, node.y + 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, node.id, node.x + 8, node.y + node.height - 14, 0xFF9A9A9A, false);

        int pinY = node.y + node.height / 2;
        this.fillCircle(guiGraphics, node.x, pinY, 6, 0xFF1A1A1A);
        this.fillCircle(guiGraphics, node.x, pinY, 5, 0xFFB0B0B0);
        this.fillCircle(guiGraphics, node.x + node.width, pinY, 6, 0xFF1A1A1A);
        this.fillCircle(guiGraphics, node.x + node.width, pinY, 5, 0xFF7FC8FF);
    }

    private void renderPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int panelX = this.width - PANEL_WIDTH;
        guiGraphics.fill(panelX, 0, this.width, this.height, 0xFF232323);
        guiGraphics.fill(panelX, 0, panelX + 1, this.height, 0xFF3A3A3A);

        guiGraphics.drawString(this.font, Component.translatable("envoys.gui.graph.palette"),
                panelX + 15, 15, 0xFFFFFF, false);

        GraphNodeKind[] kinds = GraphNodeKind.values();
        for (int i = 0; i < kinds.length; i++) {
            GraphNodeKind kind = kinds[i];
            int rowY = this.paletteRowTop(i);
            boolean hover = this.paletteRowAt(mouseX, mouseY) == i;
            guiGraphics.fill(panelX + 15, rowY, this.width - 15, rowY + PALETTE_ROW_HEIGHT,
                    hover ? 0xFF3A3A3A : 0xFF2E2E2E);
            guiGraphics.fill(panelX + 15, rowY, panelX + 21, rowY + PALETTE_ROW_HEIGHT, kind.color());
            guiGraphics.drawString(this.font, kind.display(), panelX + 29, rowY + 10, 0xFFFFFF, false);
        }

        int hintY = this.paletteRowTop(kinds.length) + 4;
        int hintWidth = PANEL_WIDTH - 30;
        guiGraphics.drawWordWrap(this.font, Component.translatable("envoys.gui.graph.hint_add"),
                panelX + 15, hintY, hintWidth, 0xFF9A9A9A);
        guiGraphics.drawWordWrap(this.font, Component.translatable("envoys.gui.graph.hint_controls"),
                panelX + 15, hintY + 30, hintWidth, 0xFF9A9A9A);
    }

    private void renderPaletteGhost(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.paletteDragKind == null || !this.paletteDragMoved || !this.isOverCanvas(mouseX, mouseY)) {
            return;
        }
        int ghostX = (int) Math.round(this.screenToWorldX(mouseX) - NODE_WIDTH / 2.0);
        int ghostY = (int) Math.round(this.screenToWorldY(mouseY) - NODE_HEIGHT / 2.0);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.panX, this.panY, 0);
        guiGraphics.pose().scale((float) this.zoom, (float) this.zoom, 1);
        this.fillRoundedRect(guiGraphics, ghostX, ghostY, NODE_WIDTH, NODE_HEIGHT, 4, 0x802A2A2A);
        this.fillRoundedRect(guiGraphics, ghostX, ghostY, NODE_WIDTH, 20, 4,
                (this.paletteDragKind.color() & 0x00FFFFFF) | 0x80000000);
        guiGraphics.pose().popPose();
    }

    private void fillRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int color) {
        int r = Math.min(radius, Math.min(width, height) / 2);
        guiGraphics.fill(x + r, y, x + width - r, y + height, color);
        guiGraphics.fill(x, y + r, x + r, y + height - r, color);
        guiGraphics.fill(x + width - r, y + r, x + width, y + height - r, color);
    }

    private void fillCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        int radiusSq = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt(radiusSq - dy * dy);
            guiGraphics.fill(centerX - dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, color);
        }
    }

    private int paletteRowTop(int index) {
        return PALETTE_TOP + index * (PALETTE_ROW_HEIGHT + PALETTE_ROW_GAP);
    }

    private int paletteRowAt(double mouseX, double mouseY) {
        int panelX = this.width - PANEL_WIDTH;
        if (mouseX < panelX + 15 || mouseX > this.width - 15) {
            return -1;
        }
        GraphNodeKind[] kinds = GraphNodeKind.values();
        for (int i = 0; i < kinds.length; i++) {
            int rowY = this.paletteRowTop(i);
            if (mouseY >= rowY && mouseY < rowY + PALETTE_ROW_HEIGHT) {
                return i;
            }
        }
        return -1;
    }

    private double screenToWorldX(double screenX) {
        return (screenX - this.panX) / this.zoom;
    }

    private double screenToWorldY(double screenY) {
        return (screenY - this.panY) / this.zoom;
    }

    private boolean isOverCanvas(double mouseX, double mouseY) {
        return mouseX >= 0 && mouseX < this.width - PANEL_WIDTH
                && mouseY >= 0 && mouseY < this.height;
    }

    private GraphViewNode findNodeAt(double worldX, double worldY) {
        for (int i = this.nodes.size() - 1; i >= 0; i--) {
            GraphViewNode node = this.nodes.get(i);
            if (worldX >= node.x && worldX <= node.x + node.width
                    && worldY >= node.y && worldY <= node.y + node.height) {
                return node;
            }
        }
        return null;
    }

    private void addNodeAt(GraphNodeKind kind, double worldX, double worldY) {
        this.nodes.add(new GraphViewNode("n" + this.nextNodeId++, kind,
                (int) Math.round(worldX), (int) Math.round(worldY)));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0 || !this.isOverCanvas(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        double worldX = this.screenToWorldX(mouseX);
        double worldY = this.screenToWorldY(mouseY);
        double newZoom = clamp(this.zoom + Math.signum(scrollY) * ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
        this.panX = mouseX - worldX * newZoom;
        this.panY = mouseY - worldY * newZoom;
        this.zoom = newZoom;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 2) {
            if (this.isOverCanvas(mouseX, mouseY)) {
                this.panning = true;
                this.lastMouseX = mouseX;
                this.lastMouseY = mouseY;
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 0) {
            if (mouseX >= this.width - PANEL_WIDTH) {
                int row = this.paletteRowAt(mouseX, mouseY);
                if (row >= 0) {
                    this.paletteDragKind = GraphNodeKind.values()[row];
                    this.paletteDragMoved = false;
                    this.palettePressX = mouseX;
                    this.palettePressY = mouseY;
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
            if (this.isOverCanvas(mouseX, mouseY)) {
                double worldX = this.screenToWorldX(mouseX);
                double worldY = this.screenToWorldY(mouseY);
                GraphViewNode node = this.findNodeAt(worldX, worldY);
                if (node != null) {
                    this.nodes.remove(node);
                    this.nodes.add(node);
                    this.draggedNode = node;
                    this.dragOffsetX = worldX - node.x;
                    this.dragOffsetY = worldY - node.y;
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.paletteDragKind != null && button == 0) {
            if (Math.hypot(mouseX - this.palettePressX, mouseY - this.palettePressY) > 4) {
                this.paletteDragMoved = true;
            }
            return true;
        }
        if (this.draggedNode != null && button == 0) {
            this.draggedNode.x = (int) Math.round(this.screenToWorldX(mouseX) - this.dragOffsetX);
            this.draggedNode.y = (int) Math.round(this.screenToWorldY(mouseY) - this.dragOffsetY);
            return true;
        }
        if (this.panning && button == 2) {
            this.panX += mouseX - this.lastMouseX;
            this.panY += mouseY - this.lastMouseY;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.paletteDragKind != null && button == 0) {
            if (this.paletteDragMoved) {
                if (this.isOverCanvas(mouseX, mouseY)) {
                    this.addNodeAt(this.paletteDragKind,
                            this.screenToWorldX(mouseX) - NODE_WIDTH / 2.0,
                            this.screenToWorldY(mouseY) - NODE_HEIGHT / 2.0);
                }
            } else {
                double centerX = this.screenToWorldX((this.width - PANEL_WIDTH) / 2.0);
                double centerY = this.screenToWorldY(this.height / 2.0);
                this.addNodeAt(this.paletteDragKind, centerX - NODE_WIDTH / 2.0, centerY - NODE_HEIGHT / 2.0);
            }
            this.paletteDragKind = null;
            this.paletteDragMoved = false;
            return true;
        }
        if (this.draggedNode != null && button == 0) {
            this.draggedNode = null;
            return true;
        }
        if (this.panning && button == 2) {
            this.panning = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (this.parentScreen != null) {
            Minecraft.getInstance().setScreen(this.parentScreen);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
