package com.frost.envoys.action.serialization;

import com.frost.envoys.Envoys;
import com.frost.envoys.util.ColorUtils;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;

public class ComponentAdapter extends TypeAdapter<Component> {

    @Override
    public void write(JsonWriter out, Component value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        try {
            HolderLookup.Provider registries = getRegistryAccess();
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);

            DataResult<JsonElement> result = ComponentSerialization.CODEC.encodeStart(ops, value);
            if (result.isSuccess() && result.result().isPresent()) {
                TypeAdapters.JSON_ELEMENT.write(out, result.result().get());
                return;
            } else {
                Envoys.LOGGER.error("Ошибка сериализации Component: {}", 
                    result.error().map(DataResult.Error::message).orElse("Неизвестная ошибка"));
            }
        } catch (Exception e) {
            Envoys.LOGGER.error("Исключение при сериализации Component: ", e);
        }

        out.nullValue();
    }

    @Override
    public Component read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        JsonElement element = TypeAdapters.JSON_ELEMENT.read(in);
        if (element == null || element.isJsonNull()) {
            return null;
        }

        try {
            HolderLookup.Provider registries = getRegistryAccess();
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);

            Component component = ComponentSerialization.CODEC.parse(ops, element)
                    .result()
                    .orElse(null);

            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String rawStr = element.getAsString();
                if (component == null || rawStr.contains("#") || rawStr.contains("&") || rawStr.contains("§")) {
                    return ColorUtils.parse(rawStr);
                }
            }

            return component;
        } catch (Exception e) {
            Envoys.LOGGER.error("Ошибка десериализации Component: ", e);
            return null;
        }
    }

    private static HolderLookup.Provider getRegistryAccess() {
        try {
            net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && server.isSameThread()) {
                return server.registryAccess();
            }
        } catch (Throwable ignored) {}

        if (FMLEnvironment.dist.isClient()) {
            try {
                if (net.minecraft.client.Minecraft.getInstance().level != null) {
                    return net.minecraft.client.Minecraft.getInstance().level.registryAccess();
                }
                if (net.minecraft.client.Minecraft.getInstance().getConnection() != null) {
                    return net.minecraft.client.Minecraft.getInstance().getConnection().registryAccess();
                }
            } catch (Throwable ignored) {}
        }

        try {
            net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                return server.registryAccess();
            }
        } catch (Throwable ignored) {}

        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }
}