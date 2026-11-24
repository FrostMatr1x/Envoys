package com.frost.envoys.platform;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public class TestPlatform implements Platform {

    private final Path gameDir;
    private final Path configDir;
    private final ConfigData config = new ConfigData();

    public TestPlatform(Path gameDir, Path configDir) {
        this.gameDir = gameDir;
        this.configDir = configDir;
    }

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return false;
    }

    @Override
    public MinecraftServer getServer() {
        return null;
    }

    @Override
    public Path getGameDir() {
        return gameDir;
    }

    @Override
    public Path getConfigDir() {
        return configDir;
    }

    @Override
    public ConfigData getConfig() {
        return config;
    }

    @Override
    public AttachmentAccess getAttachments() {
        return null;
    }

    @Override
    public BackpackAccess getBackpacks() {
        return null;
    }
}
