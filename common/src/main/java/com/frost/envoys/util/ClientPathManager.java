package com.frost.envoys.util;

import com.frost.envoys.platform.EnvoysPlatform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.File;
import java.nio.file.Path;

public class ClientPathManager {

    private static final String SINGLEPLAYER_ID = "singleplayer";

    private static String currentServerId = SINGLEPLAYER_ID;

    public static void setServerId(String hostAndPort) {
        currentServerId = sanitizeServerId(hostAndPort);
    }

    public static String getServerId() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean localServer = minecraft.isLocalServer();
        ServerData data = localServer ? null : minecraft.getCurrentServer();
        return resolveServerId(localServer, data == null ? null : data.ip, currentServerId);
    }

    static String sanitizeServerId(String hostAndPort) {
        if (hostAndPort == null || hostAndPort.isBlank()) {
            return SINGLEPLAYER_ID;
        }
        return sanitizeHost(hostAndPort);
    }

    static String resolveServerId(boolean localServer, String currentServerIp, String fallback) {
        if (localServer) {
            return SINGLEPLAYER_ID;
        }
        if (currentServerIp != null) {
            return sanitizeHost(currentServerIp);
        }
        return fallback;
    }

    private static String sanitizeHost(String value) {
        return value.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    public static Path getClientEnvoysDir() {
        return getClientEnvoysDir(getServerId());
    }

    public static Path getClientEnvoysDir(String serverId) {
        return EnvoysPlatform.getGameDir().resolve("envoysCache").resolve(serverId);
    }

    public static Path getClientSkinCacheDir() {
        return getClientSkinCacheDir(getServerId());
    }

    public static Path getClientSkinCacheDir(String serverId) {
        Path p = getClientEnvoysDir(serverId).resolve("skins").resolve("cache");
        p.toFile().mkdirs();
        return p;
    }

    public static File getClientIndexFile() {
        return getClientIndexFile(getServerId());
    }

    public static File getClientIndexFile(String serverId) {
        Path p = getClientEnvoysDir(serverId).resolve("skin");
        p.toFile().mkdirs();
        return p.resolve("index.json").toFile();
    }

    public static Path getClientAnimDir() {
        return getClientAnimDir(getServerId());
    }

    public static Path getClientAnimDir(String serverId) {
        Path p = getClientEnvoysDir(serverId).resolve("anim");
        p.toFile().mkdirs();
        return p;
    }

    public static Path getClientLocalAnimDir() {
        Path p = EnvoysPlatform.getGameDir().resolve("envoys").resolve("anim");
        p.toFile().mkdirs();
        return p;
    }

    public static Path getClientLocalLuaDir() {
        Path p = EnvoysPlatform.getGameDir().resolve("envoys").resolve("local");
        p.toFile().mkdirs();
        return p;
    }

    public static Path getClientLocalTempDir() {
        Path p = getClientLocalLuaDir().resolve(".temp");
        p.toFile().mkdirs();
        return p;
    }
}
