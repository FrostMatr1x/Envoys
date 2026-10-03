package com.frost.envoys.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Path;

public class ClientPathManager {

    private static String currentServerId = "singleplayer";

    public static void setServerId(String hostAndPort) {
        if (hostAndPort == null || hostAndPort.isBlank()) {
            currentServerId = "singleplayer";
        } else {
            currentServerId = hostAndPort.replaceAll("[^a-zA-Z0-9.-]", "_");
        }
    }

    public static String getServerId() {
        if (Minecraft.getInstance().isLocalServer()) {
            return "singleplayer";
        }
        ServerData data = Minecraft.getInstance().getCurrentServer();
        if (data != null) {
            return data.ip.replaceAll("[^a-zA-Z0-9.-]", "_");
        }
        return currentServerId;
    }

    public static Path getClientEnvoysDir() {
        return FMLPaths.GAMEDIR.get().resolve("envoysCache").resolve(getServerId());
    }

    public static Path getClientSkinCacheDir() {
        Path p = getClientEnvoysDir().resolve("skins").resolve("cache");
        p.toFile().mkdirs();
        return p;
    }

    public static File getClientIndexFile() {
        Path p = getClientEnvoysDir().resolve("skin");
        p.toFile().mkdirs();
        return p.resolve("index.json").toFile();
    }

    public static Path getClientAnimDir() {
        Path p = getClientEnvoysDir().resolve("anim");
        p.toFile().mkdirs();
        return p;
    }

    public static Path getClientLocalAnimDir() {
        Path p = FMLPaths.GAMEDIR.get().resolve("envoys").resolve("anim");
        p.toFile().mkdirs();
        return p;
    }

    public static Path getClientLocalLuaDir() {
        Path p = FMLPaths.GAMEDIR.get().resolve("envoys").resolve("local");
        p.toFile().mkdirs();
        return p;
    }

    public static Path getClientLocalTempDir() {
        Path p = getClientLocalLuaDir().resolve(".temp");
        p.toFile().mkdirs();
        return p;
    }
}