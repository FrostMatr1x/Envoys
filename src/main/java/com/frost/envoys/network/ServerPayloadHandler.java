package com.frost.envoys.network;

import com.frost.envoys.Envoys;
import com.frost.envoys.config.Config;
import com.frost.envoys.config.NPCConfigManager;
import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.NPCPassportData;
import com.frost.envoys.action.NPCScriptData;
import com.frost.envoys.action.ScriptRunner;
import com.frost.envoys.action.event.NpcEventData;
import com.frost.envoys.action.model.ActionCommand;
import com.frost.envoys.action.model.ActionTrade;
import com.frost.envoys.action.model.EntityActionData;
import com.frost.envoys.action.serialization.EntityActionAdapter;
import com.frost.envoys.gui.menu.NPCMerchantMenu;
import com.frost.envoys.init.ModItems;
import com.frost.envoys.lua.LuaEngineManager;
import com.frost.envoys.lua.LuaNpcEngine;
import com.frost.envoys.lua.LuaSandbox;
import com.frost.envoys.lua.LuaScriptStore;
import com.frost.envoys.network.payload.LuaScriptUploadResultPayload;
import com.frost.envoys.network.payload.SaveNpcLuaScriptPayload;
import com.frost.envoys.network.payload.SaveNPCPassportPayload;
import com.frost.envoys.network.payload.SaveNPCScriptPayload;
import com.frost.envoys.network.payload.SelectDialogAnswerPayload;
import com.frost.envoys.network.payload.TradeAllPayload;
import com.frost.envoys.npc.entity.BaseNPC;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.compiler.CompileException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

public class ServerPayloadHandler {

    private static final int MAX_LUA_SOURCE_BYTES = 1024 * 1024;
    private static final Pattern LUA_FILE_NAME = Pattern.compile("[a-zA-Z0-9_-]+\\.lua");

    private static boolean hasTunerAccess(ServerPlayer player) {
        if (player == null) return false;
        if (player.hasPermissions(4)) return true;
        
        return player.getMainHandItem().is(ModItems.SURVIVAL_NPC_TUNER.get()) 
            || player.getOffhandItem().is(ModItems.SURVIVAL_NPC_TUNER.get());
    }

