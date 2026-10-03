package com.frost.envoys.network;

import com.frost.envoys.Envoys;
import com.frost.envoys.client.EmoteIntegration;
import com.frost.envoys.network.payload.AnimDataPayload;
import com.frost.envoys.network.payload.AnimListPayload;
import com.frost.envoys.network.payload.AnimOpResultPayload;
import com.frost.envoys.network.payload.AnimRemovedPayload;
import com.frost.envoys.skin.service.SkinCacheService;
import com.frost.envoys.util.ClientPathManager;
import com.frost.envoys.util.PathManager;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClientAnimPayloadHandler {

    public static void handleAnimList(final AnimListPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.animations() == null) return;
            Map<String, String> index = new LinkedHashMap<>();
            for (AnimListPayload.AnimInfo info : payload.animations()) {
                if (info == null || info.name() == null) continue;
                index.put(info.name(), info.hash());
                Path cached = ClientPathManager.getClientAnimDir().resolve(EmoteIntegration.sanitizeAnimName(info.name()) + ".json");
                if (Files.exists(cached)) {
                    try {
                        byte[] data = Files.readAllBytes(cached);
                        if (SkinCacheService.calculateSHA256(data).equalsIgnoreCase(info.hash())) {
                            registerFromCache(info.name(), data);
                            continue;
                        }
                    } catch (Exception e) {
                        // C2: тихая потеря кэша недопустима — логируем и запрашиваем с сервера.
                        Envoys.LOGGER.debug("[Envoys] Failed to read cached animation '{}', requesting from server", info.name(), e);
                    }
                }
                EmoteIntegration.requestIfMissing(info.name());
            }
            EmoteIntegration.updateServerIndex(index);
        });
    }

    public static void handleAnimOpResult(final AnimOpResultPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            minecraft.player.sendSystemMessage(Component.translatable(payload.message(), payload.name()));
        });
    }

    public static void handleAnimRemoved(final AnimRemovedPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            String name = payload.name();
            if (name == null || name.isBlank()) return;
            EmoteIntegration.removeServerAnim(name);
            try {
                Path cached = ClientPathManager.getClientAnimDir().resolve(EmoteIntegration.sanitizeAnimName(name) + ".json");
                Files.deleteIfExists(cached);
            } catch (Exception e) {
                Envoys.LOGGER.warn("[Envoys] Failed to delete cached animation '{}'", name, e);
            }
        });
    }

    public static void handleAnimData(final AnimDataPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                byte[] data = payload.jsonData();
                if (data == null || data.length == 0) {
                    // B2: сервер ответил пустым файлом — анимации нет, помечаем без повторных запросов.
                    EmoteIntegration.markMissingOnServer(payload.name());
                    return;
                }

                if (!EmoteIntegration.isValidServerAnimDataSize(data)) {
                    EmoteIntegration.markMissingOnServer(payload.name());
                    Envoys.LOGGER.warn("[Envoys] Animation '{}' rejected: too large ({} bytes)", payload.name(), data.length);
                    return;
                }

                Path file = ClientPathManager.getClientAnimDir().resolve(EmoteIntegration.sanitizeAnimName(payload.name()) + ".json");
                Files.write(file, data);

                if (EmoteIntegration.isLibPresent()) {
                    registerFromCache(payload.name(), data);
                }
            } catch (Exception e) {
                EmoteIntegration.finishRequest(payload.name());
                Envoys.LOGGER.error("[Envoys] Failed to save the animation from the server", e);
            }
        });
    }

    @SuppressWarnings("deprecation")
    private static void registerFromCache(String name, byte[] data) {
        try {
            List<KeyframeAnimation> anims = AnimationSerializing.deserializeAnimation(new ByteArrayInputStream(data));
            if (anims == null || anims.isEmpty()) {
                EmoteIntegration.finishRequest(name);
                return;
            }
            KeyframeAnimation chosen = null;
            for (KeyframeAnimation anim : anims) {
                Object nameObj = anim.extraData.get("name");
                if (nameObj != null) {
                    String parsedName = EmoteIntegration.formatNameObject(nameObj);
                    if (parsedName != null && parsedName.equalsIgnoreCase(name)) {
                        chosen = anim;
                        break;
                    }
                }
            }
            if (chosen == null) {
                chosen = anims.get(0);
            }
            EmoteIntegration.registerAnim(name, chosen);
        } catch (Throwable t) {
            EmoteIntegration.finishRequest(name);
            Envoys.LOGGER.error("[Envoys] Failed to parse animation '{}'", name, t);
        }
    }
}