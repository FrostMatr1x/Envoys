package com.frost.envoys.action.serialization;

import com.frost.envoys.Envoys;
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
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;

public class ItemStackAdapter extends TypeAdapter<ItemStack> {

    @Override
    public void write(JsonWriter out, ItemStack value) throws IOException {
        if (value == null || value.isEmpty()) {
            out.nullValue();
            return;
        }

        try {
            HolderLookup.Provider registries = getRegistryAccess();
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);

            DataResult<JsonElement> result = ItemStack.CODEC.encodeStart(ops, value);
            if (result.isSuccess() && result.result().isPresent()) {
                TypeAdapters.JSON_ELEMENT.write(out, result.result().get());
                return;
            } else {
                Envoys.LOGGER.error("Ошибка сериализации ItemStack: {}", 
                    result.error().map(DataResult.Error::message).orElse("Неизвестная ошибка"));
            }
        } catch (Exception e) {
            Envoys.LOGGER.error("Исключение при сериализации ItemStack: ", e);
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

            return ItemStack.CODEC.parse(ops, element)
                    .result()
                    .orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            Envoys.LOGGER.error("Ошибка десериализации ItemStack: ", e);
            return ItemStack.EMPTY;
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