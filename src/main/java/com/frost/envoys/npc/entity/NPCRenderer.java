package com.frost.envoys.npc.entity;

import com.frost.envoys.client.EmoteIntegration;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class NPCRenderer extends HumanoidMobRenderer<BaseNPC, NPCModel<BaseNPC>> {
    private static final ResourceLocation STEVE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    private final NPCModel<BaseNPC> wideModel;
    private final NPCModel<BaseNPC> slimModel;

    public NPCRenderer(EntityRendererProvider.Context context) {
        super(context, new NPCModel<>(context.bakeLayer(NPCModel.LAYER_LOCATION), false), 0.5F);
        this.wideModel = this.model;
        this.slimModel = new NPCModel<>(context.bakeLayer(NPCModel.LAYER_LOCATION_SLIM), true);
    }

    @Override
    public void render(BaseNPC entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        String skinModel = entity.getSkinModel();
        boolean isSlim = "slim".equalsIgnoreCase(skinModel) || "alex".equalsIgnoreCase(skinModel);

        this.model = isSlim ? this.slimModel : this.wideModel;

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BaseNPC entity) {
        ResourceLocation customLocation = entity.getClientSkinLocation();
        if (customLocation != null) {
            return customLocation;
        }
        return STEVE_TEXTURE;
    }
        
    @Override
    protected void setupRotations(BaseNPC entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, float scale) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks, scale);

        EmoteIntegration.applyRootTransform(entity, poseStack, partialTicks);
    }
}