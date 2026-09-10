package com.example.epicvanguard.entity.util;

import com.example.epicvanguard.dialogue.WarriorSpeechSystem;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.inventory.WarriorInventory;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;

import java.util.List;

public class WarriorHealingHelper {

    public static boolean isHealingPotion(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PotionItem)) return false;
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
        for (MobEffectInstance effect : effects) {
            if (effect.getEffect() == MobEffects.HEAL || effect.getEffect() == MobEffects.REGENERATION) {
                return true;
            }
        }
        return false;
    }

    public static int findBestConsumableSlot(WarriorCompanionEntity warrior, boolean inEmergency) {
        WarriorInventory inv = warrior.getWarriorInventory();
        int bestSlot = -1;
        int highestScore = -1;

        for (int i = WarriorInventory.SLOT_BACKPACK_START; i <= WarriorInventory.SLOT_BACKPACK_END; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                int score = getConsumableHealingScore(warrior, stack, inEmergency);
                if (score > highestScore) {
                    highestScore = score;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    public static int getConsumableHealingScore(WarriorCompanionEntity warrior, ItemStack stack, boolean inEmergency) {
        if (stack.isEmpty()) return -1;

        // 1. Poção de Cura / Regeneração (requer Nível >= 3 do guerreiro)
        if (stack.getItem() instanceof PotionItem) {
            if (warrior.getWarriorLevel() < 3) return -1;
            List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
            for (MobEffectInstance effect : effects) {
                if (effect.getEffect() == MobEffects.HEAL) {
                    return inEmergency ? 1000 + (effect.getAmplifier() * 100) : 100;
                }
                if (effect.getEffect() == MobEffects.REGENERATION) {
                    boolean hasRegen = warrior.hasEffect(MobEffects.REGENERATION);
                    if (hasRegen) {
                        return inEmergency ? 350 : 50;
                    }
                    return inEmergency ? 800 + (effect.getAmplifier() * 50) : 150;
                }
            }
            return -1;
        }

        // 2. Alimentos especiais lendários (Maçã Dourada / Encantada)
        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return inEmergency ? 950 : 200;
        }
        if (stack.is(Items.GOLDEN_APPLE)) {
            return inEmergency ? 900 : 180;
        }

        // 3. Alimentos comuns
        if (stack.isEdible()) {
            var foodProps = stack.getItem().getFoodProperties(stack, warrior);
            if (foodProps != null) {
                for (var effectPair : foodProps.getEffects()) {
                    var effect = effectPair.getFirst().getEffect();
                    if (effect == MobEffects.POISON || effect == MobEffects.WITHER || effect == MobEffects.HARM) {
                        return -1;
                    }
                }
                int nutrition = foodProps.getNutrition();
                float saturation = foodProps.getSaturationModifier();
                int baseScore = inEmergency ? 400 : 500;
                return baseScore + (nutrition * 10) + (int) (saturation * 10);
            }
        }

        return -1;
    }

    public static void consumeHealingItem(WarriorCompanionEntity warrior, ItemStack stack, int slotIndex) {
        if (stack.isEmpty()) return;

        boolean isDrink = stack.getUseAnimation() == UseAnim.DRINK || stack.getItem() instanceof PotionItem;

        if (stack.getItem() instanceof PotionItem) {
            List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
            for (MobEffectInstance effect : effects) {
                if (effect.getEffect() == MobEffects.HEAL) {
                    warrior.heal(4.0F * (effect.getAmplifier() + 1));
                } else if (effect.getEffect().isInstantenous()) {
                    effect.getEffect().applyInstantenousEffect(warrior, warrior, warrior, effect.getAmplifier(), 1.0D);
                } else {
                    warrior.addEffect(new MobEffectInstance(effect));
                }
            }

            warrior.level().playSound(null, warrior.getX(), warrior.getY(), warrior.getZ(),
                    SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 0.8F, 0.9F + (warrior.getRandom().nextFloat() * 0.1F));

            if (warrior.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART, warrior.getX(), warrior.getY() + 1.2D, warrior.getZ(),
                        8, 0.35D, 0.35D, 0.35D, 0.05D);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, warrior.getX(), warrior.getY() + 1.0D, warrior.getZ(),
                        12, 0.4D, 0.5D, 0.4D, 0.05D);
                serverLevel.sendParticles(ParticleTypes.ENTITY_EFFECT, warrior.getX(), warrior.getY() + 1.0D, warrior.getZ(),
                        10, 0.3D, 0.5D, 0.3D, 1.0D);
            }

            stack.shrink(1);
            if (stack.isEmpty()) {
                warrior.getWarriorInventory().setItem(slotIndex, new ItemStack(Items.GLASS_BOTTLE));
            } else {
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                ItemStack remaining = warrior.getWarriorInventory().addItemToBackpack(bottle);
                if (!remaining.isEmpty()) {
                    warrior.spawnAtLocation(remaining);
                }
            }
        } else if (stack.isEdible()) {
            var foodProps = stack.getItem().getFoodProperties(stack, warrior);
            int nutrition = foodProps != null ? foodProps.getNutrition() : 4;
            float saturation = foodProps != null ? foodProps.getSaturationModifier() : 0.6F;
            float healAmount = Math.max(4.0F, nutrition + (saturation * 2.0F));

            warrior.heal(healAmount);

            if (foodProps != null) {
                for (var effectPair : foodProps.getEffects()) {
                    if (effectPair.getFirst() != null && warrior.getRandom().nextFloat() < effectPair.getSecond()) {
                        warrior.addEffect(new MobEffectInstance(effectPair.getFirst()));
                    }
                }
            }

            warrior.level().playSound(null, warrior.getX(), warrior.getY(), warrior.getZ(),
                    isDrink ? SoundEvents.GENERIC_DRINK : SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 0.7F, 1.0F);

            if (warrior.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART, warrior.getX(), warrior.getY() + 1.2D, warrior.getZ(),
                        6, 0.3D, 0.3D, 0.3D, 0.05D);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, warrior.getX(), warrior.getY() + 1.0D, warrior.getZ(),
                        8, 0.3D, 0.4D, 0.3D, 0.05D);
            }

            ItemStack containerItem = ItemStack.EMPTY;
            if (stack.is(Items.HONEY_BOTTLE)) {
                containerItem = new ItemStack(Items.GLASS_BOTTLE);
            } else if (stack.is(Items.MUSHROOM_STEW) || stack.is(Items.RABBIT_STEW) || stack.is(Items.BEETROOT_SOUP) || stack.is(Items.SUSPICIOUS_STEW)) {
                containerItem = new ItemStack(Items.BOWL);
            } else if (stack.hasCraftingRemainingItem()) {
                containerItem = stack.getCraftingRemainingItem();
            }

            stack.shrink(1);
            if (stack.isEmpty()) {
                warrior.getWarriorInventory().setItem(slotIndex, containerItem.isEmpty() ? ItemStack.EMPTY : containerItem);
            } else if (!containerItem.isEmpty()) {
                ItemStack remaining = warrior.getWarriorInventory().addItemToBackpack(containerItem);
                if (!remaining.isEmpty()) {
                    warrior.spawnAtLocation(remaining);
                }
            }
        }

        warrior.stopUsingItem();
        warrior.syncEquipmentWithInventory();
        WarriorSpeechSystem.onHealed(warrior);
    }
}
