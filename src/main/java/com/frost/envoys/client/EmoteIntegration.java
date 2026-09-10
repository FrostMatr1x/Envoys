package com.frost.envoys.client;

import com.frost.envoys.Envoys;
import com.frost.envoys.network.payload.RequestAnimListPayload;
import com.frost.envoys.network.payload.RequestAnimPayload;
import com.frost.envoys.npc.entity.BaseNPC;
import com.frost.envoys.npc.entity.NPCModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.kosmx.playerAnim.api.IPlayer;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.AnimationContainer;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.impl.IMutableModel;
import dev.kosmx.playerAnim.impl.animation.AnimationApplier;
import dev.kosmx.playerAnim.impl.animation.IBendHelper;
import io.github.kosmx.emotes.api.events.client.ClientEmoteAPI;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class EmoteIntegration {

    private static final Map<UUID, AnimationContainer<KeyframeAnimationPlayer>> STATE = new ConcurrentHashMap<>();
    private static final Map<UUID, String> ACTIVE = new ConcurrentHashMap<>();
    private static final Set<UUID> WARNED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, KeyframeAnimationPlayer> PREVIEWS = new ConcurrentHashMap<>();

    private static final Map<String, KeyframeAnimation> SERVER_ANIMS = new ConcurrentHashMap<>();
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger SERVER_LIST_VERSION = new AtomicInteger(0);

    private EmoteIntegration() {
    }

    private static boolean libPresent() {
        try {
            return ModList.get().isLoaded("emotecraft");
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isLibPresent() {
        return libPresent();
    }

    public static int serverListVersion() {
        return SERVER_LIST_VERSION.get();
    }

    public static String formatNameObject(Object nameObj) {
        if (nameObj == null) return null;
        if (nameObj instanceof Component component) {
            return component.getString();
        }
        if (nameObj instanceof String str) {
            if (str.startsWith("{") && str.endsWith("}")) {
                try {
                    com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(str).getAsJsonObject();
                    if (json.has("text")) return json.get("text").getAsString();
                    if (json.has("fallback")) return json.get("fallback").getAsString();
                    if (json.has("translate")) return json.get("translate").getAsString();
                } catch (Exception ignored) {}
            }
            return str;
        }
        return String.valueOf(nameObj);
    }

    public static void registerAnim(String name, KeyframeAnimation anim) {
        if (name == null || name.isBlank() || anim == null) {
            if (name != null) REQUESTED.remove(name);
            return;
        }
        SERVER_ANIMS.put(name, anim);
        REQUESTED.remove(name);
        SERVER_LIST_VERSION.incrementAndGet();
    }

    public static void finishRequest(String name) {
        if (name != null) {
            REQUESTED.remove(name);
        }
    }

    public static String sanitizeAnimName(String input) {
        if (input == null || input.isBlank()) return "unknown";
        String name = input;
        if (name.toLowerCase().endsWith(".json")) {
            name = name.substring(0, name.length() - 5);
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static void requestServerList() {
        try {
            REQUESTED.clear();
            PacketDistributor.sendToServer(new RequestAnimListPayload());
        } catch (Throwable t) {
            Envoys.LOGGER.error("[Envoys] EmoteIntegration.requestServerList failed", t);
        }
    }

    public static void requestIfMissing(String emoteName) {
        if (emoteName == null || emoteName.isBlank()) return;
        if (findServerAnim(emoteName) != null) return;
        if (!REQUESTED.add(emoteName)) return;
        try {
            PacketDistributor.sendToServer(new RequestAnimPayload(emoteName));
        } catch (Throwable t) {
            REQUESTED.remove(emoteName);
            Envoys.LOGGER.error("[Envoys] EmoteIntegration.requestIfMissing failed", t);
        }
    }

    private static KeyframeAnimation findServerAnim(String emoteName) {
        if (emoteName == null || emoteName.isBlank()) return null;

        // 1. Подача по точному имени или имени без учёта регистра
        KeyframeAnimation exact = SERVER_ANIMS.get(emoteName);
        if (exact != null) return exact;

        for (Map.Entry<String, KeyframeAnimation> entry : SERVER_ANIMS.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(emoteName)) {
                return entry.getValue();
            }
        }

        // 2. Запасной вариант: поиск по внутренним метаданным объекта анимации
        for (KeyframeAnimation anim : SERVER_ANIMS.values()) {
            Object extraNameObj = anim.extraData.get("name");
            if (extraNameObj != null) {
                String internalName = formatNameObject(extraNameObj);
                if (internalName != null && internalName.equalsIgnoreCase(emoteName)) {
                    return anim;
                }
            }
        }

        return null;
    }

    public static void applyRootTransform(BaseNPC entity, PoseStack poseStack, float partialTick) {
        if (!libPresent()) return;
        try {
            AnimationContainer<KeyframeAnimationPlayer> container = STATE.get(entity.getUUID());
            if (container == null || container.getAnim() == null || !container.isActive()) return;

            AnimationApplier applier = new AnimationApplier(container);
            applier.setTickDelta(partialTick);

            Vec3f pos = applier.get3DTransform("torso", TransformType.POSITION, Vec3f.ZERO);
            if (pos.equals(Vec3f.ZERO)) {
                pos = applier.get3DTransform("body", TransformType.POSITION, Vec3f.ZERO);
            }
            if (pos.equals(Vec3f.ZERO)) {
                pos = applier.get3DTransform("root", TransformType.POSITION, Vec3f.ZERO);
            }

            Vec3f rot = applier.get3DTransform("torso", TransformType.ROTATION, Vec3f.ZERO);
            if (rot.equals(Vec3f.ZERO)) {
                rot = applier.get3DTransform("body", TransformType.ROTATION, Vec3f.ZERO);
            }

            if (pos.getX() != 0 || pos.getY() != 0 || pos.getZ() != 0) {
                poseStack.translate(pos.getX() / 16.0F, -pos.getY() / 16.0F, pos.getZ() / 16.0F);
            }

            if (rot.getZ() != 0) {
                poseStack.mulPose(Axis.ZP.rotation(rot.getZ()));
            }
            if (rot.getY() != 0) {
                poseStack.mulPose(Axis.YP.rotation(rot.getY()));
            }
            if (rot.getX() != 0) {
                poseStack.mulPose(Axis.XP.rotation(rot.getX()));
            }

        } catch (Throwable t) {
            Envoys.LOGGER.error("[Envoys] EmoteIntegration.applyRootTransform failed", t);
        }
    }

    private static KeyframeAnimation findAnimation(String emoteName) {
        if (emoteName == null || emoteName.isBlank()) {
            return null;
        }
        KeyframeAnimation server = findServerAnim(emoteName);
        if (server != null) {
            return server;
        }
        Collection<KeyframeAnimation> list = ClientEmoteAPI.clientEmoteList();
        if (list == null) {
            return null;
        }
        for (KeyframeAnimation anim : list) {
            Object nameObj = anim.extraData.get("name");
            if (nameObj != null) {
                String parsedName = formatNameObject(nameObj);
                if (parsedName != null && parsedName.equalsIgnoreCase(emoteName)) {
                    return anim;
                }
            }
        }
        return null;
    }

    public static List<String> clientEmoteNames() {
        List<String> names = new ArrayList<>();
        if (!libPresent()) return names;
        try {
            Set<String> dedup = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            Collection<KeyframeAnimation> list = ClientEmoteAPI.clientEmoteList();
            if (list != null) {
                for (KeyframeAnimation anim : list) {
                    Object nameObj = anim.extraData.get("name");
                    if (nameObj != null) {
                        String name = formatNameObject(nameObj);
                        if (name != null && !name.isBlank()) {
                            dedup.add(name);
                        }
                    }
                }
            }
            dedup.addAll(SERVER_ANIMS.keySet());
            names.addAll(dedup);
        } catch (Throwable t) {
            Envoys.LOGGER.error("[Envoys] EmoteIntegration.clientEmoteNames failed", t);
        }
        return names;
    }

    public static boolean playPreview(AbstractClientPlayer player, String emoteName) {
        if (player == null || !libPresent()) return false;
        try {
            KeyframeAnimation found = findAnimation(emoteName);
            if (found == null) return false;
            stopPreview(player);
            KeyframeAnimationPlayer animPlayer = new KeyframeAnimationPlayer(found);
            PREVIEWS.put(player.getUUID(), animPlayer);
            if (player instanceof IPlayer iPlayer) {
                iPlayer.getAnimationStack().addAnimLayer(0, animPlayer);
            }
            return true;
        } catch (Throwable t) {
            Envoys.LOGGER.error("[Envoys] EmoteIntegration.playPreview failed", t);
            return false;
        }
    }

    public static void tickPreview(AbstractClientPlayer player) {
        if (player == null) return;
        KeyframeAnimationPlayer preview = PREVIEWS.get(player.getUUID());
        if (preview != null && preview.isActive()) {
            try {
                preview.tick();
            } catch (Throwable t) {
                Envoys.LOGGER.error("[Envoys] EmoteIntegration.tickPreview failed", t);
            }
        }
    }

    public static void stopPreview(AbstractClientPlayer player) {
        if (player == null) return;
        KeyframeAnimationPlayer preview = PREVIEWS.remove(player.getUUID());
        if (preview != null) {
            try {
                if (player instanceof IPlayer iPlayer) {
                    iPlayer.getAnimationStack().removeLayer(preview);
                }
            } catch (Throwable t) {
                Envoys.LOGGER.error("[Envoys] EmoteIntegration.stopPreview failed", t);
            }
        }
    }

    public static void tickClient(BaseNPC entity) {
        if (!libPresent()) return;
        try {
            UUID uuid = entity.getUUID();

            if (!entity.isAlive()) {
                STATE.remove(uuid);
                ACTIVE.remove(uuid);
                WARNED.remove(uuid);
                return;
            }

            String emoteName = entity.getEmoteType();
            if (emoteName == null || emoteName.isBlank()) {
                STATE.remove(uuid);
                ACTIVE.remove(uuid);
                WARNED.remove(uuid);
                return;
            }

            requestIfMissing(emoteName);

            AnimationContainer<KeyframeAnimationPlayer> container =
                    STATE.computeIfAbsent(uuid, k -> new AnimationContainer<>());

            String currentName = ACTIVE.get(uuid);
            if (container.getAnim() == null || !emoteName.equalsIgnoreCase(currentName)) {
                KeyframeAnimation found = findAnimation(emoteName);

                if (found != null) {
                    container.setAnim(new KeyframeAnimationPlayer(found));
                } else {
                    container.setAnim(null);
                    if (WARNED.add(uuid)) {
                        Envoys.LOGGER.warn("[Envoys] Emote '{}' not found for NPC {}; no animation will play.", emoteName, uuid);
                    }
                }
                ACTIVE.put(uuid, emoteName);
            }

            if (container.getAnim() != null) {
                container.tick();
                if (!container.getAnim().isActive()) {
                    KeyframeAnimation found = findAnimation(emoteName);
                    if (found != null) {
                        container.setAnim(new KeyframeAnimationPlayer(found));
                    }
                }
            }
        } catch (Throwable t) {
            Envoys.LOGGER.error("[Envoys] EmoteIntegration.tickClient failed", t);
        }
    }

    public static void prepareRender(BaseNPC entity, float partialTick, NPCModel<?> model) {
        if (!libPresent()) return;
        try {
            if (!(model instanceof IMutableModel mutableModel)) return;

            AnimationContainer<KeyframeAnimationPlayer> container = STATE.get(entity.getUUID());

            if (container == null || container.getAnim() == null || !container.isActive()) {
                mutableModel.getEmoteSupplier().set(null);
                IBendHelper.INSTANCE.bend(model.body, null);
                IBendHelper.INSTANCE.bend(model.leftArm, null);
                IBendHelper.INSTANCE.bend(model.rightArm, null);
                IBendHelper.INSTANCE.bend(model.leftLeg, null);
                IBendHelper.INSTANCE.bend(model.rightLeg, null);
                return;
            }

            AnimationApplier applier = new AnimationApplier(container);
            applier.setTickDelta(partialTick);
            mutableModel.getEmoteSupplier().set(applier);

            applier.updatePart("torso", model.body);
            applier.updatePart("head", model.head);
            applier.updatePart("leftArm", model.leftArm);
            applier.updatePart("rightArm", model.rightArm);
            applier.updatePart("leftLeg", model.leftLeg);
            applier.updatePart("rightLeg", model.rightLeg);

            if (model.hat != null) model.hat.copyFrom(model.head);
            if (model.jacket != null) model.jacket.copyFrom(model.body);
            if (model.leftSleeve != null) model.leftSleeve.copyFrom(model.leftArm);
            if (model.rightSleeve != null) model.rightSleeve.copyFrom(model.rightArm);
            if (model.leftPants != null) model.leftPants.copyFrom(model.leftLeg);
            if (model.rightPants != null) model.rightPants.copyFrom(model.rightLeg);

        } catch (Throwable t) {
            Envoys.LOGGER.error("[Envoys] EmoteIntegration.prepareRender failed", t);
        }
    }
}