package com.frost.envoys.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorUtilsTest {

    @Test
    void nullAndEmptyYieldEmptyComponent() {
        assertEquals("", ColorUtils.parse(null).getString());
        assertEquals("", ColorUtils.parse("").getString());
        assertTrue(ColorUtils.parse(null).getSiblings().isEmpty());
    }

    @Test
    void plainTextIsPreserved() {
        Component component = ColorUtils.parse("plain");
        assertEquals("plain", component.getString());
        assertEquals(1, component.getSiblings().size());
    }

    @Test
    void hexColorsWork() {
        assertColor("&#FF0000X", 0xFF0000);
        assertColor("<#00FF00>X", 0x00FF00);
        assertColor("#0000FFX", 0x0000FF);
    }

    @Test
    void legacyColorsAndFormatsWork() {
        Component green = ColorUtils.parse("&aX");
        assertEquals("X", green.getString());
        assertNotNull(green.getSiblings().get(0).getStyle().getColor());

        Component bold = ColorUtils.parse("&lX");
        assertTrue(bold.getSiblings().get(0).getStyle().isBold());

        Component italic = ColorUtils.parse("&oX");
        assertTrue(italic.getSiblings().get(0).getStyle().isItalic());
    }

    @Test
    void resetClearsStyle() {
        Component component = ColorUtils.parse("&a&l&rPlain");
        Component plain = component.getSiblings().get(component.getSiblings().size() - 1);
        assertEquals("Plain", plain.getString());
        assertNull(plain.getStyle().getColor());
        assertFalse(plain.getStyle().isBold());
    }

    @Test
    void brokenHexIsPlainText() {
        Component component = ColorUtils.parse("&#ZZZZZZX");
        assertEquals("&#ZZZZZZX", component.getString());
    }

    @Test
    void formattedStringRoundTrip() {
        assertEquals("", ColorUtils.toFormattedString(null));

        Component component = Component.literal("A")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF0000)).withBold(true));
        String formatted = ColorUtils.toFormattedString(component);
        assertEquals("&#FF0000&lA", formatted);

        Component parsed = ColorUtils.parse(formatted);
        assertEquals("A", parsed.getString());
        assertEquals(formatted, ColorUtils.toFormattedString(parsed));
    }

    private static void assertColor(String text, int rgb) {
        Component component = ColorUtils.parse(text);
        assertEquals("X", component.getString());
        TextColor color = component.getSiblings().get(0).getStyle().getColor();
        assertNotNull(color);
        assertEquals(rgb, color.getValue());
    }
}
