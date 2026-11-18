package com.frost.envoys.platform;

import java.nio.file.Path;

import net.minecraft.server.MinecraftServer;

public interface Platform {
    boolean isClient();

    boolean isModLoaded(String modId);

    MinecraftServer getServer();

    Path getGameDir();

    Path getConfigDir();

    ConfigData getConfig();

    AttachmentAccess getAttachments();

    BackpackAccess getBackpacks();

}
