package com.frost.envoys.client.gui;

import net.minecraft.resources.ResourceLocation;

/**
 * Configurable geometry of the NPC trade screen. Loads
 * {@code assets/envoys/gui/trade_layout.json}, which a resource pack can override.
 * Missing fields keep the defaults below, so a partial JSON is valid.
 */
public final class TradeLayout {

    public static final ResourceLocation LOCATION =
            ResourceLocation.fromNamespaceAndPath("envoys", "gui/trade_layout.json");

    private static volatile TradeLayout current = new TradeLayout();

    public Image image = new Image();
    public ListCfg list = new ListCfg();
    public RowCfg row = new RowCfg();
    public ScrollerCfg scroller = new ScrollerCfg();
    public ButtonCfg tradeAll = new ButtonCfg();
    public LabelsCfg labels = new LabelsCfg();

    public TradeLayout() {
    }

    public static TradeLayout get() {
        return current;
    }

    public static void reload(net.minecraft.server.packs.resources.ResourceManager manager) {
        current = GuiLayoutLoader.load(manager, LOCATION, TradeLayout.class, new TradeLayout());
    }

    public static class Image {
        public int width = 276;
        public int height = 166;
        public int texWidth = 276;
        public int texHeight = 166;
    }

    public static class ListCfg {
        public int x = 5;
        public int y = 20;
        public int rowStep = 20;
        public int rows = 7;
        public int offerWidth = 88;
        public int offerHeight = 18;

        public int safeRows() {
            return Math.max(1, Math.min(rows, 32));
        }
    }

    public static class RowCfg {
        public int in1 = 5;
        public int in2 = 35;
        public int arrow = 52;
        public int out = 68;
        public int arrowWidth = 10;
        public int arrowHeight = 9;
        public float itemScale = 0.9F;
        public float in1Scale = 0.9F;
        public float in2Scale = 0.9F;
        public float outScale = 0.9F;

        public float scaleFor(int slot) {
            float s = switch (slot) {
                case 0 -> in1Scale;
                case 1 -> in2Scale;
                default -> outScale;
            };
            return s <= 0.0F ? itemScale : s;
        }
    }

    public static class ScrollerCfg {
        public int x = 94;
        public int y = 18;
        public int width = 6;
        public int height = 27;
        public int trackHeight = 139;
    }

    public static class ButtonCfg {
        public int x = 113;
        public int y = 12;
        public int width = 10;
        public int height = 9;
        public boolean visible = true;
    }

    public static class LabelsCfg {
        public int titleX = 11;
        public int titleY = 7;
        public boolean titleVisible = true;
        public int inventoryX = 109;
        public int inventoryY = 72;
        public boolean inventoryVisible = true;
    }
}
