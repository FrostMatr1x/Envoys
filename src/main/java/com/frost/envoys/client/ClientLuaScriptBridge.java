package com.frost.envoys.client;

import com.frost.envoys.network.payload.FetchNpcLuaScriptPayload;
import com.frost.envoys.network.payload.NpcLuaScriptResponsePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class ClientLuaScriptBridge {

    private static final Map<UUID, Consumer<NpcLuaScriptResponsePayload>> SCREEN_LISTENERS = new HashMap<>();
    private static final Map<UUID, Consumer<NpcLuaScriptResponsePayload>> PULL_LISTENERS = new HashMap<>();

    private ClientLuaScriptBridge() {
    }

    public static void requestForScreen(UUID npcUuid, Consumer<NpcLuaScriptResponsePayload> listener) {
        SCREEN_LISTENERS.put(npcUuid, listener);
        PacketDistributor.sendToServer(new FetchNpcLuaScriptPayload(npcUuid));
    }

    public static void requestPull(UUID npcUuid, Consumer<NpcLuaScriptResponsePayload> listener) {
        PULL_LISTENERS.put(npcUuid, listener);
        PacketDistributor.sendToServer(new FetchNpcLuaScriptPayload(npcUuid));
    }

    public static void deliver(NpcLuaScriptResponsePayload payload) {
        Consumer<NpcLuaScriptResponsePayload> listener = SCREEN_LISTENERS.remove(payload.npcUuid());
        if (listener != null) {
            listener.accept(payload);
            return;
        }
        listener = PULL_LISTENERS.remove(payload.npcUuid());
        if (listener != null) {
            listener.accept(payload);
            return;
        }
        if (payload.message() != null && !payload.message().isBlank()) {
            message(payload.message());
        }
    }

    public static void clear() {
        SCREEN_LISTENERS.clear();
        PULL_LISTENERS.clear();
    }

    private static void message(String text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal(text));
        }
    }
}
