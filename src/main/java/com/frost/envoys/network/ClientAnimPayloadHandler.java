package com.frost.envoys.network;

import com.frost.envoys.Envoys;
import com.frost.envoys.client.EmoteIntegration;
import com.frost.envoys.network.payload.AnimDataPayload;
import com.frost.envoys.network.payload.AnimListPayload;
import com.frost.envoys.skin.service.SkinCacheService;
import com.frost.envoys.util.ClientPathManager;
import com.frost.envoys.util.PathManager;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ClientAnimPayloadHandler {

    public static void handleAnimList(final AnimListPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.animations() == null) return;
            for (AnimListPayload.AnimInfo info : payload.animations()) {
                Path cached = ClientPathManager.getClientAnimDir().resolve(EmoteIntegration.sanitizeAnimName(info.name()) + ".json");
                if (Files.exists(cached)) {
                    try {
                        byte[] data = Files.readAllBytes(cached);
                        if (SkinCacheService.calculateSHA256(data).equalsIgnoreCase(info.hash())) {
                            registerFromCache(info.name(), data);
                            continue;
                        }
                    } catch (Exception ignored) {
                    }
                }
                EmoteIntegration.requestIfMissing(info.name());
            }
        });
    }

    public static void handleAnimData(final AnimDataPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                byte[] data = payload.jsonData();
                if (data == null || data.length == 0) {
                    EmoteIntegration.finishRequest(payload.name());
                    return;
                }

                Path file = ClientPathManager.getClientAnimDir().resolve(EmoteIntegration.sanitizeAnimName(payload.name()) + ".json");
                Files.write(file, data);

                if (EmoteIntegration.isLibPresent()) {
                    registerFromCache(payload.name(), data);
                }
            } catch (Exception e) {
                EmoteIntegration.finishRequest(payload.name());
                Envoys.LOGGER.error("[Envoys] Ошибка сохранения анимации с сервера", e);
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
            Envoys.LOGGER.error("[Envoys] Ошибка парсинга анимации '{}'", name, t);
        }
    }
}