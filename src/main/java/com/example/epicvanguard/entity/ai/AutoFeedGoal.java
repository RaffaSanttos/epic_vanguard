package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.entity.util.WarriorHealingHelper;
import com.example.epicvanguard.inventory.WarriorInventory;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.UseAnim;

import java.util.EnumSet;

public class AutoFeedGoal extends Goal {
    private final WarriorCompanionEntity warrior;
    private int currentSlot = -1;
    private int eatingTicks = 0;
    private int totalUseDuration = 32;
    private int postEatCooldown = 0;

    public AutoFeedGoal(WarriorCompanionEntity warrior) {
        this.warrior = warrior;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    private int findConsumableSlot() {
        return WarriorHealingHelper.findBestConsumableSlot(warrior, false);
    }

    @Override
    public boolean canUse() {
        if (postEatCooldown > 0) {
            postEatCooldown--;
            return false;
        }
        if (warrior.getHealth() >= warrior.getMaxHealth()) return false;
        if (warrior.getTarget() != null && warrior.getTarget().isAlive()) return false;
        return findConsumableSlot() != -1;
    }

    @Override
    public boolean canContinueToUse() {
        if (warrior.getHealth() >= warrior.getMaxHealth()) return false;
        if (warrior.getTarget() != null && warrior.getTarget().isAlive()) return false;
        if (currentSlot == -1) return false;
        ItemStack stack = warrior.getWarriorInventory().getItem(currentSlot);
        return !stack.isEmpty() && WarriorHealingHelper.getConsumableHealingScore(warrior, stack, false) >= 0 && eatingTicks < totalUseDuration;
    }

    @Override
    public void start() {
        currentSlot = findConsumableSlot();
        if (currentSlot != -1) {
            ItemStack stack = warrior.getWarriorInventory().getItem(currentSlot);
            totalUseDuration = stack.getUseDuration() > 0 ? stack.getUseDuration() : 32;
            eatingTicks = 0;
            warrior.setItemSlot(EquipmentSlot.MAINHAND, stack);
            warrior.startUsingItem(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public void stop() {
        currentSlot = -1;
        eatingTicks = 0;
        warrior.stopUsingItem();
        warrior.syncEquipmentWithInventory();
    }

    @Override
    public void tick() {
        if (currentSlot == -1) return;

        WarriorInventory inv = warrior.getWarriorInventory();
        ItemStack stack = inv.getItem(currentSlot);
        if (stack.isEmpty() || WarriorHealingHelper.getConsumableHealingScore(warrior, stack, false) < 0) {
            stop();
            return;
        }

        if (!warrior.isUsingItem()) {
            warrior.setItemSlot(EquipmentSlot.MAINHAND, stack);
            warrior.startUsingItem(InteractionHand.MAIN_HAND);
        }

        eatingTicks++;

        // Som de mastigação/bebida e partículas a cada 4 ticks
        if (eatingTicks % 4 == 0) {
            boolean isDrink = stack.getUseAnimation() == UseAnim.DRINK || stack.getItem() instanceof PotionItem;
            warrior.level().playSound(null, warrior.getX(), warrior.getY(), warrior.getZ(),
                    isDrink ? SoundEvents.GENERIC_DRINK : SoundEvents.GENERIC_EAT,
                    SoundSource.NEUTRAL, 0.6F, 0.9F + (warrior.getRandom().nextFloat() * 0.2F));

            if (warrior.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, stack),
                        warrior.getX(), warrior.getY() + 1.35D, warrior.getZ(),
                        5, 0.18D, 0.18D, 0.18D, 0.05D);
            }
        }

        // Quando conclui os ticks de mastigação/bebida -> consome o item e cura
        if (eatingTicks >= totalUseDuration) {
            int slotToConsume = currentSlot;
            currentSlot = -1;
            eatingTicks = 0;
            WarriorHealingHelper.consumeHealingItem(warrior, stack, slotToConsume);
            postEatCooldown = 15;
            stop();
        }
    }
}
