package com.frost.envoys.neoforge;

import java.nio.file.Path;

import com.frost.envoys.config.Config;
import com.frost.envoys.platform.AttachmentAccess;
import com.frost.envoys.platform.BackpackAccess;
import com.frost.envoys.platform.ConfigData;
import com.frost.envoys.platform.Platform;

import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class NeoForgePlatform implements Platform {
    private final ConfigData config = new ConfigData();

    public static NeoForgePlatform create() {
        return new NeoForgePlatform();
    }

    private NeoForgePlatform() {
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    @Override
    public Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public ConfigData getConfig() {
        try {
            config.magicNumber = Config.MAGIC_NUMBER.get();
            config.magicNumberIntroduction = Config.MAGIC_NUMBER_INTRODUCTION.get();
            config.luaEnabled = Config.LUA_ENABLED.get();
            config.luaInstructionLimit = Config.LUA_INSTRUCTION_LIMIT.get();
            config.luaAllowCommands = Config.LUA_ALLOW_COMMANDS.get();
        } catch (IllegalStateException ignored) {
        }
        return config;
    }

    @Override
    public AttachmentAccess getAttachments() {
        return NeoForgeAttachments.INSTANCE;
    }

    @Override
    public BackpackAccess getBackpacks() {
        return NeoForgeBackpacks.INSTANCE;
    }

}
