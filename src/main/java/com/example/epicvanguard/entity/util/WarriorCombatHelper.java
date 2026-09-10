package com.example.epicvanguard.entity.util;

import com.example.epicvanguard.compat.epicfight.EpicFightCompat;
import com.example.epicvanguard.entity.CompanionSavedData;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public class WarriorCombatHelper {

    public static boolean checkFriendlyFire(WarriorCompanionEntity warrior, DamageSource source) {
        if (!warrior.isRecruited() || warrior.isDuelMode() || warrior.getOwnerUUID().isEmpty()) {
            return true;
        }

        UUID ownerId = warrior.getOwnerUUID().get();
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();

        // 1. Companheiros do mesmo dono nunca se acertam entre si
        if (attacker instanceof WarriorCompanionEntity otherWarrior && otherWarrior.isRecruited() && otherWarrior.getOwnerUUID().isPresent()) {
            if (otherWarrior.getOwnerUUID().get().equals(ownerId)) {
                return false;
            }
        }
        if (direct instanceof WarriorCompanionEntity otherWarriorDirect && otherWarriorDirect.isRecruited() && otherWarriorDirect.getOwnerUUID().isPresent()) {
            if (otherWarriorDirect.getOwnerUUID().get().equals(ownerId)) {
                return false;
            }
        }

        // 2. Ataques originados do Dono
        boolean isFromOwner = (attacker != null && attacker.getUUID().equals(ownerId)) ||
                              (direct != null && direct.getUUID().equals(ownerId));

        if (isFromOwner && warrior.getServer() != null) {
            CompanionSavedData.FriendlyFireMode ffMode = CompanionSavedData.get(warrior.getServer()).getGlobalFriendlyFireMode();
            boolean isMagic = WarriorCompanionEntity.isSpellOrMagicDamage(source);

            switch (ffMode) {
                case DISABLED:
                    return false;
                case SPELLS_ONLY:
                    if (!isMagic) {
                        return false;
                    }
                    break;
                case MELEE_ONLY:
                    if (isMagic) {
                        return false;
                    }
                    break;
                case ALL:
                    break;
            }
        }

        return true;
    }

    public static boolean checkDuelistDodge(WarriorCompanionEntity warrior, DamageSource source) {
        if (warrior.getSpecialization() == WarriorCompanionEntity.SPEC_DUELIST &&
                !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (warrior.getRandom().nextFloat() < 0.18F) {
                warrior.level().playSound(null, warrior.getX(), warrior.getY(), warrior.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 1.4F);
                if (warrior.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.POOF, warrior.getX(), warrior.getY() + 1.0D, warrior.getZ(),
                            10, 0.4D, 0.5D, 0.4D, 0.05D);
                }
                return true; // Esquivou 100%
            }
        }
        return false;
    }

    public static float applyShieldBlock(WarriorCompanionEntity warrior, DamageSource source, float amount) {
        ItemStack offhand = warrior.getItemBySlot(EquipmentSlot.OFFHAND);
        ItemStack mainhand = warrior.getItemBySlot(EquipmentSlot.MAINHAND);
        if (offhand.getItem() instanceof ShieldItem || mainhand.getItem() instanceof ShieldItem) {
            if (warrior.getRandom().nextFloat() < 0.70F) {
                warrior.level().playSound(null, warrior.getX(), warrior.getY(), warrior.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
                if (warrior.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, warrior.getX(), warrior.getY() + 1.0D, warrior.getZ(),
                            8, 0.3D, 0.3D, 0.3D, 0.1D);
                }
                return amount * 0.2F; // 80% de redução
            }
        }
        return amount;
    }

    public static void performEmergencyDodgeRoll(WarriorCompanionEntity warrior, @Nullable Vec3 threatPos) {
        if (warrior.getDodgeCooldown() > 0) return;
        warrior.setDodgeCooldown((warrior.getSpecialization() == WarriorCompanionEntity.SPEC_DUELIST) ? 30 : 60);
        warrior.setEmergencyRetreatCooldown(180);

        Vec3 awayDir;
        if (threatPos != null) {
            awayDir = warrior.position().subtract(threatPos).multiply(1.0D, 0.0D, 1.0D);
            if (awayDir.lengthSqr() < 1.0E-4D) {
                awayDir = new Vec3(-Math.sin(Math.toRadians(warrior.getYRot())), 0.0D, Math.cos(Math.toRadians(warrior.getYRot())));
            } else {
                awayDir = awayDir.normalize();
            }
        } else {
            awayDir = new Vec3(-Math.sin(Math.toRadians(warrior.getYRot())), 0.0D, Math.cos(Math.toRadians(warrior.getYRot())));
        }

        applyRollImpulse(warrior, awayDir);
        com.example.epicvanguard.dialogue.WarriorSpeechSystem.onCriticalRetreat(warrior);
    }

    public static void applyRollImpulse(WarriorCompanionEntity warrior, Vec3 dir) {
        warrior.setDeltaMovement(dir.x * 0.42D, 0.16D, dir.z * 0.42D);
        warrior.hasImpulse = true;

        warrior.level().playSound(null, warrior.getX(), warrior.getY(), warrior.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1.0F, 1.4F);
        warrior.level().playSound(null, warrior.getX(), warrior.getY(), warrior.getZ(),
                SoundEvents.WOOL_FALL, SoundSource.NEUTRAL, 1.0F, 0.9F);

        if (warrior.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, warrior.getX(), warrior.getY() + 0.2D, warrior.getZ(),
                    10, 0.3D, 0.1D, 0.3D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, warrior.getX(), warrior.getY() + 0.4D, warrior.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        EpicFightCompat.playDodgeRollAnimation(warrior);
    }
}
