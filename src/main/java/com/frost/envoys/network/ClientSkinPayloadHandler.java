package com.frost.envoys.network;

import com.frost.envoys.Envoys;
import com.frost.envoys.gui.screen.NPCSkinScreen;
import com.frost.envoys.network.payload.RequestSkinPayload;
import com.frost.envoys.network.payload.SkinConfirmedPayload;
import com.frost.envoys.network.payload.SkinDataPayload;
import com.frost.envoys.network.payload.SkinInfoPayload;
import com.frost.envoys.skin.gui.SkinGuiPreview;
import com.frost.envoys.skin.model.SkinMetaData;
import com.frost.envoys.skin.service.SkinCacheService;
import com.frost.envoys.util.PathManager;
import com.frost.envoys.util.SkinModelUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.nio.file.Files;
import java.nio.file.Path;

public class ClientSkinPayloadHandler {

    public static void handleSkinInfo(final SkinInfoPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            String uuidStr = payload.npcUuid().toString();
            Path cacheDir = PathManager.getClientSkinCacheDir();
            Path cachedPng = cacheDir.resolve(uuidStr + ".png");

            if (Files.exists(cachedPng)) {
                String localHash = SkinCacheService.calculateSHA256(cachedPng);
                if (localHash.equalsIgnoreCase(payload.hash())) {
                    try {
                        byte[] localBytes = Files.readAllBytes(cachedPng);
                        ResourceLocation location = SkinGuiPreview.registerDynamicSkin(uuidStr, localBytes);

                        if (Minecraft.getInstance().screen instanceof NPCSkinScreen skinScreen) {
                            skinScreen.onSkinDataReceived(localBytes);
                        }
                        Envoys.LOGGER.info("[Envoys] Скин {} загружен из локального кэша клиента (хэш совпал)", uuidStr);
                        return;
                    } catch (Exception e) {
                        Envoys.LOGGER.error("Ошибка чтения кэшированного скина", e);
                    }
                }
            }

            PacketDistributor.sendToServer(new RequestSkinPayload(payload.npcUuid()));
        });
    }

    public static void handleSkinData(final SkinDataPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            String uuidStr = payload.npcUuid().toString();
            byte[] pngData = payload.pngData();

            try {
                String hash = SkinCacheService.calculateSHA256(pngData);
                String model = SkinModelUtil.detectModel(pngData);

                SkinCacheService.saveToCache(
                        PathManager.getClientSkinCacheDir(),
                        uuidStr,
                        pngData,
                        new SkinMetaData(uuidStr, hash, model, "server", System.currentTimeMillis())
                );

                if (Minecraft.getInstance().screen instanceof NPCSkinScreen skinScreen) {
                    skinScreen.onSkinDataReceived(pngData);
                }

                Envoys.LOGGER.info("[Envoys] Скин {} получен от сервера и обновлен в кэше", uuidStr);

            } catch (Exception e) {
                Envoys.LOGGER.error("[Envoys] Ошибка сохранения полученного скина с сервера", e);
            }
        });
    }

    public static void handleSkinConfirmed(final SkinConfirmedPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof NPCSkinScreen skinScreen) {
                skinScreen.onSkinConfirmed(payload.hash());
            }
        });
    }
}