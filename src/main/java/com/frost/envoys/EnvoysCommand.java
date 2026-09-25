package com.frost.envoys;

import com.frost.envoys.Envoys;
import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.ScriptRunner;
import com.frost.envoys.config.NPCConfigManager;
import com.frost.envoys.init.ModEntities;
import com.frost.envoys.lua.LuaEngineManager;
import com.frost.envoys.npc.entity.BaseNPC;
import com.frost.envoys.quest.PlayerQuestManager;
import com.frost.envoys.quest.QuestDefinition;
import com.frost.envoys.quest.QuestIndex;
import com.frost.envoys.skin.model.SkinIndexData;
import com.frost.envoys.skin.model.SkinIndexEntry;
import com.frost.envoys.skin.service.SkinLocalService;
import com.frost.envoys.skin.service.SkinSyncService;
import com.frost.envoys.util.PathManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EnvoysCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("envoys")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("cleanup")
                        .then(Commands.literal("npcs").executes(ctx -> cleanupNpcs(ctx.getSource())))
                        .then(Commands.literal("skins").executes(ctx -> cleanupSkins(ctx.getSource())))
                        .then(Commands.literal("all").executes(ctx -> {
                            int npcs = cleanupNpcs(ctx.getSource());
                            int skins = cleanupSkins(ctx.getSource());
                            return npcs + skins;
                        }))
                )
                .then(Commands.literal("regen")
                        .then(Commands.argument("npc", UuidArgument.uuid())
                                .executes(ctx -> regenNpc(ctx.getSource(), UuidArgument.getUuid(ctx, "npc"))))
                )
                .then(Commands.literal("tp")
                        .then(Commands.argument("npc", UuidArgument.uuid())
                                .executes(ctx -> teleportToNpc(ctx.getSource(), UuidArgument.getUuid(ctx, "npc"))))
                )
                .then(Commands.literal("quest")
                        .then(Commands.literal("list")
                                .executes(ctx -> questList(ctx.getSource())))
                        .then(Commands.literal("find")
                                .then(Commands.argument("query", StringArgumentType.greedyString())
                                        .suggests(EnvoysCommand::suggestQuestLocalIds)
                                        .executes(ctx -> questFind(ctx.getSource(), StringArgumentType.getString(ctx, "query")))))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("uuid", StringArgumentType.word())
                                        .suggests(EnvoysCommand::suggestQuestUuids)
                                        .executes(ctx -> questDelete(ctx.getSource(), StringArgumentType.getString(ctx, "uuid")))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(EnvoysCommand::suggestPlayerNames)
                                        .then(Commands.argument("uuid", StringArgumentType.word())
                                                .suggests(EnvoysCommand::suggestQuestUuids)
                                                .executes(ctx -> questReset(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "uuid"))))))
                )
                .then(Commands.literal("npc")
                        .then(Commands.literal("passport")
                                .then(Commands.literal("save")
                                        .executes(ctx -> saveNpcConfigs(ctx.getSource())))
                                .then(Commands.literal("load")
                                        .executes(ctx -> reloadNpcConfigs(ctx.getSource()))))
                        .then(Commands.literal("kill")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(EnvoysCommand::suggestNpcTargets)
                                        .executes(ctx -> killNpc(ctx.getSource(), StringArgumentType.getString(ctx, "target")))))
                )
                .then(Commands.literal("lua")
                        .then(Commands.literal("reload")
                                .executes(ctx -> reloadLuaScripts(ctx.getSource())))
                        .then(Commands.literal("stop")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(EnvoysCommand::suggestNpcTargets)
                                        .executes(ctx -> stopLua(ctx.getSource(), StringArgumentType.getString(ctx, "target"))))));

        dispatcher.register(command);
    }

    private static int cleanupSkins(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();

        Path cacheDir = PathManager.getServerSkinCacheDir();
        File indexFile = PathManager.getServerIndexFile();

        if (!Files.exists(cacheDir) || !indexFile.exists()) {
            source.sendSuccess(() -> Component.literal("§e[Envoys] Кэш скинов пуст."), false);
            return 0;
        }

        Set<String> activeSkinKeys = new HashSet<>();

        List<UUID> savedUuids = NPCConfigManager.getAllSavedUuids();
        for (UUID uuid : savedUuids) {
            NPCConfigManager.load(uuid).ifPresent(script -> {
                if (script.passport != null) {
                    if (script.passport.skinHash != null && !script.passport.skinHash.isBlank()) {
                        activeSkinKeys.add(script.passport.skinHash.toLowerCase());
                    }
                    if (script.passport.skinValue != null && !script.passport.skinValue.isBlank()) {
                        activeSkinKeys.add(SkinSyncService.sanitizeFileName(script.passport.skinValue).toLowerCase());
                    }
                }
            });
        }

        SkinIndexData indexData = SkinLocalService.readIndex(indexFile);
        List<SkinIndexEntry> toRemove = new ArrayList<>();
        long freedBytes = 0;

        for (SkinIndexEntry entry : indexData.skins) {
            String entryHash = entry.hash != null ? entry.hash.toLowerCase() : "";
            String entryUuid = entry.uuid != null ? entry.uuid.toLowerCase() : "";

            if (!activeSkinKeys.contains(entryHash) && !activeSkinKeys.contains(entryUuid)) {
                toRemove.add(entry);

                Path pngPath = cacheDir.resolve(entry.path);
                try {
                    if (Files.exists(pngPath)) {
                        freedBytes += Files.size(pngPath);
                        Files.deleteIfExists(pngPath);
                    }
                } catch (IOException e) {
                    Envoys.LOGGER.error("[Envoys] Ошибка удаления кэш-файла скина {}", pngPath, e);
                }

                Path jsonPath = cacheDir.resolve(entry.uuid + ".json");
                try {
                    if (Files.exists(jsonPath)) {
                        freedBytes += Files.size(jsonPath);
                        Files.deleteIfExists(jsonPath);
                    }
                } catch (IOException ignored) {}
            }
        }

        indexData.skins.removeAll(toRemove);
        SkinLocalService.saveIndex(indexFile, indexData);

        final int finalRemovedCount = toRemove.size();
        final double finalFreedMb = (double) freedBytes / (1024 * 1024);

        source.sendSuccess(() -> Component.literal(String.format(
                "§a[Envoys] Очищено неиспользуемых скинов: %d (Освобождено %.2f MB)", 
                finalRemovedCount, finalFreedMb
        )), true);

        return finalRemovedCount;
    }

    private static int cleanupNpcs(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        List<UUID> savedUuids = NPCConfigManager.getAllSavedUuids();
        int removedCount = 0;
        int unverifiable = 0;

        for (UUID uuid : savedUuids) {
            // Corrupt/unreadable files are always safe to remove.
            if (NPCConfigManager.deleteIfCorrupt(uuid)) {
                removedCount++;
                continue;
            }

            // Alive NPCs are never touched: a loaded BaseNPC with this uuid in any
            // dimension proves the record is in use. Checked before loading the config
            // so NPCInteractManager.load() does not needlessly re-register SCRIPTS.
            if (isNpcLoaded(server, uuid)) {
                continue;
            }

            Optional<NPCInteractManager> opt = NPCConfigManager.load(uuid);
            if (opt.isEmpty() || opt.get().passport == null) {
                continue;
            }
            var passport = opt.get().passport;

            // Places where the NPC can plausibly exist. The last tracked position is
            // written by BaseNPC every tick, but records created by older builds (and
            // records of NPCs that died before ever ticking) may lack it, so the hold
            // position is used as a fallback: it is persisted in every build and NPCs
            // with hold-position enabled teleport back to it.
            ServerLevel passportLevel = resolveLevel(server, passport.dimension);
            List<Candidate> candidates = new ArrayList<>();

            if (passport.hasLastPosition && passportLevel != null) {
                candidates.add(new Candidate(passportLevel, passport.lastX, passport.lastZ));
            }
            if (passport.holdX != 0 || passport.holdY != 0 || passport.holdZ != 0) {
                if (passportLevel != null) {
                    if (candidates.isEmpty()
                            || candidates.stream().noneMatch(c -> c.level == passportLevel && c.x == passport.holdX && c.z == passport.holdZ)) {
                        candidates.add(new Candidate(passportLevel, passport.holdX, passport.holdZ));
                    }
                } else {
                    // Dimension unknown: verify the hold position in every dimension.
                    for (ServerLevel level : server.getAllLevels()) {
                        candidates.add(new Candidate(level, passport.holdX, passport.holdZ));
                    }
                }
            }

            if (candidates.isEmpty()) {
                // No trustworthy location at all: the record may belong to an unloaded
                // NPC, so it is kept. Report it so the admin can investigate.
                unverifiable++;
                continue;
            }

            boolean found = false;
            for (Candidate candidate : candidates) {
                // Synchronously load the chunk (including its entity list) so an NPC
                // standing in an unloaded chunk is not mistaken for a dead one.
                int chunkX = (int) Math.floor(candidate.x) >> 4;
                int chunkZ = (int) Math.floor(candidate.z) >> 4;
                candidate.level.getChunk(chunkX, chunkZ);
                if (candidate.level.getEntity(uuid) instanceof BaseNPC) {
                    found = true;
                    break;
                }
            }
            if (found) {
                continue;
            }

            // Neither loaded in any dimension nor present at any known location
            // after force-loading the candidate chunks -> the entity is gone.
            NPCConfigManager.delete(uuid);
            removedCount++;
        }

        final int finalCount = removedCount;
        final int finalUnverifiable = unverifiable;
        source.sendSuccess(() -> Component.literal(
                "§a[Envoys] Удалено записей удалённых NPC: " + finalCount
                        + " (записи существующих NPC не затрагиваются)"), true);
        if (finalUnverifiable > 0) {
            source.sendSuccess(() -> Component.literal(
                    "§e[Envoys] Пропущено записей без известной локации: " + finalUnverifiable), false);
        }
        return finalCount;
    }

    private static boolean isNpcLoaded(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(uuid) instanceof BaseNPC) {
                return true;
            }
        }
        return false;
    }

    private static ServerLevel resolveLevel(MinecraftServer server, String dimension) {
        if (dimension == null || dimension.isBlank()) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimension)) {
                return level;
            }
        }
        return null;
    }

    private record Candidate(ServerLevel level, double x, double z) {
    }

    private static int regenNpc(CommandSourceStack source, UUID uuid) {
        MinecraftServer server = source.getServer();

        Optional<NPCInteractManager> opt = NPCConfigManager.load(uuid);
        if (opt.isEmpty()) {
            source.sendFailure(Component.literal("§c[Envoys] Конфиг NPC " + uuid + " не найден."));
            return 0;
        }

        NPCInteractManager script = opt.get();

        int removed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(uuid) instanceof BaseNPC npc) {
                npc.discard();
                removed++;
            }
        }

        double x = script.passport.holdX;
        double y = script.passport.holdY;
        double z = script.passport.holdZ;

        if (x == 0 && y == 0 && z == 0) {
            x = source.getPosition().x;
            y = source.getPosition().y;
            z = source.getPosition().z;
        }

        ServerLevel level = source.getLevel();
        BaseNPC npc = new BaseNPC(ModEntities.BASE_NPC.get(), level);
        npc.setUUID(uuid);
        npc.moveTo(x, y, z, 0.0F, 0.0F);
        npc.applyPassportData(script.passport);
        level.addFreshEntity(npc);

        final int finalRemoved = removed;
        final String message = String.format(
                "§a[Envoys] NPC %s регенерирован (удалено: %d) на %.1f %.1f %.1f",
                uuid, finalRemoved, x, y, z
        );
        source.sendSuccess(() -> Component.literal(message), true);

        return 1;
    }

    private static int teleportToNpc(CommandSourceStack source, UUID uuid) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        BaseNPC npc = null;
        ServerLevel npcLevel = null;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            if (level.getEntity(uuid) instanceof BaseNPC found) {
                npc = found;
                npcLevel = level;
                break;
            }
        }

        if (npc == null) {
            source.sendFailure(Component.literal("§c[Envoys] NPC " + uuid + " не найден в мире."));
            return 0;
        }

        player.teleportTo(npcLevel, npc.getX(), npc.getY(), npc.getZ(),
                java.util.Set.<RelativeMovement>of(), player.getYRot(), player.getXRot());

        source.sendSuccess(() -> Component.literal("§a[Envoys] Телепорт к NPC " + uuid), true);
        return 1;
    }

    private static int saveNpcConfigs(CommandSourceStack source) {
        NPCConfigManager.saveAll();
        int count = NPCInteractManager.SCRIPTS.size();
        source.sendSuccess(() -> Component.literal("§a[Envoys] Сохранено конфигов NPC: " + count), true);
        return count;
    }

    private static int reloadNpcConfigs(CommandSourceStack source) {
        NPCInteractManager.SCRIPTS.clear();
        ScriptRunner.clear();
        NPCConfigManager.loadAll();
        
        MinecraftServer server = source.getServer();
        int updatedEntities = 0;

        for (ServerLevel level : server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof BaseNPC npc) {
                    NPCConfigManager.load(npc.getUUID()).ifPresent(manager -> {
                        updateNpcFromPassport(npc, manager.passport);
                    });
                    updatedEntities++;
                }
            }
        }

        int count = NPCInteractManager.SCRIPTS.size();
        final int finalUpdated = updatedEntities;
        source.sendSuccess(() -> Component.literal(
                String.format("§a[Envoys] Загружено конфигов NPC: %d (Обновлено в мире: %d)", count, finalUpdated)
        ), true);

        return count;
    }

    private static int reloadLuaScripts(CommandSourceStack source) {
        LuaEngineManager.ReloadResult result = LuaEngineManager.reloadAll();
        final String message = String.format(
                "§a[Envoys] Lua: загружено %d, ошибок %d",
                result.loaded(), result.failed()
        );
        source.sendSuccess(() -> Component.literal(message), true);
        return result.loaded();
    }

    private static int questList(CommandSourceStack source) {
        List<QuestIndex.Entry> entries = QuestIndex.entries();
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e[Envoys] Квесты не найдены."), false);
            return 0;
        }

        for (QuestIndex.Entry entry : entries) {
            QuestDefinition quest = entry.quest();
            String line = String.format("[%s] | %s -> \"%s\" (NPC: %s)",
                    safe(quest.questUuid), safe(quest.localId), safe(quest.title), entry.manager().npcUUID);
            source.sendSuccess(() -> Component.literal(line), false);
        }
        source.sendSuccess(() -> Component.literal("§a[Envoys] Всего квестов: " + entries.size()), false);
        return entries.size();
    }

    private static int questFind(CommandSourceStack source, String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isEmpty()) {
            source.sendFailure(Component.literal("§c[Envoys] Укажите запрос."));
            return 0;
        }

        String lowerQuery = query.toLowerCase(Locale.ROOT);
        int found = 0;
        for (QuestIndex.Entry entry : QuestIndex.entries()) {
            QuestDefinition quest = entry.quest();
            boolean localMatch = quest.localId != null && quest.localId.equals(query);
            boolean titleMatch = quest.title != null && quest.title.toLowerCase(Locale.ROOT).contains(lowerQuery);
            if (!localMatch && !titleMatch) {
                continue;
            }
            found++;
            String line = String.format("%s | %s | \"%s\" | NPC: %s",
                    safe(quest.questUuid), safe(quest.localId), safe(quest.title), entry.manager().npcUUID);
            source.sendSuccess(() -> Component.literal(line), false);
        }

        if (found == 0) {
            source.sendSuccess(() -> Component.literal("§e[Envoys] Ничего не найдено."), false);
        } else {
            final int total = found;
            source.sendSuccess(() -> Component.literal("§a[Envoys] Найдено: " + total), false);
        }
        return found;
    }

    private static int questDelete(CommandSourceStack source, String rawUuid) {
        String uuid = rawUuid == null ? "" : rawUuid.trim();
        if (uuid.isEmpty()) {
            source.sendFailure(Component.literal("§c[Envoys] Укажите quest_uuid."));
            return 0;
        }

        Set<NPCInteractManager> changed = new LinkedHashSet<>();
        int removed = 0;
        for (QuestIndex.Entry entry : QuestIndex.entries()) {
            if (uuid.equals(entry.quest().questUuid) && entry.manager().quests.remove(entry.quest())) {
                removed++;
                changed.add(entry.manager());
            }
        }

        for (NPCInteractManager manager : changed) {
            NPCConfigManager.save(manager);
        }
        if (removed > 0) {
            QuestIndex.invalidate();
        }

        final int total = removed;
        if (total == 0) {
            source.sendSuccess(() -> Component.literal("§e[Envoys] Квест " + safe(uuid) + " не найден."), false);
        } else {
            source.sendSuccess(() -> Component.literal("§a[Envoys] Удалено квестов: " + total), true);
        }
        return total;
    }

    private static int questReset(CommandSourceStack source, String rawPlayerName, String rawUuid) {
        MinecraftServer server = source.getServer();
        String playerName = rawPlayerName == null ? "" : rawPlayerName.trim();
        ServerPlayer target = server.getPlayerList().getPlayerByName(playerName);
        if (target == null) {
            source.sendFailure(Component.literal("§c[Envoys] Игрок '" + playerName + "' не в сети."));
            return 0;
        }
        String uuid = rawUuid == null ? "" : rawUuid.trim();
        if (uuid.isEmpty()) {
            source.sendFailure(Component.literal("§c[Envoys] Укажите quest_uuid."));
            return 0;
        }
        PlayerQuestManager.reset(target, uuid);
        final String message = String.format("§a[Envoys] Прогресс квеста %s сброшен для игрока %s.",
                safe(uuid), target.getName().getString());
        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static int stopLua(CommandSourceStack source, String rawTarget) throws CommandSyntaxException {
        BaseNPC npc = resolveNpcTarget(source, rawTarget);
        if (npc == null) {
            source.sendFailure(Component.literal("§c[Envoys] NPC не найден (укажите UUID или наведитесь на NPC: aim)."));
            return 0;
        }
        UUID uuid = npc.getUUID();
        LuaEngineManager.stop(uuid);
        final String message = "§a[Envoys] Lua-движок NPC " + uuid + " остановлен.";
        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static int killNpc(CommandSourceStack source, String rawTarget) throws CommandSyntaxException {
        BaseNPC npc = resolveNpcTarget(source, rawTarget);
        if (npc == null) {
            source.sendFailure(Component.literal("§c[Envoys] NPC не найден (укажите UUID или наведитесь на NPC: aim)."));
            return 0;
        }
        UUID uuid = npc.getUUID();
        npc.discard();
        final String message = "§a[Envoys] NPC " + uuid + " удалён.";
        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static BaseNPC resolveNpcTarget(CommandSourceStack source, String rawTarget) throws CommandSyntaxException {
        String target = rawTarget == null ? "" : rawTarget.trim();
        if ("aim".equalsIgnoreCase(target)) {
            return raycastNpc(source.getPlayerOrException());
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(target);
        } catch (IllegalArgumentException e) {
            return null;
        }
        for (ServerLevel level : source.getServer().getAllLevels()) {
            if (level.getEntity(uuid) instanceof BaseNPC npc) {
                return npc;
            }
        }
        return null;
    }

    private static BaseNPC raycastNpc(ServerPlayer player) {
        double range = 16.0D;
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * range, look.y * range, look.z * range);
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                entity -> entity instanceof BaseNPC && entity.isPickable(), range * range);
        return hit != null && hit.getEntity() instanceof BaseNPC npc ? npc : null;
    }

    private static CompletableFuture<Suggestions> suggestNpcTargets(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        if ("aim".startsWith(remaining)) {
            builder.suggest("aim");
        }
        for (ServerLevel level : context.getSource().getServer().getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof BaseNPC npc) {
                    String uuid = npc.getUUID().toString();
                    if (uuid.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                        builder.suggest(uuid);
                    }
                }
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestPlayerNames(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            String name = player.getGameProfile().getName();
            if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestQuestUuids(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        Set<String> seen = new HashSet<>();
        for (QuestIndex.Entry entry : QuestIndex.entries()) {
            String uuid = entry.quest().questUuid;
            if (uuid != null && !uuid.isBlank()
                    && uuid.toLowerCase(Locale.ROOT).startsWith(remaining)
                    && seen.add(uuid)) {
                builder.suggest(uuid);
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestQuestLocalIds(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        Set<String> seen = new HashSet<>();
        for (QuestIndex.Entry entry : QuestIndex.entries()) {
            String localId = entry.quest().localId;
            if (localId != null && !localId.isBlank()
                    && localId.toLowerCase(Locale.ROOT).startsWith(remaining)
                    && seen.add(localId)) {
                builder.suggest(localId);
            }
        }
        return builder.buildFuture();
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\u00a7' || c < 0x20) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static void updateNpcFromPassport(BaseNPC npc, com.frost.envoys.action.NPCPassportData passport) {
        if (passport == null) return;

        if (passport.npcName != null && !passport.npcName.isBlank()) {
            npc.setCustomName(com.frost.envoys.util.ColorUtils.parse(passport.npcName));
            npc.setCustomNameVisible(true);
        } else {
            npc.setCustomNameVisible(false);
        }

        // 2. Вызов метода обновления скина/параметров в самом классе BaseNPC (если он у вас есть)
        // Например: npc.refreshPassport(passport); или npc.updateSkin();
    }
}