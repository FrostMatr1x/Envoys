package com.frost.envoys.neoforge;

import com.frost.envoys.gui.menu.NPCMerchantMenu;
import com.frost.envoys.platform.BackpackAccess;
import com.frost.envoys.platform.EnvoysPlatform;

import net.minecraft.server.level.ServerPlayer;

public final class NeoForgeBackpacks implements BackpackAccess {
    public static final NeoForgeBackpacks INSTANCE = new NeoForgeBackpacks();

    private NeoForgeBackpacks() {
    }

    @Override
    public void executeTradeAll(ServerPlayer player, NPCMerchantMenu menu, int shopItem) {
        if (EnvoysPlatform.isModLoaded("sophisticatedbackpacks")) {
            TradeAllExecutor.execute(player, menu, shopItem);
        }
    }
}
