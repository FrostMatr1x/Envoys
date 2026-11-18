package com.frost.envoys.platform;

import com.frost.envoys.gui.menu.NPCMerchantMenu;

import net.minecraft.server.level.ServerPlayer;

public interface BackpackAccess {
    void executeTradeAll(ServerPlayer player, NPCMerchantMenu menu, int shopItem);
}
