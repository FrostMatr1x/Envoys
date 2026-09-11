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
import com.google.gson.Gson;

import net.neoforged.fml.loading.FMLPaths;

public class NPCConfigManager {

    public static final Gson GSON = EntityActionAdapter.GSON;

    private static Path getNpcDir() {

        Path dir = FMLPaths.GAMEDIR.get().resolve("world/envoys/npcs");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to create npc config dir", e);
        }
        return dir;
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

    public static void delete(UUID uuid) {
        NPCInteractManager.unregister(uuid);
        try {
            Files.deleteIfExists(getNpcFile(uuid));
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to delete NPC config {}", uuid, e);
        }
        QuestIndex.invalidate();
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
