package com.frost.envoys.skin.service;

import com.frost.envoys.Envoys;
import com.frost.envoys.util.PathManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class AnimSyncService {

    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(4);

    public record AnimLoadResult(String name, byte[] jsonData, String hash, String errorMsg) {
        public boolean isSuccess() { return jsonData != null && errorMsg == null; }
    }

    public record AnimListEntry(String name, String hash) {
    }

    public static CompletableFuture<AnimLoadResult> loadAnimAsync(String name, File worldDir) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (name == null || name.isBlank()) {
                    return new AnimLoadResult(name, null, null, "Пустое имя анимации");
                }

                Path animDir = PathManager.getServerAnimDir(worldDir);
                if (!Files.isDirectory(animDir)) {
                    return new AnimLoadResult(name, null, null, "Папка анимаций не найдена");
                }

                try (Stream<Path> files = Files.list(animDir)) {
                    for (Path file : files.toList()) {
                        String fileName = file.getFileName().toString();
                        if (!fileName.toLowerCase().endsWith(".json")) continue;

                        try (InputStream in = Files.newInputStream(file)) {
                            byte[] data = in.readAllBytes();

                            // Проверяем все возможные псевдонимы файла для поиска
                            List<String> possibleNames = extractAllNamesFromJson(data, fileName);
                            for (String possibleName : possibleNames) {
                                if (possibleName.equalsIgnoreCase(name)) {
                                    return new AnimLoadResult(name, data, SkinCacheService.calculateSHA256(data), null);
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }

                return new AnimLoadResult(name, null, null, "Анимация не найдена на сервере");
            } catch (Exception e) {
                Envoys.LOGGER.error("[Envoys] Ошибка при загрузке анимации", e);
                return new AnimLoadResult(name, null, null, "Ошибка загрузки анимации");
            }
        }, IO_EXECUTOR);
    }

    public static CompletableFuture<List<AnimListEntry>> listAnimsAsync(File worldDir) {
        return CompletableFuture.supplyAsync(() -> {
            List<AnimListEntry> result = new ArrayList<>();
            try {
                Path animDir = PathManager.getServerAnimDir(worldDir);
                if (!Files.isDirectory(animDir)) return result;

                try (Stream<Path> files = Files.list(animDir)) {
                    for (Path file : files.toList()) {
                        String fileName = file.getFileName().toString();
                        if (!fileName.toLowerCase().endsWith(".json")) continue;

                        try (InputStream in = Files.newInputStream(file)) {
                            byte[] data = in.readAllBytes();
                            String hash = SkinCacheService.calculateSHA256(data);

                            // Берём ровно ОДНО главное читаемое имя для отображения в GUI
                            String primaryName = extractPrimaryNameFromJson(data, fileName);
                            result.add(new AnimListEntry(primaryName, hash));
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception e) {
                Envoys.LOGGER.error("[Envoys] Ошибка при сканировании анимаций", e);
            }
            return result;
        }, IO_EXECUTOR);
    }

    public static String extractPrimaryNameFromJson(byte[] data, String fileName) {
        String fileWithoutExt = fileName.replaceAll("(?i)\\.json$", "");

        try {
            JsonElement rootElement = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
            if (rootElement.isJsonObject()) {
                JsonObject json = rootElement.getAsJsonObject();

                // 1. Приоритет: Поле "name" в корне (Emotecraft)
                if (json.has("name")) {
                    List<String> extracted = new ArrayList<>();
                    extractNamesFromElement(json.get("name"), extracted);
                    if (!extracted.isEmpty()) return extracted.get(0);
                }

                // 2. Имя из "player_animation_library" -> "name" (GeckoLib / Blockbench)
                if (json.has("animations") && json.get("animations").isJsonObject()) {
                    JsonObject animsObj = json.getAsJsonObject("animations");
                    for (Map.Entry<String, JsonElement> entry : animsObj.entrySet()) {
                        if (entry.getValue().isJsonObject()) {
                            JsonObject animObj = entry.getValue().getAsJsonObject();
                            if (animObj.has("player_animation_library") && animObj.get("player_animation_library").isJsonObject()) {
                                JsonObject palObj = animObj.getAsJsonObject("player_animation_library");
                                if (palObj.has("name")) {
                                    List<String> extracted = new ArrayList<>();
                                    extractNamesFromElement(palObj.get("name"), extracted);
                                    if (!extracted.isEmpty()) return extracted.get(0);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return fileWithoutExt;
    }

    public static List<String> extractAllNamesFromJson(byte[] data, String fileName) {
        Set<String> names = new LinkedHashSet<>();
        String fileWithoutExt = fileName.replaceAll("(?i)\\.json$", "");

        try {
            JsonElement rootElement = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
            if (rootElement.isJsonObject()) {
                JsonObject json = rootElement.getAsJsonObject();

                if (json.has("name")) {
                    extractNamesFromElement(json.get("name"), names);
                }

                if (json.has("animations") && json.get("animations").isJsonObject()) {
                    JsonObject animsObj = json.getAsJsonObject("animations");
                    for (Map.Entry<String, JsonElement> entry : animsObj.entrySet()) {
                        String animKey = entry.getKey();
                        if (!animKey.isBlank()) {
                            names.add(animKey);
                        }
                        if (entry.getValue().isJsonObject()) {
                            JsonObject animObj = entry.getValue().getAsJsonObject();
                            if (animObj.has("player_animation_library") && animObj.get("player_animation_library").isJsonObject()) {
                                JsonObject palObj = animObj.getAsJsonObject("player_animation_library");
                                if (palObj.has("name")) {
                                    extractNamesFromElement(palObj.get("name"), names);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (!fileWithoutExt.isBlank()) {
            names.add(fileWithoutExt);
        }

        return new ArrayList<>(names);
    }

    private static void extractNamesFromElement(JsonElement nameElem, Iterable<String> namesCollector) {
        if (nameElem == null || nameElem.isJsonNull()) return;

        if (nameElem.isJsonPrimitive()) {
            String str = nameElem.getAsString();
            if (!str.isBlank()) {
                if (str.startsWith("{") && str.endsWith("}")) {
                    try {
                        JsonElement parsed = JsonParser.parseString(str);
                        extractNamesFromElement(parsed, namesCollector);
                        return;
                    } catch (Exception ignored) {}
                }
                addNameToCollector(namesCollector, str);
            }
        } else if (nameElem.isJsonObject()) {
            JsonObject obj = nameElem.getAsJsonObject();
            if (obj.has("text")) {
                String text = obj.get("text").getAsString();
                if (!text.isBlank()) addNameToCollector(namesCollector, text);
            }
            if (obj.has("fallback")) {
                String fallback = obj.get("fallback").getAsString();
                if (!fallback.isBlank()) addNameToCollector(namesCollector, fallback);
            }
            if (obj.has("translate")) {
                String translate = obj.get("translate").getAsString();
                if (!translate.isBlank()) addNameToCollector(namesCollector, translate);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void addNameToCollector(Iterable<String> collector, String name) {
        if (collector instanceof Set set) {
            set.add(name);
        } else if (collector instanceof List list) {
            list.add(name);
        }
    }
}