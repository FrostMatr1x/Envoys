package com.frost.envoys.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathManager {

    private static Path serverWorldDir = null;

    public static void initServer(MinecraftServer server) {
        serverWorldDir = server.getWorldPath(LevelResource.ROOT);
    }

    public static void clearServer() {
        serverWorldDir = null;
    }

    public static Path getServerWorldDir() {
        if (serverWorldDir == null) {
            throw new IllegalStateException("Server world directory has not been initialized yet!");
        }
        return serverWorldDir;
    }

    public static Path getServerEnvoysDir() {
        return getServerWorldDir().resolve("envoys");
    }

    public static Path getServerSkinDir() {
        Path p = getServerEnvoysDir().resolve("skins");
        createDirectories(p);
        return p;
    }

    public static Path getServerSkinCacheDir() {
        Path p = getServerSkinDir().resolve("cache");
        createDirectories(p);
        return p;
    }

    public static File getServerIndexFile() {
        Path p = getServerEnvoysDir().resolve("skin");
        createDirectories(p);
        return p.resolve("index.json").toFile();
    }

    public static Path getServerAnimDir() {
        Path p = getServerEnvoysDir().resolve("anim");
        createDirectories(p);
        return p;
    }

    public static Path getServerNPCsDir() {
        Path p = getServerEnvoysDir().resolve("npcs");
        createDirectories(p);
        return p;
    }

    public static Path getServerLuaDir() {
        Path p = getServerEnvoysDir().resolve("lua");
        createDirectories(p);
        return p;
    }

    private static void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}