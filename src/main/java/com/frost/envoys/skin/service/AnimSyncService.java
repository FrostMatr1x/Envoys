package com.frost.envoys.skin.service;

import com.frost.envoys.Envoys;
import com.frost.envoys.util.PathManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class AnimSyncService {

    private static final int MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024; // 2 МБ
    private static final int MAX_NAME_LENGTH = 128;

    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Envoys-Anim-IO");
        t.setDaemon(true);
        return t;
    });

    public record AnimLoadResult(String name, byte[] jsonData, String hash, String errorMsg) {
        public boolean isSuccess() { return jsonData != null && errorMsg == null; }
    }

    public record AnimListEntry(String name, String hash) {
    }

    // Результат операции записи/удаления на сервере: messageKey — ключ
    // локализации, который клиент отображает игроку в чате.
    public record AnimSaveResult(boolean success, String messageKey) {
    }

    // D1: кэш списка анимаций. Заполняется при старте сервера (и по явной
    // команде обновления через refreshCache), между запросами не пересканируется.
    private static final AtomicReference<CachedData> CACHE = new AtomicReference<>(new CachedData(List.of(), Map.of()));

    private record CachedData(List<AnimListEntry> list, Map<String, List<String>> namesByFile) {
    }

    public static CompletableFuture<AnimLoadResult> loadAnimAsync(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (name == null || name.isBlank()) {
                    return new AnimLoadResult(name, null, null, "Empty animation name");
                }
                if (!isValidAnimName(name)) {
                    return new AnimLoadResult(name, null, null, "Invalid animation name");
                }

                CachedData cached = CACHE.get();

                // Поиск по кэшу: файл, у которого имя из полного набора совпадает с запрошенным.
                for (Map.Entry<String, List<String>> entry : cached.namesByFile().entrySet()) {
                    for (String possibleName : entry.getValue()) {
                        if (possibleName.equalsIgnoreCase(name)) {
                            return loadFileByName(entry.getKey(), name);
                        }
                    }
                }

                // Файл мог быть добавлен после последнего обновления кэша — читаем папку напрямую.
                Path animDir = PathManager.getServerAnimDir();
                if (!Files.isDirectory(animDir)) {
                    return new AnimLoadResult(name, null, null, "Animation folder not found");
                }

                try (Stream<Path> files = Files.list(animDir)) {
                    for (Path file : files.toList()) {
                        String fileName = file.getFileName().toString();
                        if (!fileName.toLowerCase().endsWith(".json")) continue;
                        if (!cached.namesByFile().containsKey(fileName)) {
                            byte[] data = readFileLimited(file);
                            if (data == null) continue;
                            List<String> possibleNames = extractAllNamesFromJson(data, fileName);
                            for (String possibleName : possibleNames) {
                                if (possibleName.equalsIgnoreCase(name)) {
                                    return new AnimLoadResult(name, data, SkinCacheService.calculateSHA256(data), null);
                                }
                            }
                        }
                    }
                }

                return new AnimLoadResult(name, null, null, "Animation not found on the server");
            } catch (Exception e) {
                Envoys.LOGGER.error("[Envoys] Failed to load animation", e);
                return new AnimLoadResult(name, null, null, "Failed to load animation");
            }
        }, IO_EXECUTOR);
    }

    // D1: возвращает кэшированный список; при пустом кэше выполняет первичный скан.
    public static CompletableFuture<List<AnimListEntry>> listAnimsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            CachedData cached = CACHE.get();
            if (!cached.list().isEmpty()) return cached.list();
            CachedData refreshed = scanAndCache();
            CACHE.set(refreshed);
            return refreshed.list();
        }, IO_EXECUTOR);
    }

    // D1: публичное API для админ-команды обновления кэша.
    // Основной сценарий — вызов при запуске сервера; команда может вызвать повторно.
    public static CompletableFuture<Integer> refreshCache() {
        return CompletableFuture.supplyAsync(AnimSyncService::refreshCacheSync, IO_EXECUTOR);
    }

    private static synchronized int refreshCacheSync() {
        CachedData refreshed = scanAndCache();
        CACHE.set(refreshed);
        Envoys.LOGGER.info("[Envoys] Animation cache refreshed: {} entries", refreshed.list().size());
        return refreshed.list().size();
    }

    // C2S: приём анимации от клиента. Все проверки безопасности выполняются
    // здесь, до любой записи на диск сервера.
    public static CompletableFuture<AnimSaveResult> saveAnimAsync(String rawName, byte[] data, String expectedHash) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String name = normalizeAnimName(rawName);
                if (!isValidAnimName(name)) {
                    return new AnimSaveResult(false, "envoys.cmd.anim.bad_name");
                }
                if (data == null || data.length == 0) {
                    return new AnimSaveResult(false, "envoys.cmd.anim.invalid_json");
                }
                if (data.length > MAX_FILE_SIZE_BYTES) {
                    return new AnimSaveResult(false, "envoys.cmd.anim.too_big");
                }
                try {
                    JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
                } catch (Exception e) {
                    return new AnimSaveResult(false, "envoys.cmd.anim.invalid_json");
                }
                String actualHash = SkinCacheService.calculateSHA256(data);
                if (expectedHash == null || !actualHash.equalsIgnoreCase(expectedHash)) {
                    return new AnimSaveResult(false, "envoys.cmd.anim.hash_mismatch");
                }

                Path animDir = PathManager.getServerAnimDir().normalize();
                Path target = animDir.resolve(name + ".json").normalize();
                if (target.getParent() == null || !target.getParent().equals(animDir)) {
                    return new AnimSaveResult(false, "envoys.cmd.anim.bad_name");
                }

                Path tmp = animDir.resolve(name + ".json.tmp").normalize();
                Files.write(tmp, data);
                try {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
                refreshCacheSync();
                return new AnimSaveResult(true, "envoys.cmd.anim.push_done");
            } catch (Exception e) {
                Envoys.LOGGER.error("[Envoys] Failed to save animation '{}'", rawName, e);
                return new AnimSaveResult(false, "envoys.cmd.anim.push_failed");
            }
        }, IO_EXECUTOR);
    }

    // C2S: удаление анимации с сервера.
    public static CompletableFuture<AnimSaveResult> deleteAnimAsync(String rawName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String name = normalizeAnimName(rawName);
                if (!isValidAnimName(name)) {
                    return new AnimSaveResult(false, "envoys.cmd.anim.bad_name");
                }

                Path animDir = PathManager.getServerAnimDir().normalize();
                String fileName = resolveAnimFileName(name);
                Path target = (fileName != null)
                        ? animDir.resolve(fileName).normalize()
                        : animDir.resolve(name + ".json").normalize();
                if (target.getParent() == null || !target.getParent().equals(animDir)) {
                    return new AnimSaveResult(false, "envoys.cmd.anim.bad_name");
                }
                if (!Files.isRegularFile(target)) {
                    return new AnimSaveResult(false, "envoys.cmd.anim.not_found_server");
                }
                Files.delete(target);
                refreshCacheSync();
                return new AnimSaveResult(true, "envoys.cmd.anim.delete_done");
            } catch (Exception e) {
                Envoys.LOGGER.error("[Envoys] Failed to delete animation '{}'", rawName, e);
                return new AnimSaveResult(false, "envoys.cmd.anim.delete_failed");
            }
        }, IO_EXECUTOR);
    }

    private static String normalizeAnimName(String rawName) {
        if (rawName == null) return "";
        String name = rawName.trim();
        if (name.toLowerCase().endsWith(".json")) {
            name = name.substring(0, name.length() - 5);
        }
        return name;
    }

    // Поиск фактического .json файла по любому алиасу (основное имя, ключ
    // animations, имя из player_animation_library или имя файла). Нужен для
    // delete: клиент присылает отображаемое (основное) имя, которое может не
    // совпадать с именем файла на диске.
    private static String resolveAnimFileName(String name) {
        CachedData cached = CACHE.get();
        for (Map.Entry<String, List<String>> entry : cached.namesByFile().entrySet()) {
            for (String possibleName : entry.getValue()) {
                if (possibleName.equalsIgnoreCase(name)) {
                    return entry.getKey();
                }
            }
        }
        // Файл мог быть добавлен после обновления кэша — смотрим папку напрямую.
        try {
            Path animDir = PathManager.getServerAnimDir();
            if (!Files.isDirectory(animDir)) return null;
            try (Stream<Path> files = Files.list(animDir)) {
                for (Path file : files.toList()) {
                    String fileName = file.getFileName().toString();
                    if (!fileName.toLowerCase().endsWith(".json")) continue;
                    byte[] data = readFileLimited(file);
                    if (data == null) continue;
                    for (String possibleName : extractAllNamesFromJson(data, fileName)) {
                        if (possibleName.equalsIgnoreCase(name)) {
                            return fileName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Envoys.LOGGER.debug("[Envoys] Failed to resolve animation file for '{}'", name, e);
        }
        return null;
    }

    private static CachedData scanAndCache() {
        List<AnimListEntry> list = new ArrayList<>();
        Map<String, List<String>> namesByFile = new LinkedHashMap<>();
        try {
            Path animDir = PathManager.getServerAnimDir();
            if (!Files.isDirectory(animDir)) return new CachedData(list, namesByFile);

            try (Stream<Path> files = Files.list(animDir)) {
                for (Path file : files.toList()) {
                    String fileName = file.getFileName().toString();
                    if (!fileName.toLowerCase().endsWith(".json")) continue;

                    try (InputStream in = Files.newInputStream(file)) {
                        byte[] data = in.readAllBytes();
                        String hash = SkinCacheService.calculateSHA256(data);

                        List<String> allNames = extractAllNamesFromJson(data, fileName);
                        namesByFile.put(fileName, allNames);

                        // A1/A2: все алиасы файла сохраняются в namesByFile, чтобы
                        // запрос по любому имени находился при загрузке, но в
                        // визуальный список попадает только одно основное имя —
                        // иначе одна анимация отображалась бы несколько раз.
                        String primary = extractPrimaryNameFromJson(data, fileName);
                        list.add(new AnimListEntry(primary, hash));
                    } catch (Exception e) {
                        Envoys.LOGGER.warn("[Envoys] Failed to read animation file '{}'", fileName, e);
                    }
                }
            }
        } catch (Exception e) {
            Envoys.LOGGER.error("[Envoys] Failed to scan animations", e);
        }
        return new CachedData(list, namesByFile);
    }

    private static AnimLoadResult loadFileByName(String fileName, String requestedName) {
        try {
            Path animDir = PathManager.getServerAnimDir();
            Path file = animDir.resolve(fileName);
            if (!Files.isRegularFile(file)) {
                return new AnimLoadResult(requestedName, null, null, "Animation not found on the server");
            }
            byte[] data = readFileLimited(file);
            if (data == null) {
                return new AnimLoadResult(requestedName, null, null, "Animation file too large or unreadable");
            }
            return new AnimLoadResult(requestedName, data, SkinCacheService.calculateSHA256(data), null);
        } catch (Exception e) {
            Envoys.LOGGER.error("[Envoys] Failed to load animation file '{}'", fileName, e);
            return new AnimLoadResult(requestedName, null, null, "Failed to load animation");
        }
    }

    private static byte[] readFileLimited(Path file) throws java.io.IOException {
        if (Files.size(file) > MAX_FILE_SIZE_BYTES) {
            Envoys.LOGGER.warn("[Envoys] Animation file '{}' exceeds size limit ({} bytes), skipping", file.getFileName(), MAX_FILE_SIZE_BYTES);
            return null;
        }
        return Files.readAllBytes(file);
    }

    public static boolean isValidAnimName(String name) {
        if (name == null || name.isBlank()) return false;
        if (name.length() > MAX_NAME_LENGTH) return false;
        if (name.contains("..") || name.contains("/") || name.contains("\\")) return false;
        for (int i = 0; i < name.length(); i++) {
            if (Character.isISOControl(name.charAt(i))) return false;
        }
        return true;
    }

    public static String extractPrimaryNameFromJson(byte[] data, String fileName) {
        String fileWithoutExt = fileName.replaceAll("(?i)\\.json$", "");

        try {
            JsonElement rootElement = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
            if (rootElement.isJsonObject()) {
                JsonObject json = rootElement.getAsJsonObject();

                if (json.has("name")) {
                    List<String> extracted = new ArrayList<>();
                    extractNamesFromElement(json.get("name"), extracted);
                    if (!extracted.isEmpty()) return extracted.get(0);
                }

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
        } catch (Exception e) {
            Envoys.LOGGER.debug("[Envoys] Failed to extract primary name from '{}'", fileName, e);
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
        } catch (Exception e) {
            Envoys.LOGGER.debug("[Envoys] Failed to extract names from '{}'", fileName, e);
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
