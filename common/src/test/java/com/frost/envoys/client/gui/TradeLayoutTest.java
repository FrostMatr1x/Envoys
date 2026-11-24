package com.frost.envoys.client.gui;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeLayoutTest {

    @Test
    void safeRowsClamps() {
        TradeLayout.ListCfg list = new TradeLayout.ListCfg();
        list.rows = 0;
        assertEquals(1, list.safeRows());
        list.rows = 7;
        assertEquals(7, list.safeRows());
        list.rows = 100;
        assertEquals(32, list.safeRows());
    }

    @Test
    void rowScaleFallsBack() {
        TradeLayout.RowCfg row = new TradeLayout.RowCfg();
        assertEquals(0.9f, row.scaleFor(0));
        assertEquals(0.9f, row.scaleFor(1));
        assertEquals(0.9f, row.scaleFor(2));

        row.in1Scale = 0;
        row.itemScale = 0.5f;
        assertEquals(0.5f, row.scaleFor(0));
    }

    @Test
    void partialJsonKeepsDefaults() {
        TradeLayout layout = new Gson().fromJson("{\"image\":{\"width\":300}}", TradeLayout.class);

        assertNotNull(layout);
        assertEquals(300, layout.image.width);
        assertEquals(166, layout.image.height);
        assertEquals(20, layout.list.rowStep);
        assertEquals(5, layout.row.in1);
        assertEquals(6, layout.scroller.width);
        assertTrue(layout.tradeAll.visible);
        assertTrue(layout.labels.titleVisible);
    }
}
