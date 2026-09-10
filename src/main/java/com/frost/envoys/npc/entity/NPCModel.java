package com.frost.envoys.npc.entity;

import com.frost.envoys.Envoys;
import com.frost.envoys.client.EmoteIntegration;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

public class NPCModel<T extends BaseNPC> extends PlayerModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Envoys.MODID, "base_npc"), "main");
    
    public static final ModelLayerLocation LAYER_LOCATION_SLIM = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Envoys.MODID, "base_npc_slim"), "main");

    public NPCModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    public static LayerDefinition createBodyLayer(boolean slim) {
        return LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity == null) return;

        if (entity.getEmoteType() != null && !entity.getEmoteType().isBlank()) {
            this.head.yRot = 0;
            this.head.xRot = 0;
        }

        float partialTick = ageInTicks - (float) Math.floor(ageInTicks);
        EmoteIntegration.prepareRender(entity, partialTick, this);
    }
}