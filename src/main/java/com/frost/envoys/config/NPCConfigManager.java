package com.frost.envoys.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.frost.envoys.Envoys;
import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.NPCScriptData;
import com.frost.envoys.action.serialization.EntityActionAdapter;
import com.frost.envoys.quest.QuestIndex;
import com.frost.envoys.util.PathManager;
import com.google.gson.Gson;

import net.neoforged.fml.loading.FMLPaths;

public class NPCConfigManager {

    public static final Gson GSON = EntityActionAdapter.GSON;

    private static Path migratedDir = null;

    private static Path getNpcDir() {
        Path dir;
        try {
            // Correct location: <world save>/envoys/npcs (works for singleplayer saves and
            // dedicated servers with a custom level-name). Falls back to the legacy path only
            // when the server is not initialised yet.
            dir = PathManager.getServerNPCsDir();
            migrateLegacyIfNeeded(dir);
        } catch (IllegalStateException e) {
            dir = FMLPaths.GAMEDIR.get().resolve("world/envoys/npcs");
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to create npc config dir", e);
        }
        return dir;
    }

    /**
     * Older builds stored configs in {@code <gameDir>/world/envoys/npcs}, which is wrong for
     * singleplayer (the world lives in {@code saves/<world>}). If the new directory has no
     * configs yet, copy the legacy ones so existing NPCs keep their data.
     */
    private static void migrateLegacyIfNeeded(Path newDir) {
        if (newDir.equals(migratedDir)) {
            return;
        }
        migratedDir = newDir;

        Path legacy = FMLPaths.GAMEDIR.get().resolve("world/envoys/npcs");
        if (newDir.equals(legacy) || !Files.isDirectory(legacy)) {
            return;
        }
        try (Stream<Path> existing = Files.list(newDir)) {
            if (existing.anyMatch(p -> p.getFileName().toString().endsWith(".json"))) {
                return;
            }
        } catch (IOException e) {
            return;
        }
        try (Stream<Path> legacyFiles = Files.list(legacy)) {
            legacyFiles.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            Files.copy(p, newDir.resolve(p.getFileName()),
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            Envoys.LOGGER.error("[Envoys] Failed to migrate NPC config {}", p, e);
                        }
                    });
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to list legacy NPC config dir", e);
        }
    }

    private static Path getNpcFile(UUID uuid) {
        return getNpcDir().resolve(uuid.toString() + ".json");
    }

    public static void save(NPCInteractManager script) {
        NPCScriptData data = NPCScriptData.fromManager(script);
        data.quests = NPCScriptData.sanitizeQuests(data.quests, script);

        Path file = getNpcFile(script.npcUUID);
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to save NPC script {}", script.npcUUID, e);
        }
        QuestIndex.invalidate();
    }

    public static void saveAll() {
        NPCInteractManager.SCRIPTS.values().forEach(NPCConfigManager::save);
        Envoys.LOGGER.info("[Envoys] Saved {} NPC scripts", NPCInteractManager.SCRIPTS.size());
    }

    public static Optional<NPCInteractManager> load(UUID uuid) {
        Path file = getNpcFile(uuid);
        if (!Files.exists(file)) return Optional.empty();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            NPCScriptData data = EntityActionAdapter.GSON.fromJson(reader, NPCScriptData.class);
            if (data == null) {
                return Optional.empty();
            }

            NPCInteractManager script = new NPCInteractManager(UUID.fromString(data.npcUUID));
            if (data.passport != null) {
                script.passport = data.passport;
            }
            data.applyTo(script);

            return Optional.of(script);
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to load NPC script {}", uuid, e);
            return Optional.empty();
        } catch (RuntimeException e) {
            Envoys.LOGGER.error("[Envoys] Skipping unreadable NPC script {}", uuid, e);
            return Optional.empty();
        }
    }

    public static void loadAll() {
        Path dir = getNpcDir();
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> {
                     String fileName = p.getFileName().toString();
                     return fileName.endsWith(".json") && !fileName.endsWith(".trades.json");
                 })
                 .forEach(p -> {
                     String name = p.getFileName().toString().replace(".json", "");
                     try {
                         load(UUID.fromString(name));
                     } catch (RuntimeException e) {
                         Envoys.LOGGER.warn("[Envoys] Skipping invalid NPC config file: {}", p);
                     }
                 });
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to list npc config dir", e);
        }
        QuestIndex.invalidate();
        Envoys.LOGGER.info("[Envoys] Loaded {} NPC scripts", NPCInteractManager.SCRIPTS.size());
    }

    /**
     * Removes the config file only when it is present but cannot be parsed as valid JSON.
     * A missing/unreadable file is never treated as corrupt, so records of NPCs that are
     * simply not loaded right now are preserved.
     */
    public static boolean deleteIfCorrupt(UUID uuid) {
        Path file = getNpcFile(uuid);
        if (!Files.isRegularFile(file)) {
            return false;
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            if (json.isBlank()) {
                return false;
            }
            NPCScriptData data = GSON.fromJson(json, NPCScriptData.class);
            if (data == null) {
                delete(uuid);
                return true;
            }
            return false;
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Could not read NPC config {}", uuid, e);
            return false;
        } catch (RuntimeException e) {
            Envoys.LOGGER.warn("[Envoys] Removing corrupt NPC config {}", uuid, e);
            delete(uuid);
            return true;
        }
    }

    public static void delete(UUID uuid) {
        NPCInteractManager.unregister(uuid);
        try {
            Files.deleteIfExists(getNpcFile(uuid));
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to delete NPC config {}", uuid, e);
        }
        deleteLuaDir(uuid);
        QuestIndex.invalidate();
    }

    /**
     * Removes the per-NPC Lua script directory (&lt;world&gt;/envoys/lua/&lt;uuid&gt;/) that is
     * left behind when an NPC record is deleted. Safe to call when the server world
     * path is unavailable.
     */
    private static void deleteLuaDir(UUID uuid) {
        Path dir;
        try {
            dir = PathManager.getServerLuaDir().resolve(uuid.toString());
        } catch (IllegalStateException e) {
            return;
        }
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    Envoys.LOGGER.warn("[Envoys] Could not delete Lua file {}", p, e);
                }
            });
        } catch (IOException e) {
            Envoys.LOGGER.warn("[Envoys] Failed to remove Lua dir for NPC {}", uuid, e);
        }
    }

    public static List<UUID> getAllSavedUuids() {
        List<UUID> uuids = new ArrayList<>();
        Path dir = getNpcDir();
        if (Files.exists(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(p -> {
                        String fileName = p.getFileName().toString();
                        return fileName.endsWith(".json") && !fileName.endsWith(".trades.json");
                    })
                    .forEach(p -> {
                        String name = p.getFileName().toString().replace(".json", "");
                        try {
                            uuids.add(UUID.fromString(name));
                        } catch (IllegalArgumentException ignored) {}
                    });
            } catch (IOException e) {
                Envoys.LOGGER.error("[Envoys] Failed to list npc config dir", e);
            }
        }
        return uuids;
    }
}
