package com.frost.envoys.client;

import com.frost.envoys.network.payload.SaveNpcLuaScriptPayload;
import com.frost.envoys.npc.entity.BaseNPC;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ClientLuaCommands {

    private ClientLuaCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("envoys")
                .then(Commands.literal("lua")
                        .then(Commands.literal("list")
                                .executes(ctx -> list()))
                        .then(Commands.literal("send")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(ClientLuaCommands::suggestTargets)
                                        .then(Commands.argument("file", StringArgumentType.word())
                                                .suggests(ClientLuaCommands::suggestFiles)
                                                .executes(ctx -> send(
                                                        StringArgumentType.getString(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "file"))))))));
    }

    private static int list() {
        List<String> scripts = ClientLocalScriptStore.listScripts();
        if (scripts.isEmpty()) {
            message("§e[Envoys] В папке envoys/local нет .lua файлов.");
            return 0;
        }
        message("§a[Envoys] Локальные Lua-скрипты (" + scripts.size() + "):");
        for (String name : scripts) {
            message("§7 - " + name);
        }
        return scripts.size();
    }

    private static int send(String target, String fileName) {
        UUID npcId = resolveTarget(target);
        if (npcId == null) {
            return 0;
        }

        String source = ClientLocalScriptStore.readScript(fileName);
        if (source == null) {
            message("§c[Envoys] Не удалось прочитать локальный файл: " + fileName);
            return 0;
        }

        PacketDistributor.sendToServer(new SaveNpcLuaScriptPayload(npcId, fileName, source));
        message("§7[Envoys] Отправка " + fileName + " для NPC " + npcId + "...");
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestTargets(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> options = new ArrayList<>();
        options.add("aim");

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null) {
            Entity crosshair = crosshairNpc();

            List<Entity> nearby = new ArrayList<>();
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (entity instanceof BaseNPC && entity.distanceToSqr(minecraft.player) <= 256.0D && entity != crosshair) {
                    nearby.add(entity);
                }
            }
            nearby.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(minecraft.player)));

            if (crosshair != null) {
                options.add(crosshair.getUUID().toString());
            }
            for (Entity npc : nearby) {
                options.add(npc.getUUID().toString());
            }
        }

        return SharedSuggestionProvider.suggest(options, builder);
    }

    private static CompletableFuture<Suggestions> suggestFiles(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ClientLocalScriptStore.listScripts(), builder);
    }

    private static Entity crosshairNpc() {
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (entity instanceof BaseNPC) {
                return entity;
            }
        }
        return null;
    }

    private static UUID resolveTarget(String target) {
        if ("aim".equalsIgnoreCase(target)) {
            Entity crosshair = crosshairNpc();
            if (crosshair != null) {
                return crosshair.getUUID();
            }
            message("§c[Envoys] Наведитесь на NPC (или укажите UUID).");
            return null;
        }

        try {
            return UUID.fromString(target);
        } catch (IllegalArgumentException e) {
            message("§c[Envoys] Некорректный UUID: " + target);
            return null;
        }
    }

    private static void message(String text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal(text));
        }
    }
}
