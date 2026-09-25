package com.frost.envoys.client.gui.backup;

import com.frost.envoys.Envoys;
import com.frost.envoys.client.gui.script.ScenarioCompiler;
import com.frost.envoys.client.gui.script.ScenarioDecompiler;
import com.frost.envoys.client.gui.script.ScriptProject;
import com.frost.envoys.util.ClientPathManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientBackupManager {

    private static final Map<UUID, ScriptProject> ACTIVE = new HashMap<>();
    private static final Map<UUID, ScriptProject> PENDING_RESTORE = new HashMap<>();

    private ClientBackupManager() {
    }

    public static void markActive(UUID npcUuid, ScriptProject project) {
        ACTIVE.put(npcUuid, project);
    }

    public static void clearActive(UUID npcUuid) {
        ACTIVE.remove(npcUuid);
    }

    public static void flushActive() {
        for (Map.Entry<UUID, ScriptProject> entry : ACTIVE.entrySet()) {
            if (hasDirty(entry.getValue())) {
                save(entry.getKey(), entry.getValue());
            }
        }
    }

    public static boolean hasDirty(ScriptProject project) {
        return project != null && project.dirty;
    }

    public static void stashPendingRestore(UUID npcUuid, ScriptProject project) {
        if (project != null) {
            PENDING_RESTORE.put(npcUuid, project);
        }
    }

    public static ScriptProject consumePendingRestore(UUID npcUuid) {
        return PENDING_RESTORE.remove(npcUuid);
    }

    public static Path file(UUID npcUuid) {
        return ClientPathManager.getClientLocalTempDir().resolve(npcUuid.toString() + ".temp.lua");
    }

    public static boolean exists(UUID npcUuid) {
        return Files.isRegularFile(file(npcUuid));
    }

    public static void save(UUID npcUuid, ScriptProject project) {
        if (project == null) {
            return;
        }
        try {
            String source = ScenarioCompiler.compile(project, ScriptProject.EVENT_ORDER, false);
            Files.writeString(file(npcUuid), source, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Envoys.LOGGER.error("[Envoys] Failed to write Lua .temp for NPC {}", npcUuid, e);
        }
    }

    public static ScriptProject load(UUID npcUuid) {
        Path path = file(npcUuid);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            ScriptProject project = ScenarioDecompiler.decompile(source, ScriptProject.EVENT_ORDER);
            project.dirty = true;
            return project;
        } catch (Exception e) {
            Envoys.LOGGER.error("[Envoys] Failed to read Lua .temp for NPC {}", npcUuid, e);
            return null;
        }
    }

    public static void delete(UUID npcUuid) {
        try {
            Files.deleteIfExists(file(npcUuid));
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to delete Lua .temp for NPC {}", npcUuid, e);
        }
    }
}
