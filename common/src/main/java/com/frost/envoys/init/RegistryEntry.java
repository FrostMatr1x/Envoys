package com.frost.envoys.init;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;

public final class RegistryEntry<T> {
    private final ResourceLocation id;
    private Supplier<T> supplier;
    private T value;

    public RegistryEntry(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation getId() {
        return id;
    }

    public void set(T value) {
        this.value = value;
    }

    public void bind(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (supplier != null) {
            return supplier.get();
        }
        if (value == null) {
            throw new IllegalStateException("Registry entry is not bound: " + id);
        }
        return value;
    }

    public boolean isBound() {
        return supplier != null || value != null;
    }
}
