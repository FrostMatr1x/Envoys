package com.frost.envoys.network;

import com.frost.envoys.EnvoysConstants;
import com.frost.envoys.gui.screen.NPCSkinScreen;
import com.frost.envoys.network.payload.RequestSkinPayload;
import com.frost.envoys.network.payload.SkinConfirmedPayload;
import com.frost.envoys.network.payload.SkinDataPayload;
import com.frost.envoys.network.payload.SkinInfoPayload;
import com.frost.envoys.skin.gui.SkinGuiPreview;
import com.frost.envoys.skin.model.SkinMetaData;
import com.frost.envoys.skin.service.SkinCacheService;
import com.frost.envoys.util.ClientPathManager;
import com.frost.envoys.util.PathManager;
import com.frost.envoys.util.SkinModelUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;

public class ClientSkinPayloadHandler {

    public static void handleSkinInfo(final SkinInfoPayload payload, final NetworkContext context) {
        context.enqueueWork(() -> {
            String uuidStr = payload.npcUuid().toString();
            Path cacheDir = ClientPathManager.getClientSkinCacheDir();
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
                        EnvoysConstants.LOGGER.info("[Envoys] Skin {} loaded from the client local cache (hash matched)", uuidStr);
                        return;
                    } catch (Exception e) {
                        EnvoysConstants.LOGGER.error("Failed to read cached skin", e);
                    }
                }
            }

            Network.sendToServer(new RequestSkinPayload(payload.npcUuid()));
        });
    }

    public static void handleSkinData(final SkinDataPayload payload, final NetworkContext context) {
        context.enqueueWork(() -> {
            String uuidStr = payload.npcUuid().toString();
            byte[] pngData = payload.pngData();

            try {
                String hash = SkinCacheService.calculateSHA256(pngData);
                String model = SkinModelUtil.detectModel(pngData);

                SkinCacheService.saveToCache(
                        ClientPathManager.getClientSkinCacheDir(),
                        uuidStr,
                        pngData,
                        new SkinMetaData(uuidStr, hash, model, "server", System.currentTimeMillis())
                );

                if (Minecraft.getInstance().screen instanceof NPCSkinScreen skinScreen) {
                    skinScreen.onSkinDataReceived(pngData);
                }

                EnvoysConstants.LOGGER.info("[Envoys] Skin {} received from the server and updated in the cache", uuidStr);

            } catch (Exception e) {
                EnvoysConstants.LOGGER.error("[Envoys] Failed to save the skin received from the server", e);
            }
        });
    }

    public static void handleSkinConfirmed(final SkinConfirmedPayload payload, final NetworkContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof NPCSkinScreen skinScreen) {
                skinScreen.onSkinConfirmed(payload.hash());
            }
        });
    }
}