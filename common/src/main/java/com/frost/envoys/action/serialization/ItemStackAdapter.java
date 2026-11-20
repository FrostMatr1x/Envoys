package com.frost.envoys.action.serialization;

import com.frost.envoys.platform.EnvoysPlatform;

import com.frost.envoys.EnvoysConstants;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
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
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;

public class ItemStackAdapter extends TypeAdapter<ItemStack> {

    @Override
    public void write(JsonWriter out, ItemStack value) throws IOException {
        if (value == null || value.isEmpty()) {
            out.nullValue();
            return;
        }

        // Kept non-throwing on encode failure: the in-memory stack is already valid, and
        // throwing would abort whole-file serialization on GUI write paths.
        try {
            HolderLookup.Provider registries = getRegistryAccess();
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);

            DataResult<JsonElement> result = ItemStack.CODEC.encodeStart(ops, value);
            if (result.isSuccess() && result.result().isPresent()) {
                TypeAdapters.JSON_ELEMENT.write(out, result.result().get());
                return;
            } else {
                EnvoysConstants.LOGGER.error("Failed to serialize ItemStack: {}", 
                    result.error().map(DataResult.Error::message).orElse("Unknown error"));
            }
        } catch (Exception e) {
            EnvoysConstants.LOGGER.error("Exception while serializing ItemStack: ", e);
        }

        out.nullValue();
    }

    @Override
    public ItemStack read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return ItemStack.EMPTY;
        }

        JsonElement element = TypeAdapters.JSON_ELEMENT.read(in);
        if (element == null || element.isJsonNull()) {
            return ItemStack.EMPTY;
        }

        try {
            HolderLookup.Provider registries = getRegistryAccess();
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);

            DataResult<ItemStack> result = ItemStack.CODEC.parse(ops, element);
            if (result.isSuccess() && result.result().isPresent()) {
                return result.result().get();
            }
            String error = result.error().map(DataResult.Error::message).orElse("Unknown error");
            EnvoysConstants.LOGGER.error("Failed to deserialize ItemStack ({}): {}", error, element);
            throw new JsonParseException("Failed to parse ItemStack: " + error);
        } catch (JsonParseException e) {
            throw e;
        } catch (Exception e) {
            EnvoysConstants.LOGGER.error("Exception while deserializing ItemStack: ", e);
            throw new JsonParseException("Failed to parse ItemStack", e);
        }
    }

    private static HolderLookup.Provider getRegistryAccess() {
        try {
            net.minecraft.server.MinecraftServer server = EnvoysPlatform.getServer();
            if (server != null && server.isSameThread()) {
                return server.registryAccess();
            }
        } catch (Throwable ignored) {}

        if (EnvoysPlatform.isClient()) {
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
            net.minecraft.server.MinecraftServer server = EnvoysPlatform.getServer();
            if (server != null) {
                return server.registryAccess();
            }
        } catch (Throwable ignored) {}

        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }
}