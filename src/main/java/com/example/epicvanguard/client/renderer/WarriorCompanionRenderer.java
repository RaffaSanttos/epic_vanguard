package com.example.epicvanguard.client.renderer;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for WarriorCompanionEntity.
 * Uses the standard humanoid model and renders the warrior's skin
 * based on the SKIN_ID stored in the entity's synced data.
 * Skins are loaded from: assets/magicmod/textures/entity/warrior/warrior_skin_N.png
 */
public class WarriorCompanionRenderer extends HumanoidMobRenderer<WarriorCompanionEntity, HumanoidModel<WarriorCompanionEntity>> {

    public WarriorCompanionRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HumanoidModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(pContext.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(pContext.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                pContext.getModelManager()
        ));
        this.addLayer(new ItemInHandLayer<>(this, pContext.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(WarriorCompanionEntity entity) {
        int skinId = entity.getSkinId();
        skinId = Math.max(0, Math.min(skinId, WarriorCompanionEntity.SKIN_COUNT - 1));
        return new ResourceLocation(com.example.epicvanguard.EpicVanguardMod.MOD_ID, "textures/entity/warrior/warrior_skin_" + (skinId + 1) + ".png");
    }
}
