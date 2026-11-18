package com.frost.envoys.skin.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class SkinGuiPreview {

    public static ResourceLocation registerDynamicSkin(String identifier, byte[] pngData) {
        if (pngData == null || pngData.length == 0) {
            return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
        }

        NativeImage image = null;
        try (InputStream is = new ByteArrayInputStream(pngData)) {
            image = NativeImage.read(is);

            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    "envoys", 
                    "dynamic_skin/" + identifier.toLowerCase().replaceAll("[^a-z0-9_.-]", "_")
            );

            var textureManager = Minecraft.getInstance().getTextureManager();

            AbstractTexture oldTexture = textureManager.getTexture(location, null);
            if (oldTexture != null) {
                textureManager.release(location);
            }

            // DynamicTexture takes ownership of the NativeImage and closes it with the texture.
            DynamicTexture texture = new DynamicTexture(image);
            image = null;
            textureManager.register(location, texture);
            return location;
        } catch (Exception e) {
            return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
        } finally {
            if (image != null) {
                image.close();
            }
        }
    }
}