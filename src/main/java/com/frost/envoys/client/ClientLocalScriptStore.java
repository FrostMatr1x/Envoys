package com.frost.envoys.client;

import com.frost.envoys.Envoys;
import com.frost.envoys.util.ClientPathManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class ClientLocalScriptStore {

    public static final Pattern FILE_NAME = Pattern.compile("[a-zA-Z0-9_-]+\\.lua");

    private ClientLocalScriptStore() {
    }

    public static Path localDir() {
        return ClientPathManager.getClientLocalLuaDir();
    }

    public static List<String> listScripts() {
        List<String> result = new ArrayList<>();
        Path dir = localDir();
        try (Stream<Path> entries = Files.list(dir)) {
            entries.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> FILE_NAME.matcher(name).matches())
                    .sorted()
                    .forEach(result::add);
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to list local Lua scripts in {}", dir, e);
        }
        return result;
    }

    public static String readScript(String fileName) {
        if (fileName == null || !FILE_NAME.matcher(fileName).matches()) {
            return null;
        }
        Path file = localDir().resolve(fileName);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Envoys.LOGGER.error("[Envoys] Failed to read local Lua script {}", file, e);
            return null;
        }
    }
}
