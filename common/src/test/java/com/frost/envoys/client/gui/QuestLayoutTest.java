package com.frost.envoys.client.gui;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QuestLayoutTest {

    @Test
    void panelResolvesPosition() {
        QuestLayout.Panel panel = new QuestLayout.Panel();

        panel.xMode = "FIXED";
        panel.x = 10;
        assertEquals(10, panel.resolveX(500, 260));

        panel.xMode = "CENTER";
        assertEquals(120, panel.resolveX(500, 260));

        panel.xMode = "SCREEN_PERCENT";
        assertEquals(120, panel.resolveX(500, 260));

        panel.yMode = "FIXED";
        panel.y = 10;
        assertEquals(10, panel.resolveY(500, 220));

        panel.yMode = "CENTER";
        assertEquals(140, panel.resolveY(500, 220));
    }

    @Test
    void panelResolvesSize() {
        QuestLayout.Panel panel = new QuestLayout.Panel();

        panel.widthMode = "FIXED";
        panel.widthValue = 260;
        assertEquals(260, panel.resolveWidth(500));

        panel.widthMode = "SCREEN_PERCENT";
        panel.widthValue = 0.5f;
        assertEquals(250, panel.resolveWidth(500));

        panel.widthMode = "SCREEN_MINUS";
        panel.widthValue = 260;
        assertEquals(260, panel.resolveWidth(500));

        panel.heightMode = "FIXED";
        panel.heightValue = 220;
        assertEquals(220, panel.resolveHeight(500));

        panel.heightMode = "SCREEN_PERCENT";
        panel.heightValue = 0.5f;
        assertEquals(250, panel.resolveHeight(500));

        panel.heightMode = "SCREEN_MINUS";
        panel.heightValue = 220;
        assertEquals(220, panel.resolveHeight(500));
    }

    @Test
    void partialJsonKeepsDefaults() {
        QuestLayout layout = new Gson().fromJson(
                "{\"panel\":{\"xMode\":\"FIXED\",\"x\":5}}", QuestLayout.class);

        assertNotNull(layout);
        assertEquals("FIXED", layout.panel.xMode);
        assertEquals(5, layout.panel.x);
        assertEquals("CENTER", layout.panel.yMode);
        assertEquals(260f, layout.panel.widthValue);
        assertEquals(30, layout.list.rowHeight);
        assertNotNull(layout.list.scroller);
    }
}
