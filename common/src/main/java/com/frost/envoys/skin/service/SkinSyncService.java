package com.frost.envoys.skin.service;

import com.frost.envoys.network.Network;

import com.frost.envoys.EnvoysConstants;
import com.frost.envoys.skin.model.SkinIndexData;
import com.frost.envoys.skin.model.SkinIndexEntry;
import com.frost.envoys.skin.model.SkinMetaData;
import com.frost.envoys.util.ClientPathManager;
import com.frost.envoys.util.PathManager;
import com.frost.envoys.util.SkinModelUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class SkinSyncService {

    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread thread = new Thread(r, "Envoys-Skin-IO");
        thread.setDaemon(true);
        return thread;
    });

    private static final long MAX_SKIN_BYTES = 2L * 1024 * 1024;

    public record SkinFetchResult(byte[] pngData, String hash, String model, String source, String errorMsg) {
        public boolean isSuccess() { return pngData != null && errorMsg == null; }
    }

    record Storage(Path cacheDir, File indexFile, Path searchDir) {
    }

    interface RemoteGateway {
        byte[] download(String url) throws Exception;

        Optional<SkinRemoteService.MojangSkinResult> fetchMojang(String username);
    }

    private static final RemoteGateway DEFAULT_GATEWAY = new RemoteGateway() {
        @Override
        public byte[] download(String url) throws Exception {
            return SkinRemoteService.downloadSkinFromUrl(url);
        }

        @Override
        public Optional<SkinRemoteService.MojangSkinResult> fetchMojang(String username) {
            return SkinRemoteService.fetchMojangSkin(username);
        }
    };

    static Storage resolveStorage(boolean isClient, String serverId) {
        if (isClient) {
            return new Storage(
                    ClientPathManager.getClientSkinCacheDir(serverId),
                    ClientPathManager.getClientIndexFile(serverId),
                    ClientPathManager.getClientEnvoysDir(serverId));
        }
        return new Storage(
                PathManager.getServerSkinCacheDir(),
                PathManager.getServerIndexFile(),
                PathManager.getServerSkinDir());
    }

    public static String resolveFileKey(String query, String skinType) {
        if (query == null || query.isBlank()) return "unknown";

        String normalized = query.trim();
        if ("URL".equalsIgnoreCase(skinType)
                && !normalized.toLowerCase().startsWith("http://")
                && !normalized.toLowerCase().startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        return sanitizeFileName(normalized);
    }

    public static String sanitizeFileName(String input) {
        if (input == null || input.isBlank()) return "unknown";

        if (input.startsWith("http://") || input.startsWith("https://")) {
            return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8)).toString();
        }

        String name = input;

        if (name.toLowerCase().endsWith(".png")) {
            name = name.substring(0, name.length() - 4);
        }

        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static CompletableFuture<SkinFetchResult> loadSkinAsync(String query, String skinType, boolean isClient) {
        return loadSkinAsync(query, skinType,
                () -> isClient
                        ? resolveStorage(true, ClientPathManager.getServerId())
                        : resolveStorage(false, null),
                DEFAULT_GATEWAY);
    }

    static CompletableFuture<SkinFetchResult> loadSkinAsync(String query, String skinType,
            Storage storage, RemoteGateway gateway) {
        return loadSkinAsync(query, skinType, () -> storage, gateway);
    }

    private static CompletableFuture<SkinFetchResult> loadSkinAsync(String query, String skinType,
            Supplier<Storage> storageSupplier, RemoteGateway gateway) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (query == null || query.isBlank()) {
                    EnvoysConstants.LOGGER.warn("Skin request with empty value (type={})", skinType);
                    return new SkinFetchResult(null, null, null, null, "Empty request");
                }

                Storage storage = storageSupplier.get();
                Path cacheDir = storage.cacheDir();
                File indexFile = storage.indexFile();

                String normalizedQuery = query.trim();
                boolean isUrlType = "URL".equalsIgnoreCase(skinType) || normalizedQuery.startsWith("http://") || normalizedQuery.startsWith("https://");

                if ("URL".equalsIgnoreCase(skinType) && !normalizedQuery.toLowerCase().startsWith("http://") && !normalizedQuery.toLowerCase().startsWith("https://")) {
                    normalizedQuery = "https://" + normalizedQuery;
                }

                String fileKey = resolveFileKey(normalizedQuery, skinType);

                SkinIndexData index = SkinLocalService.readIndex(indexFile);
                Optional<SkinIndexEntry> entryOpt = index == null ? Optional.empty() : index.skins.stream()
                        .filter(e -> e != null
                                && e.uuid != null
                                && e.hash != null
                                && SkinLocalService.isSafeSkinFileName(e.path))
                        .filter(e -> e.uuid.equalsIgnoreCase(fileKey) || e.path.equalsIgnoreCase(fileKey + ".png"))
                        .findFirst();

                if (entryOpt.isPresent()) {
                    SkinIndexEntry entry = entryOpt.get();
                    Path cachedPng = cacheDir.resolve(entry.path).normalize();
                    byte[] data = readSkinFile(cachedPng, cacheDir, false);
                    if (data != null) {
                        String currentHash = SkinCacheService.calculateSHA256(data);
                        if (currentHash.equalsIgnoreCase(entry.hash)) {
                            return new SkinFetchResult(data, currentHash, entry.model, entry.source, null);
                        }
                    }
                }

                if (isUrlType) {
                    byte[] data = gateway.download(normalizedQuery);
                    String hash = SkinCacheService.calculateSHA256(data);
                    String model = SkinModelUtil.detectModel(data);

                    SkinCacheService.saveToCache(cacheDir, fileKey, data, new SkinMetaData(fileKey, hash, model, "url", System.currentTimeMillis()));

                    SkinIndexEntry newEntry = new SkinIndexEntry(fileKey, fileKey + ".png", model, "url", hash, System.currentTimeMillis());
                    if (!SkinLocalService.addOrUpdateIndex(indexFile, newEntry)) {
                        EnvoysConstants.LOGGER.warn("Skin index was not updated for '{}' (index unreadable)", fileKey);
                    }

                    return new SkinFetchResult(data, hash, model, "url", null);
                }

                Path searchFolder = storage.searchDir();
                Optional<Path> foundFile = SkinLocalService.findSkinRecursively(searchFolder, normalizedQuery);
                if (foundFile.isPresent()) {
                    byte[] data = readSkinFile(foundFile.get(), searchFolder, true);
                    if (data == null) {
                        EnvoysConstants.LOGGER.warn("Refusing to read skin file outside {}: {}", searchFolder, foundFile.get());
                    } else {
                        String hash = SkinCacheService.calculateSHA256(data);
                        String model = SkinModelUtil.detectModel(data);

                        SkinCacheService.saveToCache(cacheDir, fileKey, data, new SkinMetaData(fileKey, hash, model, "local", System.currentTimeMillis()));

                        SkinIndexEntry newEntry = new SkinIndexEntry(fileKey, fileKey + ".png", model, "local", hash, System.currentTimeMillis());
                        if (!SkinLocalService.addOrUpdateIndex(indexFile, newEntry)) {
                            EnvoysConstants.LOGGER.warn("Skin index was not updated for '{}' (index unreadable)", fileKey);
                        }

                        return new SkinFetchResult(data, hash, model, "local", null);
                    }
                }

                if ("NICKNAME".equalsIgnoreCase(skinType)) {
                    var mojangOpt = gateway.fetchMojang(normalizedQuery);
                    if (mojangOpt.isPresent()) {
                        byte[] data = gateway.download(mojangOpt.get().textureUrl());
                        String hash = SkinCacheService.calculateSHA256(data);
                        String model = mojangOpt.get().model();

                        SkinCacheService.saveToCache(cacheDir, fileKey, data, new SkinMetaData(fileKey, hash, model, "mojang", System.currentTimeMillis()));

                        SkinIndexEntry newEntry = new SkinIndexEntry(fileKey, fileKey + ".png", model, "mojang", hash, System.currentTimeMillis());
                        if (!SkinLocalService.addOrUpdateIndex(indexFile, newEntry)) {
                            EnvoysConstants.LOGGER.warn("Skin index was not updated for '{}' (index unreadable)", fileKey);
                        }

                        return new SkinFetchResult(data, hash, model, "mojang", null);
                    }
                }

                EnvoysConstants.LOGGER.warn("Skin '{}' (type={}) was not found", normalizedQuery, skinType);
                return new SkinFetchResult(null, null, null, null, "Skin not found");

            } catch (IllegalArgumentException e) {
                EnvoysConstants.LOGGER.warn("Invalid skin '{}' (type={}): {}", query, skinType, e.getMessage());
                return new SkinFetchResult(null, null, null, null, e.getMessage());
            } catch (Exception e) {
                EnvoysConstants.LOGGER.error("Failed to load skin '{}' (type={})", query, skinType, e);
                return new SkinFetchResult(null, null, null, null, "Network or download error");
            }
        }, IO_EXECUTOR);
    }

    private static byte[] readSkinFile(Path file, Path root, boolean allowSymlink) {
        try {
            Path realRoot = root.toRealPath();
            boolean regular = allowSymlink
                    ? Files.isRegularFile(file)
                    : Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS);
            if (!regular) {
                return null;
            }
            Path realFile = file.toRealPath();
            if (!realFile.startsWith(realRoot)) {
                return null;
            }
            long size = Files.size(realFile);
            if (size <= 0 || size > MAX_SKIN_BYTES) {
                return null;
            }
            return Files.readAllBytes(realFile);
        } catch (IOException e) {
            return null;
        }
    }
}
