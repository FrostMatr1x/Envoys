package com.frost.envoys.network;

import com.frost.envoys.Envoys;
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
import com.frost.envoys.network.payload.SaveNPCPassportPayload;
import com.frost.envoys.network.payload.SaveNPCScriptPayload;
import com.frost.envoys.network.payload.SelectDialogAnswerPayload;
import com.frost.envoys.network.payload.TradeAllPayload;
import com.frost.envoys.npc.entity.BaseNPC;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {

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
                p.emote = payload.emote();
            } else {

                p.npcName = sanitizeName(payload.name());
                p.isHoldPosEnabled = payload.isHoldPosEnabled();

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