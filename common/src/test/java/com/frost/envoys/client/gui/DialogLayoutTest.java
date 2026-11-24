package com.frost.envoys.client.gui;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DialogLayoutTest {

    @Test
    void parseColorSupportsHexAndFallback() {
        assertEquals(0xFFFFFF00, DialogLayout.parseColor("0xFFFFFF00", 0));
        assertEquals(0x00FF00, DialogLayout.parseColor("#00FF00", 0));
        assertEquals(42, DialogLayout.parseColor("zzz", 42));
        assertEquals(42, DialogLayout.parseColor(null, 42));
        assertEquals(42, DialogLayout.parseColor("   ", 42));
    }

    @Test
    void sizeModeParsing() {
        assertEquals(DialogLayout.SizeMode.FIXED, DialogLayout.SizeMode.parse("fixed", DialogLayout.SizeMode.CENTER));
        assertEquals(DialogLayout.SizeMode.SCREEN_PERCENT,
                DialogLayout.SizeMode.parse("SCREEN_PERCENT", DialogLayout.SizeMode.CENTER));
        assertEquals(DialogLayout.SizeMode.CENTER,
                DialogLayout.SizeMode.parse("bad", DialogLayout.SizeMode.CENTER));
        assertEquals(DialogLayout.SizeMode.CENTER,
                DialogLayout.SizeMode.parse(null, DialogLayout.SizeMode.CENTER));
    }

    @Test
    void frameResolvesWidthModes() {
        DialogLayout.Frame frame = new DialogLayout.Frame();
        frame.widthMode = "FIXED";
        frame.widthValue = 100;
        assertEquals(100, frame.resolveWidth(400, 0));

        frame.widthMode = "SCREEN_MINUS";
        frame.widthValue = 20;
        assertEquals(380, frame.resolveWidth(400, 0));

        frame.widthMode = "SCREEN_PERCENT";
        frame.widthValue = 0.5f;
        assertEquals(200, frame.resolveWidth(400, 0));

        frame.widthMode = "CENTER";
        frame.widthValue = 20;
        assertEquals(380, frame.resolveWidth(400, 0));

        frame.widthMode = "FIXED";
        frame.widthValue = 0;
        assertEquals(1, frame.resolveWidth(400, 0));
    }

    @Test
    void frameResolvesHeightModes() {
        DialogLayout.Frame frame = new DialogLayout.Frame();
        frame.heightMode = "FIXED";
        frame.heightValue = 100;
        assertEquals(100, frame.resolveHeight(400, 0));

        frame.heightMode = "SCREEN_MINUS";
        frame.heightValue = 20;
        assertEquals(380, frame.resolveHeight(400, 0));

        frame.heightMode = "SCREEN_PERCENT";
        frame.heightValue = 0.5f;
        assertEquals(200, frame.resolveHeight(400, 0));

        frame.heightMode = "CENTER";
        assertEquals(200, frame.resolveHeight(400, 0));
    }

    @Test
    void optionsResolveWidthModes() {
        DialogLayout.OptionsCfg options = new DialogLayout.OptionsCfg();
        options.widthMode = "FIXED";
        options.widthValue = 100;
        assertEquals(100, options.resolveWidth(400));

        options.widthMode = "SCREEN_MINUS";
        options.widthValue = 20;
        assertEquals(380, options.resolveWidth(400));

        options.widthMode = "SCREEN_PERCENT";
        options.widthValue = 0.5f;
        assertEquals(200, options.resolveWidth(400));

        options.widthMode = "CENTER";
        assertEquals(200, options.resolveWidth(400));
    }

    @Test
    void partialJsonKeepsDefaults() {
        DialogLayout layout = new Gson().fromJson(
                "{\"frame\":{\"widthMode\":\"FIXED\",\"widthValue\":200}}", DialogLayout.class);

        assertNotNull(layout);
        assertEquals(200f, layout.frame.widthValue);
        assertEquals("SCREEN_PERCENT", layout.frame.heightMode);
        assertEquals(0.4f, layout.frame.heightValue);
        assertNotNull(layout.name);
        assertNotNull(layout.text);
        assertNotNull(layout.options);
    }

    @Test
    void safeScaleAndColorDefaults() {
        DialogLayout.NameCfg name = new DialogLayout.NameCfg();
        name.scale = 0;
        assertEquals(0.1f, name.safeScale());
        assertEquals(0xFFFFFF00, name.colorOrDefault());

        DialogLayout.TextCfg text = new DialogLayout.TextCfg();
        text.scale = -1;
        assertEquals(0.1f, text.safeScale());
        assertEquals(0xFFFFFFFF, text.colorOrDefault());

        DialogLayout.OptionsCfg options = new DialogLayout.OptionsCfg();
        options.scale = 0;
        assertEquals(0.1f, options.safeScale());
    }
}
