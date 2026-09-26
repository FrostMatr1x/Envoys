package com.frost.envoys.client;

import com.frost.envoys.action.NPCInteractManager;
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
                                                        StringArgumentType.getString(ctx, "file"))))))
                        .then(Commands.literal("pull")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(ClientLuaCommands::suggestTargets)
                                        .executes(ctx -> pull(StringArgumentType.getString(ctx, "target")))))));
    }

    private static int list() {
        List<String> scripts = ClientLocalScriptStore.listScripts();
        if (scripts.isEmpty()) {
            message(Component.translatable("envoys.cmd.lua.list_empty"));
            return 0;
        }
        message(Component.translatable("envoys.cmd.lua.list_header", scripts.size()));
        for (String name : scripts) {
            message(Component.literal("§7 - " + name));
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
            message(Component.translatable("envoys.cmd.lua.read_failed", fileName));
            return 0;
        }

        PacketDistributor.sendToServer(new SaveNpcLuaScriptPayload(npcId, fileName, source, true));
        message(Component.translatable("envoys.cmd.lua.sending", fileName, npcId.toString()));
        return 1;
    }

    private static int pull(String target) {
        UUID npcId = resolveTarget(target);
        if (npcId == null) {
            return 0;
        }

        String npcName = resolveNpcName(target, npcId);
        ClientLuaScriptBridge.requestPull(npcId, payload -> {
            if (!payload.exists()) {
                message(Component.translatable("envoys.cmd.lua.pull_failed", payload.message()));
                return;
            }
            String fileName = ScriptNames.fileName(npcName, npcId);
            if (ClientLocalScriptStore.writeScript(fileName, payload.source())) {
                message(Component.translatable("envoys.cmd.lua.saved", fileName));
            } else {
                message(Component.translatable("envoys.cmd.lua.write_failed", fileName));
            }
        });
        message(Component.translatable("envoys.cmd.lua.requesting", npcId.toString()));
        return 1;
    }

    private static String resolveNpcName(String target, UUID npcId) {
        Entity entity = resolveTargetEntity(target, npcId);
        if (entity != null && entity.getCustomName() != null && !entity.getCustomName().getString().isBlank()) {
            return entity.getCustomName().getString();
        }
        NPCInteractManager manager = NPCInteractManager.byUUID(npcId).orElse(null);
        if (manager != null && manager.passport != null && manager.passport.npcName != null
                && !manager.passport.npcName.isBlank()) {
            return manager.passport.npcName;
        }
        return "npc";
    }

    private static Entity resolveTargetEntity(String target, UUID npcId) {
        if ("aim".equalsIgnoreCase(target)) {
            return crosshairNpc();
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (npcId.equals(entity.getUUID())) {
                    return entity;
                }
            }
        }
        return null;
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
            message(Component.translatable("envoys.cmd.lua.aim_required"));
            return null;
        }

        try {
            return UUID.fromString(target);
        } catch (IllegalArgumentException e) {
            message(Component.translatable("envoys.cmd.lua.invalid_uuid", target));
            return null;
        }
    }

    private static void message(Component text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(text);
        }
    }
}
