package com.frost.envoys.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Path;

public class PathManager {

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

    public static Path getServerEnvoysDir(File worldDir) {
        return worldDir.toPath().resolve("envoys");
    }

    public static Path getServerSkinDir(File worldDir) {
        Path p = getServerEnvoysDir(worldDir).resolve("skins");
        p.toFile().mkdirs();
        return p;
    }

    public static Path getServerSkinCacheDir(File worldDir) {
        Path p = getServerSkinDir(worldDir).resolve("cache");
        p.toFile().mkdirs();
        return p;
    }

    public static File getServerIndexFile(File worldDir) {
        Path p = getServerEnvoysDir(worldDir).resolve("skin");
        p.toFile().mkdirs();
        return p.resolve("index.json").toFile();
    }

    public static Path getClientAnimDir() {
        Path p = getClientEnvoysDir().resolve("anim");
        p.toFile().mkdirs();
        return p;
    }

    public static Path getServerAnimDir(File worldDir) {
        Path p = getServerEnvoysDir(worldDir).resolve("anim");
        p.toFile().mkdirs();
        return p;
    }

    public static Path getServerNPCsDir(File worldDir) {
        Path p = getServerEnvoysDir(worldDir).resolve("npcs");
        p.toFile().mkdirs();
        return p;
    }
}