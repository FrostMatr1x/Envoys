package com.frost.envoys.client.anim;

import com.frost.envoys.Envoys;
import com.frost.envoys.client.EmoteIntegration;
import com.frost.envoys.skin.service.AnimSyncService;
import com.frost.envoys.skin.service.SkinCacheService;
import com.frost.envoys.util.ClientPathManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Клиентское хранилище локальных анимаций из {@code envoys/anim/}.
 * Поддерживает рекурсивный поиск .json, кэширует SHA-256 и размер файла.
 */
public final class ClientAnimStore {

    public static final int MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;

    public record Entry(Path path, String hash, long size, String displayName) {
    }

    public record ScanResult(int loaded, int errors) {
    }

    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
    private static volatile long version = 0L;

    private ClientAnimStore() {
    }

    /** Полный рекурсивный пересбор кэша. Ключ — канонизированное имя анимации. */
    public static synchronized ScanResult scan() {
        CACHE.clear();
        int errors = 0;

        Path dir = ClientPathManager.getClientLocalAnimDir();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted()
                    .forEach(files::add);
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to scan local animations in {}", dir, e);
            return new ScanResult(0, 1);
        }

        for (Path file : files) {
            try {
                long size = Files.size(file);
                if (size > MAX_FILE_SIZE_BYTES) {
                    errors++;
                    Envoys.LOGGER.warn("[Envoys] Local animation '{}' is too large ({} bytes), skipped", file, size);
                    continue;
                }
                byte[] data = Files.readAllBytes(file);
                String hash = SkinCacheService.calculateSHA256(data);
                String primary = AnimSyncService.extractPrimaryNameFromJson(data, file.getFileName().toString());
                String name = EmoteIntegration.sanitizeAnimName(primary);
                // Отображаемое имя хранится "сырым" (как в JSON), т.к. sanitizeAnimName
                // заменяет кириллицу и § на '_'. Иначе локальная анимация и та же
                // анимация, пришедшая с сервера под исходным именем, считались бы
                // разными и попадали в список дважды.
                String displayName = (primary == null || primary.isBlank()) ? name : primary;

                Entry existing = CACHE.get(name);
                if (existing == null) {
                    CACHE.put(name, new Entry(file, hash, size, displayName));
                } else {
                    Envoys.LOGGER.warn("[Envoys] Duplicate local animation name '{}' ({} kept, {} ignored)",
                            name, existing.path(), file);
                }
            } catch (Exception e) {
                errors++;
                Envoys.LOGGER.warn("[Envoys] Failed to read local animation '{}'", file, e);
            }
        }

        version++;
        return new ScanResult(CACHE.size(), errors);
    }

    /** Файловые (безопасные) имена — используются командами push и автодополнением. */
    public static List<String> listNames() {
        List<String> names = new ArrayList<>(CACHE.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** Отображаемые имена (как в JSON/на сервере) — используются GUI выбора эмоций. */
    public static List<String> listDisplayNames() {
        List<String> names = new ArrayList<>(CACHE.size());
        for (Entry entry : CACHE.values()) {
            if (entry.displayName() != null && !entry.displayName().isBlank()) {
                names.add(entry.displayName());
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static boolean isEmpty() {
        return CACHE.isEmpty();
    }

    public static Entry get(String name) {
        if (name == null) return null;
        // Сначала пробуем файловый (sanitized) ключ, затем исходное/отображаемое имя.
        Entry direct = CACHE.get(EmoteIntegration.sanitizeAnimName(name));
        if (direct != null) return direct;
        direct = CACHE.get(name);
        if (direct != null) return direct;
        for (Map.Entry<String, Entry> entry : CACHE.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) return entry.getValue();
            Entry value = entry.getValue();
            if (value.displayName() != null && value.displayName().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static byte[] read(Entry entry) {
        if (entry == null) return null;
        try {
            if (Files.size(entry.path()) > MAX_FILE_SIZE_BYTES) return null;
            return Files.readAllBytes(entry.path());
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to read local animation '{}'", entry.path(), e);
            return null;
        }
    }

    public static void invalidate() {
        CACHE.clear();
        version++;
    }

    public static long version() {
        return version;
    }
}
