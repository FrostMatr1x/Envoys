package com.frost.envoys.npc.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;

import java.util.UUID;

public class MannequinEntity extends RemotePlayer {

    private boolean isSlim = false;
    private ResourceLocation customTexture;

    public MannequinEntity(ClientLevel level) {
        super(level, new GameProfile(UUID.randomUUID(), "Mannequin"));

        for (PlayerModelPart part : PlayerModelPart.values()) {
            this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION,
                    (byte) (this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION) | part.getMask()));
        }
    }

    public boolean isSlim() {
        return isSlim;
    }

    public void setSlim(boolean slim) {
        this.isSlim = slim;
    }

    public ResourceLocation getCustomTexture() {
        return customTexture;
    }

    public void setCustomTexture(ResourceLocation customTexture) {
        this.customTexture = customTexture;
    }

    @Override
    public PlayerSkin getSkin() {
        PlayerSkin.Model model = isSlim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE;
        ResourceLocation texture = (customTexture != null)
                ? customTexture
                : DefaultPlayerSkin.get(getUUID()).texture();

        return new PlayerSkin(
                texture,
                null,
                null,
                null,
                model,
                true
        );
    }

    @Override
    public boolean isModelPartShown(PlayerModelPart part) {
        return true;
    }
}