package com.frost.envoys.client.gui;

import net.minecraft.resources.ResourceLocation;

/**
 * Configurable geometry of the quest wall screen. Loads
 * {@code assets/envoys/gui/quest_layout.json}, which a resource pack can override.
 * Missing fields keep the defaults below, so a partial JSON is valid.
 */
public final class QuestLayout {

    public static final ResourceLocation LOCATION =
            ResourceLocation.fromNamespaceAndPath("envoys", "gui/quest_layout.json");

    private static volatile QuestLayout current = new QuestLayout();

    public Panel panel = new Panel();
    public ListCfg list = new ListCfg();

    public QuestLayout() {
    }

    public static QuestLayout get() {
        return current;
    }

    public static void reload(net.minecraft.server.packs.resources.ResourceManager manager) {
        current = GuiLayoutLoader.load(manager, LOCATION, QuestLayout.class, new QuestLayout());
    }

    public static class Panel {
        public String xMode = "CENTER";
        public int x = 0;
        public String yMode = "CENTER";
        public int y = 0;
        public String widthMode = "FIXED";
        public float widthValue = 260;
        public String heightMode = "FIXED";
        public float heightValue = 220;
        public int pad = 12;
        public int headerHeight = 18;
        public int footerHeight = 24;

        public int resolveX(int screenWidth, int resolvedWidth) {
            DialogLayout.SizeMode mode = DialogLayout.SizeMode.parse(xMode, DialogLayout.SizeMode.FIXED);
            return switch (mode) {
                case FIXED -> x;
                default -> (screenWidth - resolvedWidth) / 2;
            };
        }

        public int resolveY(int screenHeight, int resolvedHeight) {
            DialogLayout.SizeMode mode = DialogLayout.SizeMode.parse(yMode, DialogLayout.SizeMode.FIXED);
            return switch (mode) {
                case FIXED -> y;
                default -> (screenHeight - resolvedHeight) / 2;
            };
        }

        public int resolveWidth(int screenWidth) {
            DialogLayout.SizeMode mode = DialogLayout.SizeMode.parse(widthMode, DialogLayout.SizeMode.FIXED);
            return switch (mode) {
                case SCREEN_PERCENT -> (int) (screenWidth * widthValue);
                default -> (int) widthValue;
            };
        }

        public int resolveHeight(int screenHeight) {
            DialogLayout.SizeMode mode = DialogLayout.SizeMode.parse(heightMode, DialogLayout.SizeMode.FIXED);
            return switch (mode) {
                case SCREEN_PERCENT -> (int) (screenHeight * heightValue);
                default -> (int) heightValue;
            };
        }
    }

    public static class ListCfg {
        public int rowHeight = 30;
        public Scroller scroller = new Scroller();

        public static class Scroller {
            public int width = 6;
            public int height = 27;
        }
    }
}