    public static void handleSaveNPCScript(final SaveNPCScriptPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                if (!(context.player() instanceof ServerPlayer player)) return;

                if (!hasTunerAccess(player)) {
                    Envoys.LOGGER.warn("[Envoys] Player {} tried to save script without holding a Tuner!", player.getName().getString());
                    return;
                }

                boolean isOp = player.hasPermissions(4);

                ServerLevel level = player.serverLevel();
                Entity target = level.getEntity(payload.npcId());
                if (target != null && player.distanceToSqr(target) > 64.0D) {
                    Envoys.LOGGER.warn("[Envoys] Player {} tried to edit NPC script {} from too far away!",
                            player.getName().getString(), payload.npcId());
                    return;
                }

                NPCInteractManager manager = NPCInteractManager.byUUID(payload.npcId())
                        .orElseGet(() -> new NPCInteractManager(payload.npcId()));

                if (!isOp && manager.passport.creativeTunerOnly) {
                    Envoys.LOGGER.warn("[Envoys] Player {} tried to modify creative-only NPC script!", player.getName().getString());
                    return;
                }

                NPCScriptData scriptData = EntityActionAdapter.GSON.fromJson(payload.jsonScript(), NPCScriptData.class);
                if (scriptData == null) return;

                if (!isOp && containsForbiddenActions(scriptData)) {
                    Envoys.LOGGER.warn("[Envoys] Security alert! Player {} tried to save forbidden action types in NPC {}", 
                            player.getName().getString(), payload.npcId());
                    return;
                }

                scriptData.applyTo(manager);

                NPCConfigManager.save(manager);
                Envoys.LOGGER.info("[Envoys] Script for NPC {} successfully saved on server", payload.npcId());

            } catch (Exception e) {
                Envoys.LOGGER.error("[Envoys] Failed to process and save NPC script for ID {}", payload.npcId(), e);
            }
        });
    }

    public static void handleSaveNpcLuaScript(final SaveNpcLuaScriptPayload payload, final IPayloadContext context) {
        ServerPlayer[] playerRef = new ServerPlayer[1];
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            playerRef[0] = player;

            try {
                if (!player.hasPermissions(4)) {
                    Envoys.LOGGER.warn("[Envoys] Player {} tried to upload a Lua script without OP-4",
                            player.getName().getString());
                    replyLua(player, false, "§cТолько OP-4 может загружать Lua-скрипты.");
                    return;
                }

                if (!Config.LUA_ENABLED.get()) {
                    replyLua(player, false, "§cLua-движок выключен в конфиге сервера.");
                    return;
                }

                MinecraftServer server = player.getServer();
                if (server == null) {
                    replyLua(player, false, "§cСервер недоступен.");
                    return;
                }

                if (findLuaNpc(server, payload.npcId()) == null) {
                    replyLua(player, false, "§cNPC " + payload.npcId() + " не найден ни в одном измерении.");
                    return;
                }

                String fileName = payload.fileName();
                if (fileName == null || !LUA_FILE_NAME.matcher(fileName).matches()) {
                    Envoys.LOGGER.warn("[Envoys] Player {} tried to upload Lua script with invalid name '{}'",
                            player.getName().getString(), fileName);
                    replyLua(player, false, "§cНедопустимое имя файла (ожидается [a-zA-Z0-9_-]+.lua).");
                    return;
                }

                String source = payload.source() == null ? "" : payload.source();
                int byteLength = source.getBytes(StandardCharsets.UTF_8).length;
                if (byteLength > MAX_LUA_SOURCE_BYTES) {
                    replyLua(player, false, "§cСкрипт слишком большой: " + byteLength + " байт (лимит 1 МБ).");
                    return;
                }

                try {
                    LuaSandbox.compileOnly(source);
                } catch (CompileException e) {
                    Envoys.LOGGER.warn("[Envoys] Lua compile error for NPC {} uploaded by {}: {}",
                            payload.npcId(), player.getName().getString(), e.getMessage());
                    replyLua(player, false, "§cОшибка компиляции: " + e.getMessage());
                    return;
                } catch (LuaError e) {
                    Envoys.LOGGER.warn("[Envoys] Lua setup error for NPC {} uploaded by {}",
                            payload.npcId(), player.getName().getString(), e);
                    replyLua(player, false, "§cОшибка подготовки Lua: " + e.getMessage());
                    return;
                }

                if (!LuaScriptStore.writeScript(payload.npcId(), source)) {
                    replyLua(player, false, "§cНе удалось сохранить скрипт на диск.");
                    return;
                }

                LuaNpcEngine engine = LuaEngineManager.createOrRestart(payload.npcId());
                if (engine != null && engine.isErrored()) {
                    replyLua(player, true, "§eСкрипт сохранён, но движок упал при запуске: " + engine.errorText());
                } else {
                    replyLua(player, true, "§aСкрипт '" + fileName + "' сохранён, движок перезапущен.");
                }

                Envoys.LOGGER.info("[Envoys] Lua script '{}' saved for NPC {} by {}",
                        fileName, payload.npcId(), player.getName().getString());
            } catch (Exception e) {
                Envoys.LOGGER.error("[Envoys] Failed to process Lua script upload for NPC {}", payload.npcId(), e);
                replyLua(player, false, "§cВнутренняя ошибка при сохранении скрипта.");
            }
        }).exceptionally(e -> {
            Envoys.LOGGER.error("[Envoys] Unhandled error during Lua script upload for NPC {}", payload.npcId(), e);
            ServerPlayer player = playerRef[0];
            if (player != null) {
                replyLua(player, false, "§cВнутренняя ошибка при сохранении скрипта.");
            }
            return null;
        });
    }

    private static void replyLua(ServerPlayer player, boolean success, String message) {
        player.connection.send(new LuaScriptUploadResultPayload(success, message));
    }

    private static BaseNPC findLuaNpc(MinecraftServer server, UUID npcId) {
        if (server == null || npcId == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(npcId) instanceof BaseNPC npc) {
                return npc;
            }
        }
        return null;
    }

    private static boolean containsForbiddenActions(NPCScriptData scriptData) {
        if (scriptData.events == null) {
            return false;
        }
        for (NpcEventData event : scriptData.events.values()) {
            if (event == null || event.actions() == null) {
                continue;
            }
            for (EntityActionData action : event.actions()) {
                if (action == null) {
                    continue;
                }
                String type = action.getType();
                if ("command".equalsIgnoreCase(type) || "trade".equalsIgnoreCase(type)) {
                    return true;
                }
                if (action instanceof ActionCommand || action instanceof ActionTrade) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void handleSelectDialogAnswer(final SelectDialogAnswerPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ServerLevel level = ((ServerPlayer) player).serverLevel();
            Entity entity = level.getEntity(payload.npcUuid());
            if (entity == null || player.distanceToSqr(entity) > 64.0D) {
                return;
            }

            ScriptRunner runner = ScriptRunner.RUNNERS.get(payload.npcUuid());
            if (runner != null && runner.targetPlayer() == player) {
                runner.onDialogAnswer(payload.nextActionId());
                return;
            }

            LuaNpcEngine engine = LuaEngineManager.getEngine(payload.npcUuid());
            if (engine != null) {
                engine.onDialogAnswer(player.getUUID(), payload.nextActionId());
            }
        });
    }

    public static void handleTradeAll(final TradeAllPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof NPCMerchantMenu menu)) return;
            if (!menu.stillValid(player)) return;
            TradeAllExecutor.execute(player, menu, payload.shopItem());
        });
    }

    public static void handleSaveNPCPassport(final SaveNPCPassportPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            if (!hasTunerAccess(player)) {
                Envoys.LOGGER.warn("[Envoys] Player {} tried to save NPC passport without tuner!", player.getName().getString());
                return;
            }

            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(payload.npcUuid());

            if (entity != null && player.distanceToSqr(entity) > 64.0D) {
                Envoys.LOGGER.warn("[Envoys] Player {} tried to edit NPC {} from too far away!", player.getName().getString(), payload.npcUuid());
                return;
            }

            NPCInteractManager manager = NPCInteractManager.byUUID(payload.npcUuid()).orElse(null);
            if (manager == null) {
                Envoys.LOGGER.warn("[Envoys] Passport save for unknown NPC {} ignored", payload.npcUuid());
                return;
            }

            boolean isOp = player.hasPermissions(4);
            NPCPassportData p = manager.passport;

            if (!isOp && p.creativeTunerOnly) {
                Envoys.LOGGER.warn("[Envoys] Player {} tried to modify creative-only NPC passport!", player.getName().getString());
                return;
            }

            if (isOp) {
                p.npcName = sanitizeName(payload.name());
                p.size = payload.size();
                p.speed = payload.speed();
                p.hp = payload.hp();
                p.holdX = payload.holdPosition().x;
                p.holdY = payload.holdPosition().y;
                p.holdZ = payload.holdPosition().z;
                p.isVisible = payload.isVisible();
                p.isHoldPosEnabled = payload.isHoldPosEnabled();
                p.canTakeDamage = payload.canTakeDamage();
                p.useGravity = payload.useGravity();
                p.creativeTunerOnly = payload.creativeTunerOnly();
                p.lookLocked = payload.lookLocked();
                p.emote = payload.emote();
            } else {

                p.npcName = sanitizeName(payload.name());
                p.isHoldPosEnabled = payload.isHoldPosEnabled();
                p.lookLocked = payload.lookLocked();

                if (Float.isFinite(payload.size())) {
                    p.size = Math.clamp(0.1f, payload.size(), 5.0f);
                }

                if (p.holdX == 0 && p.holdY == 0 && p.holdZ == 0 && entity != null) {
                    p.holdX = entity.getX();
                    p.holdY = entity.getY();
                    p.holdZ = entity.getZ();
                }
                p.emote = payload.emote();
            }

            NPCConfigManager.save(manager);

            if (entity instanceof BaseNPC npc) {
                npc.applyPassportData(p);
            }

            Envoys.LOGGER.info("[Envoys] Passport successfully saved & applied for NPC {}", payload.npcUuid());
        });
    }

    private static String sanitizeName(String input) {
        if (input == null || input.isBlank()) return "Steve";
        String trimmed = input.trim();
        return trimmed.length() > 50 ? trimmed.substring(0, 50) : trimmed;
    }
}