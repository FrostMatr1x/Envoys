package com.frost.envoys.lua;

import com.frost.envoys.Envoys;
import com.frost.envoys.util.PathManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public final class LuaScriptStore {

    private static final String MAIN_SCRIPT = "main.lua";

    private LuaScriptStore() {
    }

    public static Path npcDir(UUID npcId) {
        return PathManager.getServerLuaDir().resolve(npcId.toString());
    }

    public static Path scriptFile(UUID npcId) {
        return npcDir(npcId).resolve(MAIN_SCRIPT);
    }

    public static boolean scriptExists(UUID npcId) {
        return Files.isRegularFile(scriptFile(npcId));
    }

    public static String readScript(UUID npcId) {
        Path file = scriptFile(npcId);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to read Lua script {}", file, e);
            return null;
        }
    }

    public static boolean writeScript(UUID npcId, String source) {
        Path file = scriptFile(npcId);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, source == null ? "" : source, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to write Lua script {}", file, e);
            return false;
        }
    }

    public static List<UUID> listNpcsWithScripts() {
        List<UUID> result = new ArrayList<>();
        Path root = PathManager.getServerLuaDir();
        if (!Files.isDirectory(root)) {
            return result;
        }
        try (Stream<Path> entries = Files.list(root)) {
            entries.filter(Files::isDirectory)
                    .filter(dir -> Files.isRegularFile(dir.resolve(MAIN_SCRIPT)))
                    .forEach(dir -> {
                        try {
                            result.add(UUID.fromString(dir.getFileName().toString()));
                        } catch (IllegalArgumentException ignored) {
                        }
                    });
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to list Lua script directory {}", root, e);
        }
        return result;
    }
}
