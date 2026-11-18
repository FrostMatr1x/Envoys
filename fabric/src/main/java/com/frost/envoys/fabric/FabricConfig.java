package com.frost.envoys.fabric;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.frost.envoys.EnvoysConstants;
import com.frost.envoys.platform.ConfigData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

public final class FabricConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ConfigData DATA = new ConfigData();

    private FabricConfig() {
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("envoys.json");
        if (!Files.isRegularFile(file)) {
            save(file);
            return;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("magicNumber")) {
                DATA.magicNumber = root.get("magicNumber").getAsInt();
            }
            if (root.has("magicNumberIntroduction")) {
                DATA.magicNumberIntroduction = root.get("magicNumberIntroduction").getAsString();
            }
            if (root.has("lua")) {
                JsonObject lua = root.getAsJsonObject("lua");
                if (lua.has("enabled")) {
                    DATA.luaEnabled = lua.get("enabled").getAsBoolean();
                }
                if (lua.has("instructionLimit")) {
                    DATA.luaInstructionLimit = Math.max(1000, lua.get("instructionLimit").getAsInt());
                }
                if (lua.has("allowCommands")) {
                    DATA.luaAllowCommands = lua.get("allowCommands").getAsBoolean();
                }
            }
        } catch (Exception e) {
            EnvoysConstants.LOGGER.error("[Envoys] Failed to read config/envoys.json, using defaults", e);
        }
    }

    private static void save(Path file) {
        JsonObject lua = new JsonObject();
        lua.addProperty("enabled", DATA.luaEnabled);
        lua.addProperty("instructionLimit", DATA.luaInstructionLimit);
        lua.addProperty("allowCommands", DATA.luaAllowCommands);

        JsonObject root = new JsonObject();
        root.addProperty("magicNumber", DATA.magicNumber);
        root.addProperty("magicNumberIntroduction", DATA.magicNumberIntroduction);
        root.add("lua", lua);

        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            EnvoysConstants.LOGGER.error("[Envoys] Failed to write config/envoys.json", e);
        }
    }

    public static ConfigData data() {
        return DATA;
    }
}
