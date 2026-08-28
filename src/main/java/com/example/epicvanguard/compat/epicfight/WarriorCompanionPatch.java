package com.example.epicvanguard.compat.epicfight;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

public class WarriorCompanionPatch extends HumanoidMobPatch<WarriorCompanionEntity> {

    public WarriorCompanionPatch() {
        super(Factions.NEUTRAL);
    }

    public static void initAttributes(EntityAttributeModificationEvent event) {
        event.add(com.example.epicvanguard.init.ModEntityTypes.WARRIOR_COMPANION.get(), EpicFightAttributes.IMPACT.get(), 0.5D);
        event.add(com.example.epicvanguard.init.ModEntityTypes.WARRIOR_COMPANION.get(), EpicFightAttributes.WEIGHT.get(), 20.0D);
        event.add(com.example.epicvanguard.init.ModEntityTypes.WARRIOR_COMPANION.get(), EpicFightAttributes.STUN_ARMOR.get(), 0.0D);
        event.add(com.example.epicvanguard.init.ModEntityTypes.WARRIOR_COMPANION.get(), EpicFightAttributes.MAX_STAMINA.get(), 15.0D);
        event.add(com.example.epicvanguard.init.ModEntityTypes.WARRIOR_COMPANION.get(), EpicFightAttributes.STAMINA_REGEN.get(), 1.0D);
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, Animations.BIPED_IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, Animations.BIPED_WALK);
        animator.addLivingAnimation(LivingMotions.CHASE, Animations.BIPED_RUN);
        animator.addLivingAnimation(LivingMotions.FALL, Animations.BIPED_FALL);
        animator.addLivingAnimation(LivingMotions.MOUNT, Animations.BIPED_MOUNT);
        animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_DEATH);
    }

    @Override
    public void updateMotion(boolean isClientSide) {
        this.commonAggressiveMobUpdateMotion(isClientSide);
    }

    @Override
    public boolean isTargetInvulnerable(Entity target) {
        if (this.original != null && this.original.isRecruited() && !this.original.isDuelMode() && this.original.getOwnerUUID().isPresent()) {
            if (target != null && target.getUUID().equals(this.original.getOwnerUUID().get())) {
                return true; // Immune to Epic Fight attacks and sweep collisions
            }
        }
        return super.isTargetInvulnerable(target);
    }
}
