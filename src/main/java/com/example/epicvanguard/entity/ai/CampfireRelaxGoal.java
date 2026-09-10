package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.dialogue.WarriorSpeechSystem;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class CampfireRelaxGoal extends Goal {

    private final WarriorCompanionEntity warrior;
    private BlockPos campfirePos = null;
    private BlockPos sitSpotPos = null;
    private int checkCooldown = 0;
    private int ambientTimer = 0;
    private int warmHandsTimer = 0;

    public CampfireRelaxGoal(WarriorCompanionEntity warrior) {
        this.warrior = warrior;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (warrior.isTalking() || warrior.isDuelMode() || warrior.isEmergencyRetreating() || warrior.isPrisoner()) {
            return false;
        }

        if (warrior.getTarget() != null || warrior.getLastHurtByMob() != null) {
            return false;
        }

        if (checkCooldown > 0) {
            checkCooldown--;
            return false;
        }
        checkCooldown = 40; // Verifica a cada 2 segundos

        if (warrior.isRecruited()) {
            int mode = warrior.getCombatMode();
            if (mode == 0) {
                // Modo 0 (Seguir): só descansa se o dono estiver por perto e parado/agachado
                Player owner = warrior.getOwner();
                if (owner == null || warrior.distanceToSqr(owner) > 100.0D) return false;
                if (owner.getDeltaMovement().horizontalDistanceSqr() > 0.01D && !owner.isCrouching()) {
                    return false;
                }
            }
            // Modos 1 (Guarda) e 2 (Parado) podem usar livremente se houver fogueira próxima
        }

        return findNearbyLitCampfire();
    }

    private boolean findNearbyLitCampfire() {
        BlockPos origin = warrior.blockPosition();
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    mut.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = warrior.level().getBlockState(mut);
                    if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
                        BlockPos safeSpot = findSafeSitSpotAround(mut);
                        if (safeSpot != null) {
                            this.campfirePos = mut.immutable();
                            this.sitSpotPos = safeSpot;
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private BlockPos findSafeSitSpotAround(BlockPos firePos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = firePos.relative(dir, 2);
            BlockPos below = candidate.below();
            BlockState stateBelow = warrior.level().getBlockState(below);
            BlockState stateAt = warrior.level().getBlockState(candidate);
            BlockState stateAbove = warrior.level().getBlockState(candidate.above());

            if (stateBelow.isSolidRender(warrior.level(), below) && !stateAt.blocksMotion() && !stateAbove.blocksMotion()) {
                if (!warrior.level().getBlockState(candidate).is(net.minecraft.world.level.block.Blocks.FIRE)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    @Override
    public boolean canContinueToUse() {
        if (warrior.isTalking() || warrior.isDuelMode() || warrior.isEmergencyRetreating() || warrior.isPrisoner()) {
            return false;
        }
        if (warrior.getTarget() != null || warrior.getLastHurtByMob() != null) {
            return false;
        }
        if (campfirePos == null || sitSpotPos == null) {
            return false;
        }

        BlockState fireState = warrior.level().getBlockState(campfirePos);
        if (!(fireState.getBlock() instanceof CampfireBlock) || !fireState.getValue(CampfireBlock.LIT)) {
            return false;
        }

        if (warrior.isRecruited() && warrior.getCombatMode() == 0) {
            Player owner = warrior.getOwner();
            if (owner == null || warrior.distanceToSqr(owner) > 144.0D) { // > 12 blocos de distância do dono
                return false;
            }
        }

        return true;
    }

    @Override
    public void start() {
        ambientTimer = 0;
        warmHandsTimer = 0;
    }

    @Override
    public void tick() {
        if (sitSpotPos == null || campfirePos == null) return;

        double distToSpotSq = warrior.distanceToSqr(sitSpotPos.getX() + 0.5D, sitSpotPos.getY(), sitSpotPos.getZ() + 0.5D);

        // Se ainda não chegou ao ponto de sentar
        if (distToSpotSq > 1.8D) {
            if (warrior.getPose() == Pose.SITTING) {
                warrior.setPose(Pose.STANDING);
            }
            warrior.getNavigation().moveTo(sitSpotPos.getX() + 0.5D, sitSpotPos.getY(), sitSpotPos.getZ() + 0.5D, 0.75D);
            return;
        }

        // Chegou ao ponto seguro perto da fogueira
        warrior.getNavigation().stop();
        warrior.getLookControl().setLookAt(campfirePos.getX() + 0.5D, campfirePos.getY() + 0.3D, campfirePos.getZ() + 0.5D, 30.0F, 30.0F);

        if (warrior.getPose() != Pose.SITTING) {
            warrior.setPose(Pose.SITTING);
            WarriorSpeechSystem.onCampfireRest(warrior);
        }

        // Efeitos ambientes periódicos
        ambientTimer++;
        if (ambientTimer >= 140) {
            ambientTimer = 0;
            if (warrior.level() instanceof ServerLevel serverLevel) {
                // Partícula sutil de conforto
                if (warrior.getRandom().nextFloat() < 0.4F) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE, campfirePos.getX() + 0.5D, campfirePos.getY() + 0.6D, campfirePos.getZ() + 0.5D,
                            3, 0.15D, 0.2D, 0.15D, 0.01D);
                }
                if (warrior.getRandom().nextFloat() < 0.3F) {
                    serverLevel.playSound(null, warrior.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 0.6F, 1.0F);
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, warrior.getX(), warrior.getY() + 0.8D, warrior.getZ(),
                            2, 0.2D, 0.2D, 0.2D, 0.02D);
                }
            }
        }
    }

    @Override
    public void stop() {
        if (warrior.getPose() == Pose.SITTING) {
            warrior.setPose(Pose.STANDING);
        }
        this.campfirePos = null;
        this.sitSpotPos = null;
    }
}
