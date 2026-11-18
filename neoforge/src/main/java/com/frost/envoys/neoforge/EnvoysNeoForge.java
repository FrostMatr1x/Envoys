package com.frost.envoys.neoforge;

import com.frost.envoys.EnvoysCommon;
import com.frost.envoys.EnvoysConstants;
import com.frost.envoys.config.Config;
import com.frost.envoys.network.Network;
import com.frost.envoys.platform.EnvoysPlatform;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(EnvoysConstants.MODID)
public final class EnvoysNeoForge {
    public EnvoysNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        EnvoysPlatform.init(NeoForgePlatform.create());
        Network.init(new NeoForgeNetwork());

        NeoForgeRegistrations.register(modEventBus);
        NeoForgeAttachments.register(modEventBus);
        modEventBus.addListener(NeoForgeRegistrations::registerAttributes);
        modEventBus.addListener(NeoForgeRegistrations::addCreative);
        modEventBus.addListener(NeoForgeNetworkEvents::register);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        if (FMLEnvironment.dist.isClient()) {
            modEventBus.register(NeoForgeClientEvents.ModBusEvents.class);
            NeoForge.EVENT_BUS.register(NeoForgeClientEvents.GameBusEvents.class);
        }

        NeoForge.EVENT_BUS.register(NeoForgeServerEvents.class);
        NeoForge.EVENT_BUS.register(NeoForgeQuestKillTracker.class);

        EnvoysCommon.initialize();
    }
}
