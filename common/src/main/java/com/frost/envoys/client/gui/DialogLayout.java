package com.frost.envoys.client.gui;

import net.minecraft.resources.ResourceLocation;

/**
 * Configurable geometry and text styling of the NPC dialog screen. Loads
 * {@code assets/envoys/gui/dialog_layout.json}, which a resource pack can override.
 * Missing fields keep the defaults below, so a partial JSON is valid.
 */
public final class DialogLayout {

    public static final ResourceLocation LOCATION =
            ResourceLocation.fromNamespaceAndPath("envoys", "gui/dialog_layout.json");

    private static volatile DialogLayout current = new DialogLayout();

    public Frame frame = new Frame();
    public NameCfg name = new NameCfg();
    public TextCfg text = new TextCfg();
    public OptionsCfg options = new OptionsCfg();

    public DialogLayout() {
    }

    public static DialogLayout get() {
        return current;
    }

    public static void reload(net.minecraft.server.packs.resources.ResourceManager manager) {
        current = GuiLayoutLoader.load(manager, LOCATION, DialogLayout.class, new DialogLayout());
    }

    public static int parseColor(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            String hex = raw.trim().replace("0x", "").replace("0X", "").replace("#", "");
            return (int) Long.parseUnsignedLong(hex, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public enum SizeMode {
        FIXED, SCREEN_MINUS, SCREEN_PERCENT, CENTER;

        public static SizeMode parse(String raw, SizeMode fallback) {
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            try {
                return valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return fallback;
            }
        }
    }

    public static class Frame {
        public int x = 2;
        public int y = 10;
        public String widthMode = "SCREEN_MINUS";
        public float widthValue = 20;
        public String heightMode = "SCREEN_PERCENT";
        public float heightValue = 0.4F;
        public int borderThickness = 7;

        public int resolveWidth(int screenWidth, int contentWidth) {
            SizeMode mode = SizeMode.parse(widthMode, SizeMode.SCREEN_MINUS);
            return switch (mode) {
                case FIXED -> Math.max(1, (int) widthValue);
                case SCREEN_PERCENT -> Math.max(1, (int) (screenWidth * widthValue));
                default -> Math.max(1, screenWidth - (int) widthValue);
            };
        }

        public int resolveHeight(int screenHeight, int contentHeight) {
            SizeMode mode = SizeMode.parse(heightMode, SizeMode.SCREEN_PERCENT);
            return switch (mode) {
                case FIXED -> Math.max(1, (int) heightValue);
                case SCREEN_MINUS -> Math.max(1, screenHeight - (int) heightValue);
                default -> Math.max(1, (int) (screenHeight * heightValue));
            };
        }
    }

    public static class NameCfg {
        public boolean visible = true;
        public float scale = 1.0F;
        public int padLeft = 10;
        public int padTop = 5;
        public String color = "0xFFFFFF00";
        public boolean shadow = true;

        public float safeScale() {
            return Math.max(0.1F, scale);
        }

        public int colorOrDefault() {
            return parseColor(color, 0xFFFFFF00);
        }
    }

    public static class TextCfg {
        public float scale = 1.0F;
        public int lineHeight = 10;
        public int padLeft = 5;
        public int padRight = 8;
        public int padTop = 5;
        public int padBottom = 2;
        public int offsetFromName = 12;
        public String color = "0xFFFFFFFF";
        public boolean shadow = true;
        public Scroller scroller = new Scroller();

        public float safeScale() {
            return Math.max(0.1F, scale);
        }

        public int colorOrDefault() {
            return parseColor(color, 0xFFFFFFFF);
        }

        public static class Scroller {
            public int offsetRight = 8;
            public int width = 6;
            public int height = 27;
        }
    }

    public static class OptionsCfg {
        public boolean visible = true;
        public int x = 10;
        public int yOffsetFromFrame = 15;
        public String widthMode = "SCREEN_PERCENT";
        public float widthValue = 0.6F;
        public int height = 22;
        public int spacing = 6;
        public int maxVisible = 5;
        public float scale = 1.0F;

        public float safeScale() {
            return Math.max(0.1F, scale);
        }

        public int resolveWidth(int screenWidth) {
            SizeMode mode = SizeMode.parse(widthMode, SizeMode.SCREEN_PERCENT);
            return switch (mode) {
                case FIXED -> Math.max(1, (int) widthValue);
                case SCREEN_MINUS -> Math.max(1, screenWidth - (int) widthValue);
                default -> Math.max(1, (int) (screenWidth * widthValue));
            };
        }
    }
}
