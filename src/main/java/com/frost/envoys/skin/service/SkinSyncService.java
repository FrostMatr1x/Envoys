package com.frost.envoys.skin.service;

import com.frost.envoys.Envoys;
import com.frost.envoys.skin.model.SkinIndexData;
import com.frost.envoys.skin.model.SkinIndexEntry;
import com.frost.envoys.skin.model.SkinMetaData;
import com.frost.envoys.util.PathManager;
import com.frost.envoys.util.SkinModelUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkinSyncService {

    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(4);

    public record SkinFetchResult(byte[] pngData, String hash, String model, String source, String errorMsg) {
        public boolean isSuccess() { return pngData != null && errorMsg == null; }
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

    public static CompletableFuture<SkinFetchResult> loadSkinAsync(String query, String skinType, File worldDir, boolean isClient) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (query == null || query.isBlank()) {
                    return new SkinFetchResult(null, null, null, null, "Пустой запрос");
                }

                Path cacheDir = isClient ? PathManager.getClientSkinCacheDir() : PathManager.getServerSkinCacheDir(worldDir);
                File indexFile = isClient ? PathManager.getClientIndexFile() : PathManager.getServerIndexFile(worldDir);

                String normalizedQuery = query.trim();
                boolean isUrlType = "URL".equalsIgnoreCase(skinType) || normalizedQuery.startsWith("http://") || normalizedQuery.startsWith("https://");

                if ("URL".equalsIgnoreCase(skinType) && !normalizedQuery.toLowerCase().startsWith("http://") && !normalizedQuery.toLowerCase().startsWith("https://")) {
                    normalizedQuery = "https://" + normalizedQuery;
                }

                String fileKey = sanitizeFileName(normalizedQuery);

                SkinIndexData index = SkinLocalService.readIndex(indexFile);
                Optional<SkinIndexEntry> entryOpt = index.skins.stream()
                        .filter(e -> e.uuid.equalsIgnoreCase(fileKey) || e.path.equalsIgnoreCase(fileKey + ".png"))
                        .findFirst();

                if (entryOpt.isPresent()) {
                    SkinIndexEntry entry = entryOpt.get();
                    Path cachedPng = cacheDir.resolve(entry.path);
                    if (Files.exists(cachedPng)) {
                        byte[] data = Files.readAllBytes(cachedPng);
                        String currentHash = SkinCacheService.calculateSHA256(data);
                        if (currentHash.equalsIgnoreCase(entry.hash)) {
                            return new SkinFetchResult(data, currentHash, entry.model, entry.source, null);
                        }
                    }
                }

                if (isUrlType) {
                    byte[] data = SkinRemoteService.downloadSkinFromUrl(normalizedQuery);
                    String hash = SkinCacheService.calculateSHA256(data);
                    String model = SkinModelUtil.detectModel(data);

                    SkinCacheService.saveToCache(cacheDir, fileKey, data, new SkinMetaData(fileKey, hash, model, "url", System.currentTimeMillis()));

                    SkinIndexEntry newEntry = new SkinIndexEntry(fileKey, fileKey + ".png", model, "url", hash, System.currentTimeMillis());
                    SkinLocalService.addOrUpdateIndex(indexFile, newEntry);

                    return new SkinFetchResult(data, hash, model, "url", null);
                }

                Path searchFolder = isClient ? PathManager.getClientEnvoysDir() : PathManager.getServerSkinDir(worldDir);
                Optional<Path> foundFile = SkinLocalService.findSkinRecursively(searchFolder, normalizedQuery);
                if (foundFile.isPresent()) {
                    byte[] data = Files.readAllBytes(foundFile.get());
                    String hash = SkinCacheService.calculateSHA256(data);
                    String model = SkinModelUtil.detectModel(data);

                    SkinCacheService.saveToCache(cacheDir, fileKey, data, new SkinMetaData(fileKey, hash, model, "local", System.currentTimeMillis()));

                    SkinIndexEntry newEntry = new SkinIndexEntry(fileKey, fileKey + ".png", model, "local", hash, System.currentTimeMillis());
                    SkinLocalService.addOrUpdateIndex(indexFile, newEntry);

                    return new SkinFetchResult(data, hash, model, "local", null);
                }

                if ("NICKNAME".equalsIgnoreCase(skinType)) {
                    var mojangOpt = SkinRemoteService.fetchMojangSkin(normalizedQuery);
                    if (mojangOpt.isPresent()) {
                        byte[] data = SkinRemoteService.downloadSkinFromUrl(mojangOpt.get().textureUrl());
                        String hash = SkinCacheService.calculateSHA256(data);
                        String model = mojangOpt.get().model();

                        SkinCacheService.saveToCache(cacheDir, fileKey, data, new SkinMetaData(fileKey, hash, model, "mojang", System.currentTimeMillis()));

                        SkinIndexEntry newEntry = new SkinIndexEntry(fileKey, fileKey + ".png", model, "mojang", hash, System.currentTimeMillis());
                        SkinLocalService.addOrUpdateIndex(indexFile, newEntry);

                        return new SkinFetchResult(data, hash, model, "mojang", null);
                    }
                }

                return new SkinFetchResult(null, null, null, null, "Скин не найден");

            } catch (IllegalArgumentException e) {
                return new SkinFetchResult(null, null, null, null, e.getMessage());
            } catch (Exception e) {
                Envoys.LOGGER.error("Ошибка при загрузке скина: ", e);
                return new SkinFetchResult(null, null, null, null, "Ошибка сети или загрузки");
            }
        }, IO_EXECUTOR);
    }
}