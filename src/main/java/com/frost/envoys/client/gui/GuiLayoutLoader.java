package com.frost.envoys.client.gui;

import com.frost.envoys.Envoys;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.Optional;

/**
 * Loader for client GUI layout configs. Missing or corrupted JSON falls back
 * to the supplied safe defaults.
 */
public final class GuiLayoutLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private GuiLayoutLoader() {
    }

    public static <T> T load(ResourceManager manager, ResourceLocation location, Class<T> type, T defaults) {
        if (manager == null) {
            return defaults;
        }
        try {
            Optional<Resource> resource = manager.getResource(location);
            if (resource.isEmpty()) {
                return defaults;
            }
            try (Reader reader = resource.get().openAsReader()) {
                T parsed = GSON.fromJson(reader, type);
                return parsed != null ? parsed : defaults;
            }
        } catch (Exception e) {
            Envoys.LOGGER.error("[Envoys] Failed to load gui layout {}", location, e);
            return defaults;
        }
    }
}
