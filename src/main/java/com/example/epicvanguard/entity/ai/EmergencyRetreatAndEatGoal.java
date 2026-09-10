package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.entity.util.WarriorCombatHelper;
import com.example.epicvanguard.entity.util.WarriorHealingHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class EmergencyRetreatAndEatGoal extends Goal {
    private final WarriorCompanionEntity warrior;
    private int currentSlot = -1;
    private int eatingTicks = 0;
    private int totalUseDuration = 32;
    private int recalPathTicks = 0;

    public EmergencyRetreatAndEatGoal(WarriorCompanionEntity warrior) {
        this.warrior = warrior;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private int findBestConsumableSlot() {
        return WarriorHealingHelper.findBestConsumableSlot(warrior, true);
    }

    @Override
    public boolean canUse() {
        if (warrior.isTalking() || warrior.isDuelMode()) return false;
        boolean lowHp = warrior.getHealth() <= (warrior.getMaxHealth() * 0.40F);
        return (lowHp || warrior.isEmergencyRetreating()) && findBestConsumableSlot() != -1;
    }

    @Override
    public boolean canContinueToUse() {
        if (warrior.isTalking() || warrior.isDuelMode()) return false;
        if (warrior.getHealth() >= (warrior.getMaxHealth() * 0.75F)) return false;
        return findBestConsumableSlot() != -1;
    }

    @Override
    public void start() {
        warrior.setTarget(null);
        warrior.setLastHurtByMob(null);
        warrior.clearCombatTarget();
        warrior.setEmergencyRetreatCooldown(180);

        LivingEntity threat = warrior.getLastHurtByMob();
        if (threat == null || !threat.isAlive()) threat = warrior.getTarget();
        WarriorCombatHelper.performEmergencyDodgeRoll(warrior, threat != null ? threat.position() : null);

        currentSlot = findBestConsumableSlot();
        eatingTicks = 0;
        recalPathTicks = 0;

        if (currentSlot != -1) {
            ItemStack stack = warrior.getWarriorInventory().getItem(currentSlot);
            totalUseDuration = stack.getUseDuration() > 0 ? stack.getUseDuration() : 32;
            warrior.setItemSlot(EquipmentSlot.MAINHAND, stack);
            warrior.startUsingItem(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public void tick() {
        if (warrior.getTarget() != null) {
            warrior.setTarget(null);
        }
        if (warrior.getLastHurtByMob() != null) {
            warrior.setLastHurtByMob(null);
        }

        // 1. Navegação de fuga / reposicionamento
        if (recalPathTicks <= 0) {
            recalPathTicks = 12;
            LivingEntity threat = null;
            var nearbyEnemies = warrior.level().getEntitiesOfClass(
                    Monster.class,
                    warrior.getBoundingBox().inflate(8.0D),
                    net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR
            );
            if (!nearbyEnemies.isEmpty()) {
                threat = nearbyEnemies.get(0);
            }

            Vec3 targetPos;
            Player owner = warrior.getOwner();
            if (owner != null && owner.isAlive() && warrior.distanceToSqr(owner) > 9.0D && warrior.distanceToSqr(owner) <= 324.0D) {
                // Posiciona-se 2.5 blocos atrás da linha de visão do dono, usando o comandante como cobertura!
                Vec3 ownerLook = owner.getLookAngle().multiply(1.0, 0.0, 1.0);
                if (ownerLook.lengthSqr() > 1.0E-4D) {
                    targetPos = owner.position().subtract(ownerLook.normalize().scale(2.5D));
                } else {
                    targetPos = owner.position();
                }
            } else if (threat != null) {
                Vec3 awayDir = warrior.position().subtract(threat.position()).multiply(1.0D, 0.0D, 1.0D).normalize().scale(5.0D);
                targetPos = warrior.position().add(awayDir);
            } else {
                targetPos = warrior.position().add(
                        -Math.sin(Math.toRadians(warrior.getYRot())) * 4.0D,
                        0.0D,
                        Math.cos(Math.toRadians(warrior.getYRot())) * 4.0D
                );
            }

            warrior.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.25D);
            warrior.getLookControl().setLookAt(targetPos.x, warrior.getY() + 0.6D, targetPos.z, 50.0F, 50.0F);

            // Se um monstro estiver colado (< 2.5 blocos), dá um pulo tático para criar espaço enquanto consome
            if (threat != null && warrior.distanceToSqr(threat) < 6.25D) {
                Vec3 pushAway = warrior.position().subtract(threat.position()).multiply(1.0, 0.0, 1.0);
                if (pushAway.lengthSqr() > 1.0E-4D) {
                    warrior.performTacticalStep(pushAway.normalize(), 0.35D);
                }
            }
        } else {
            recalPathTicks--;
        }

        // 2. Lógica de consumo
        if (currentSlot == -1 || WarriorHealingHelper.getConsumableHealingScore(warrior, warrior.getWarriorInventory().getItem(currentSlot), true) < 0) {
            currentSlot = findBestConsumableSlot();
        }

        if (currentSlot == -1) {
            stop();
            return;
        }

        ItemStack consumableStack = warrior.getWarriorInventory().getItem(currentSlot);
        if (consumableStack.isEmpty()) {
            stop();
            return;
        }

        if (!warrior.isUsingItem()) {
            warrior.setItemSlot(EquipmentSlot.MAINHAND, consumableStack);
            warrior.startUsingItem(InteractionHand.MAIN_HAND);
        }

        eatingTicks++;

        if (eatingTicks % 4 == 0) {
            boolean isDrink = consumableStack.getUseAnimation() == UseAnim.DRINK || consumableStack.getItem() instanceof PotionItem;
            warrior.level().playSound(null, warrior.getX(), warrior.getY(), warrior.getZ(),
                    isDrink ? SoundEvents.GENERIC_DRINK : SoundEvents.GENERIC_EAT,
                    SoundSource.NEUTRAL, 0.6F, 0.9F + (warrior.getRandom().nextFloat() * 0.2F));

            if (warrior.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, consumableStack),
                        warrior.getX(), warrior.getY() + 1.35D, warrior.getZ(),
                        6, 0.2D, 0.2D, 0.2D, 0.05D);
            }
        }

        totalUseDuration = consumableStack.getUseDuration() > 0 ? consumableStack.getUseDuration() : 32;
        if (eatingTicks >= totalUseDuration) {
            eatingTicks = 0;
            int slotToConsume = currentSlot;
            currentSlot = -1;
            WarriorHealingHelper.consumeHealingItem(warrior, consumableStack, slotToConsume);
        }
    }

    @Override
    public void stop() {
        eatingTicks = 0;
        currentSlot = -1;
        warrior.stopUsingItem();
        warrior.syncEquipmentWithInventory();
        warrior.setEmergencyRetreatCooldown(0);
    }
}
