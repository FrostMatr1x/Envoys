package com.frost.envoys.fabric;

import com.frost.envoys.gui.menu.NPCMerchantMenu;
import com.frost.envoys.platform.BackpackAccess;

import net.minecraft.server.level.ServerPlayer;

public final class FabricBackpacks implements BackpackAccess {
    public static final FabricBackpacks INSTANCE = new FabricBackpacks();

    private FabricBackpacks() {
    }

    @Override
    public void executeTradeAll(ServerPlayer player, NPCMerchantMenu menu, int shopItem) {
    }
}
