package com.frost.envoys.fabric;

import com.frost.envoys.EnvoysCommon;
import com.frost.envoys.network.Network;
import com.frost.envoys.platform.EnvoysPlatform;

import net.fabricmc.api.ModInitializer;

public final class EnvoysFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricConfig.load();
        EnvoysPlatform.init(FabricPlatform.create());
        Network.init(new FabricNetwork());

        FabricAttachments.register();
        FabricRegistrations.register();
        FabricNetworkRegistry.registerPayloadTypes();
        FabricNetworkRegistry.registerServerReceivers();
        FabricServerEvents.register();

        EnvoysCommon.initialize();
    }
}
