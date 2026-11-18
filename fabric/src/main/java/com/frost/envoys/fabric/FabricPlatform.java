package com.frost.envoys.fabric;

import java.nio.file.Path;

import com.frost.envoys.platform.AttachmentAccess;
import com.frost.envoys.platform.BackpackAccess;
import com.frost.envoys.platform.ConfigData;
import com.frost.envoys.platform.Platform;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

public final class FabricPlatform implements Platform {
    private static volatile MinecraftServer server;

    public static FabricPlatform create() {
        return new FabricPlatform();
    }

    private FabricPlatform() {
    }

    public static void setServer(MinecraftServer server) {
        FabricPlatform.server = server;
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public MinecraftServer getServer() {
        return server;
    }

    @Override
    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public ConfigData getConfig() {
        return FabricConfig.data();
    }

    @Override
    public AttachmentAccess getAttachments() {
        return FabricAttachments.INSTANCE;
    }

    @Override
    public BackpackAccess getBackpacks() {
        return FabricBackpacks.INSTANCE;
    }

}
