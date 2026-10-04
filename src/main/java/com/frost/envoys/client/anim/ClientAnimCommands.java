package com.frost.envoys.client.anim;

import com.frost.envoys.client.EmoteIntegration;
import com.frost.envoys.network.payload.DeleteAnimPayload;
import com.frost.envoys.network.payload.PushAnimPayload;
import com.google.gson.JsonParser;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ClientAnimCommands {

    private ClientAnimCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("envoys")
                .then(Commands.literal("anim")
                        .then(Commands.literal("reload")
                                .executes(ctx -> reload()))
                        .then(Commands.literal("push")
                                .executes(ctx -> pushAll())
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests(ClientAnimCommands::suggestLocal)
                                        .executes(ctx -> pushOne(StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("delete")
                                // Имена серверных анимаций содержат пробелы и §-коды,
                                // поэтому word() обрезал бы вставленную подсказку.
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .suggests(ClientAnimCommands::suggestServer)
                                        .executes(ctx -> delete(StringArgumentType.getString(ctx, "name")))))));
    }

    private static int reload() {
        ClientAnimStore.ScanResult result = ClientAnimStore.scan();
        message(Component.translatable("envoys.cmd.anim.reload_done", result.loaded(), result.errors()));
        return result.loaded();
    }

    private static int pushAll() {
        if (ClientAnimStore.isEmpty()) {
            ClientAnimStore.scan();
        }
        if (ClientAnimStore.isEmpty()) {
            message(Component.translatable("envoys.cmd.anim.local_empty"));
            return 0;
        }
        if (!EmoteIntegration.serverIndexLoaded()) {
            EmoteIntegration.requestServerList();
            message(Component.translatable("envoys.cmd.anim.index_loading"));
            return 0;
        }

        int sent = 0;
        int failed = 0;
        for (String localName : ClientAnimStore.listNames()) {
            ClientAnimStore.Entry entry = ClientAnimStore.get(localName);
            if (entry == null) continue;
            String serverHash = EmoteIntegration.serverHash(localName);
            if (serverHash != null && serverHash.equalsIgnoreCase(entry.hash())) {
                continue;
            }
            if (send(localName, entry)) {
                sent++;
            } else {
                failed++;
            }
        }

        if (sent == 0 && failed == 0) {
            message(Component.translatable("envoys.cmd.anim.all_up_to_date"));
        } else {
            message(Component.translatable("envoys.cmd.anim.push_all_sending", sent, failed));
        }
        return sent;
    }

    private static int pushOne(String name) {
        String canonical = EmoteIntegration.sanitizeAnimName(name);
        ClientAnimStore.Entry entry = ClientAnimStore.get(canonical);
        if (entry == null) {
            message(Component.translatable("envoys.cmd.anim.not_found_local", canonical));
            return 0;
        }
        return send(canonical, entry) ? 1 : 0;
    }

    private static boolean send(String name, ClientAnimStore.Entry entry) {
        byte[] data = ClientAnimStore.read(entry);
        if (data == null || data.length == 0) {
            message(Component.translatable("envoys.cmd.anim.read_failed", name));
            return false;
        }
        if (data.length > ClientAnimStore.MAX_FILE_SIZE_BYTES) {
            message(Component.translatable("envoys.cmd.anim.too_big", name));
            return false;
        }
        try {
            JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
        } catch (Exception e) {
            message(Component.translatable("envoys.cmd.anim.invalid_json", name));
            return false;
        }
        PacketDistributor.sendToServer(new PushAnimPayload(name, data, entry.hash()));
        message(Component.translatable("envoys.cmd.anim.pushing", name));
        return true;
    }

    private static int delete(String name) {
        if (!EmoteIntegration.serverIndexLoaded()) {
            EmoteIntegration.requestServerList();
            message(Component.translatable("envoys.cmd.anim.index_loading"));
            return 0;
        }
        PacketDistributor.sendToServer(new DeleteAnimPayload(name));
        message(Component.translatable("envoys.cmd.anim.deleting", name));
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestLocal(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ClientAnimStore.listNames(), builder);
    }

    private static CompletableFuture<Suggestions> suggestServer(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> names = EmoteIntegration.serverAnimNames();
        return SharedSuggestionProvider.suggest(names, builder);
    }

    private static void message(Component text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(text);
        }
    }
}
