package com.frost.envoys;

import com.frost.envoys.client.gui.DialogLayout;
import com.frost.envoys.client.gui.QuestLayout;
import com.frost.envoys.client.gui.TradeLayout;
import com.frost.envoys.client.gui.script.ActionGraph;
import com.frost.envoys.client.gui.script.EventUiState;
import com.frost.envoys.client.gui.script.ScenarioCompileException;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.lua.LuaEvent;
import com.frost.envoys.quest.QuestProgress;
import com.frost.envoys.quest.QuestType;
import com.frost.envoys.util.ColorUtils;
import com.google.gson.Gson;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.LuaString;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SmokeTest {

    @Test
    void noBootstrapSmoke() throws Exception {
        assertEquals("x", Component.literal("x").getString());
        assertNotNull(Component.translatable("envoys.test").getContents());
        assertEquals("X", ColorUtils.parse("&aX").getString());
        assertNotNull(new ScenarioCompileException("envoys.compile.test").component());
        assertNotNull(new Gson().fromJson("{}", DialogLayout.class));
        assertNotNull(new Gson().fromJson("{}", QuestLayout.class));
        assertNotNull(new Gson().fromJson("{}", TradeLayout.class));

        var encoded = QuestProgress.CODEC.encodeStart(JsonOps.INSTANCE, new QuestProgress()).getOrThrow();
        assertNotNull(QuestProgress.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        assertNotNull(new ActionGraph("click"));
    }

    @Test
    void enumContracts() {
        assertArrayEquals(new QuestType[]{QuestType.ITEM, QuestType.BOOLEAN, QuestType.KILL}, QuestType.values());
        assertArrayEquals(new EventUiState[]{
                EventUiState.GREEN_SYNCED,
                EventUiState.YELLOW_MODIFIED,
                EventUiState.GREY_LOCKED_CUSTOM
        }, EventUiState.values());

        LuaString player = LuaString.valueOf("player");
        LuaEvent event = new LuaEvent(EventType.CLICK, player);
        assertSame(player, event.player);
        assertEquals(EventType.CLICK, event.type);
    }
}
