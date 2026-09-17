package com.frost.envoys.network;

import java.util.ArrayList;
import java.util.List;

import com.frost.envoys.Envoys;
import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.NPCPassportData;
import com.frost.envoys.action.NPCScriptData;
import com.frost.envoys.action.serialization.EntityActionAdapter;
import com.frost.envoys.client.quest.ClientQuestTracker;
import com.frost.envoys.gui.screen.NPCDialogScreen;
import com.frost.envoys.gui.screen.NPCDialogScreen.DialogOption;
import com.frost.envoys.gui.screen.NPCConfigScreen;
import com.frost.envoys.network.payload.OpenDialogPayload;
import com.frost.envoys.network.payload.OpenSettingGuiPayload;
import com.frost.envoys.network.payload.SelectDialogAnswerPayload;
import com.frost.envoys.network.payload.SyncPlayerQuestsPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {

    public static void handleOpenSettingGui(final OpenSettingGuiPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {

            NPCInteractManager manager = NPCInteractManager.byUUID(payload.entityId())
                    .orElseGet(() -> new NPCInteractManager(payload.entityId()));

            NPCPassportData p = manager.passport;
            p.npcName = payload.name();
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

            NPCInteractManager screenManager = manager;

            if (payload.jsonScript() != null && !payload.jsonScript().isEmpty()) {
                try {
                    NPCScriptData scriptData = EntityActionAdapter.GSON.fromJson(payload.jsonScript(), NPCScriptData.class);
                    if (scriptData != null) {
                        boolean sharesJvmWithServer = Minecraft.getInstance().hasSingleplayerServer();

                        if (sharesJvmWithServer) {
                            screenManager = new NPCInteractManager(payload.entityId());
                            screenManager.passport = p;
                            NPCInteractManager.SCRIPTS.put(payload.entityId(), manager);
                        }

                        scriptData.applyTo(screenManager);
                    }
                } catch (Exception e) {
                    Envoys.LOGGER.error("[Envoys] Failed to parse received NPC script on client", e);
                }
            }

            Minecraft.getInstance().setScreen(new NPCConfigScreen(screenManager, payload.isCreativeTuner()));
        });
    }

    public static void handleOpenDialog(final OpenDialogPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            List<DialogOption> options = new ArrayList<>();

            payload.answers().forEach((answerText, nextActionId) -> {
                options.add(new DialogOption(
                    Component.literal(answerText),
                    () -> PacketDistributor.sendToServer(new SelectDialogAnswerPayload(payload.npcUuid(), nextActionId))
                ));
            });

            String title = "NPC";
            NPCInteractManager manager = NPCInteractManager.byUUID(payload.npcUuid()).orElse(null);
            if (manager != null && manager.passport != null && !manager.passport.npcName.isBlank()) {
                title = manager.passport.npcName;
            } else if (payload.npcName() != null && !payload.npcName().isBlank()) {
                title = payload.npcName();
            }

            Minecraft.getInstance().setScreen(new NPCDialogScreen(
                Component.literal(title),
                payload.npcMessage(),
                options,
                payload.npcUuid()
            ));
        });
    }

    public static void handleSyncPlayerQuests(final SyncPlayerQuestsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestTracker.get().replace(payload.quests()));
    }
}