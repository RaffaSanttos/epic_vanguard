package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Heroic Intervene Goal:
 * Emergency life-saving reaction when the player drops to < 3 degrees of life (HP<=6.0):
 * 1. Alerts with a martial horn sound.
 * 2. Rushes at maximum sprint (1.4x) to interpose between player and attacker.
 * 3. Taunts, body-blocks, and forcibly draws enemy aggro onto itself.
 * 4. Executes class-specific emergency defense.
 */
public class HeroicInterveneGoal extends Goal {

    private final WarriorCompanionEntity warrior;
    private LivingEntity currentAttacker = null;
    private int shoutCooldown = 0;
    private int attackCooldown = 0;

    public HeroicInterveneGoal(WarriorCompanionEntity warrior) {
        this.warrior = warrior;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!warrior.isRecruited() || warrior.getCombatMode() == 2) return false;
        if (warrior.isTalking() || warrior.isInventoryOpen() || warrior.isDuelMode() || warrior.isPrisoner()) return false;

        if (!warrior.isOwnerInCriticalDanger()) return false;

        LivingEntity attacker = warrior.getOwnerAttacker();
        if (attacker == null || !attacker.isAlive() || attacker == warrior) return false;

        this.currentAttacker = attacker;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!warrior.isRecruited() || warrior.getCombatMode() == 2) return false;
        if (!warrior.isAlive() || warrior.isEmergencyRetreating()) return false;

        Player owner = warrior.getOwner();
        if (owner == null || !owner.isAlive() || owner.getHealth() > 8.0F) return false;

        return currentAttacker != null && currentAttacker.isAlive() && warrior.distanceToSqr(owner) <= 324.0D;
    }

    @Override
    public void start() {
        Player owner = warrior.getOwner();
        if (owner == null) return;

        warrior.level().playSound(null, warrior.blockPosition(),
                SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).get(), SoundSource.PLAYERS, 1.2F, 1.1F);

        if (warrior.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, warrior.getX(), warrior.getY() + 1.2D, warrior.getZ(),
                    15, 0.4F, 0.5F, 0.4F, 0.1F);
        }

        owner.displayClientMessage(
                Component.literal("\u00a6c\u00a7l\udbc0\udc19 [Vanguarda] \u00a7e" + warrior.getWarriorName() + " \u00a6cinterv\u00e9m para proteger sua vida!"),
                true
        );

        this.shoutCooldown = 60;
        this.attackCooldown = 0;
    }

    @Override
    public void stop() {
        this.currentAttacker = null;
        this.warrior.lowerShield();
    }

    @Override
    public void tick() {
        Player owner = warrior.getOwner();
        if (owner == null) return;

        if (currentAttacker == null || !currentAttacker.isAlive()) {
            currentAttacker = warrior.getOwnerAttacker();
            if (currentAttacker == null) return;
        }

        if (attackCooldown > 0) attackCooldown--;
        if (shoutCooldown > 0) shoutCooldown--;

        warrior.getLookControl().setLookAt(currentAttacker, 40.0F, 40.0F);

        Vec3 ownerPos = owner.position();
        Vec3 threatPos = currentAttacker.position();
        Vec3 dir = threatPos.subtract(ownerPos).multiply(1.0, 0.0, 1.0);
        if (dir.lengthSqr() < 1.0E-4D) {
            dir = new Vec3(0, 0, 1);
        } else {
            dir = dir.normalize();
        }

        Vec3 interceptPos = ownerPos.add(dir.scale(1.6D));
        warrior.getNavigation().moveTo(interceptPos.x, interceptPos.y, interceptPos.z, 1.40D);

        if (currentAttacker instanceof Mob mob && mob.getTarget() != warrior) {
            mob.setTarget(warrior);
            mob.setLastHurtByMob(warrior);
        }
        warrior.setTarget(currentAttacker);

        int spec = warrior.getSpecialization();
        double distToAttackerSq = warrior.distanceToSqr(currentAttacker);
        double reachSqr = (warrior.getBbWidth() * 2.0F * warrior.getBbWidth() * 2.0F + currentAttacker.getBbWidth());

        if (spec == WarriorCompanionEntity.SPEC_GUARDIAN) {
            if (warrior.hasShield()) {
                warrior.raiseShield();
            }
            if (distToAttackerSq <= reachSqr && attackCooldown <= 0) {
                warrior.swing(InteractionHand.MAIN_HAND);
                warrior.doHurtTarget(currentAttacker);
                attackCooldown = 18;
            }
        } else if (spec == WarriorCompanionEntity.SPEC_BERSERKER) {
            if (distToAttackerSq <= reachSqr && attackCooldown <= 0) {
                warrior.swing(InteractionHand.MAIN_HAND);
                warrior.doHurtTarget(currentAttacker);

                Vec3 knockbackDir = currentAttacker.position().subtract(ownerPos).multiply(1.0, 0.0, 1.0).normalize().scale(0.8D);
                currentAttacker.setDeltaMovement(knockbackDir.x, 0.25D, knockbackDir.z);

                attackCooldown = 12;
            }
        } else {
            if (distToAttackerSq <= reachSqr && attackCooldown <= 0) {
                warrior.swing(InteractionHand.MAIN_HAND);
                warrior.doHurtTarget(currentAttacker);
                attackCooldown = 10;
            }
        }
    }
}
