package io.github.raffasanttos.epicvanguard.client.renderer;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

/**
 * Renderer do guerreiro: modelo humanoide com a skin do SKIN_ID sincronizado
 * (assets/epicvanguard/textures/entity/warrior/warrior_skin_N.png).
 * Com o Epic Fight instalado, quem desenha é o renderer "patched" do Epic Fight.
 */
public class WarriorCompanionRenderer extends HumanoidMobRenderer<WarriorCompanionEntity, HumanoidModel<WarriorCompanionEntity>> {

    public WarriorCompanionRenderer(EntityRendererProvider.Context pContext) {
        // HumanoidMobRenderer já adiciona a camada do item na mão (antes ela era adicionada de novo e o item era desenhado duas vezes)
        super(pContext, new HumanoidModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(pContext.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(pContext.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                pContext.getModelManager()
        ));
    }

    @Override
    public void render(WarriorCompanionEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        HumanoidModel<WarriorCompanionEntity> model = this.getModel();
        ItemStack main = entity.getMainHandItem();
        ItemStack off = entity.getOffhandItem();
        HumanoidModel.ArmPose rightPose = main.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        HumanoidModel.ArmPose leftPose = off.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;

        // Pose de braço para arco puxado, besta, escudo erguido e comer/beber
        if (entity.isUsingItem()) {
            HumanoidModel.ArmPose usePose = switch (entity.getUseItem().getUseAnimation()) {
                case BOW -> HumanoidModel.ArmPose.BOW_AND_ARROW;
                case BLOCK -> HumanoidModel.ArmPose.BLOCK;
                case CROSSBOW -> HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                case SPEAR -> HumanoidModel.ArmPose.THROW_SPEAR;
                default -> HumanoidModel.ArmPose.ITEM;
            };
            if (entity.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                rightPose = usePose;
            } else {
                leftPose = usePose;
            }
        } else if (main.getItem() instanceof CrossbowItem && entity.isAggressive()) {
            rightPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }

        model.rightArmPose = rightPose;
        model.leftArmPose = leftPose;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WarriorCompanionEntity entity) {
        int skinId = entity.getSkinId();
        skinId = Math.max(0, Math.min(skinId, WarriorCompanionEntity.SKIN_COUNT - 1));
        return new ResourceLocation(io.github.raffasanttos.epicvanguard.EpicVanguardMod.MOD_ID, "textures/entity/warrior/warrior_skin_" + (skinId + 1) + ".png");
    }
}
