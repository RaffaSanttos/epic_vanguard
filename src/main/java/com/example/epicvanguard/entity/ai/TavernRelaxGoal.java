package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModBlocks;
import com.example.epicvanguard.init.ModPoiTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumSet;

public class TavernRelaxGoal extends Goal {
    private final WarriorCompanionEntity warrior;
    private final double speed;
    private BlockPos tavernPointPos = null;
    private int wanderCooldown = 0;
    private int consumeTimer = 0;
    private int sitTimer = 0;

    public TavernRelaxGoal(WarriorCompanionEntity warrior, double speed) {
        this.warrior = warrior;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (warrior.isRecruited() || warrior.isPrisoner() || warrior.isDuelMode() || warrior.isTalking() || warrior.getTarget() != null) {
            return false;
        }
        if (tavernPointPos == null || !warrior.level().getBlockState(tavernPointPos).is(ModBlocks.VANGUARD_POINT.get())) {
            findTavernPoint();
        }
        return tavernPointPos != null;
    }

    private void findTavernPoint() {
        if (warrior.level() instanceof ServerLevel serverLevel) {
            var poiManager = serverLevel.getPoiManager();
            var opt = poiManager.getInRange(
                    holder -> holder.is(ModPoiTypes.VANGUARD_POI.getKey()),
                    warrior.blockPosition(),
                    48,
                    PoiManager.Occupancy.ANY
            ).map(PoiRecord::getPos).findFirst();

            if (opt.isPresent()) {
                tavernPointPos = opt.get();
                return;
            }
        }

        BlockPos origin = warrior.blockPosition();
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int dx = -32; dx <= 32; dx += 2) {
            for (int dy = -10; dy <= 10; dy++) {
                for (int dz = -32; dz <= 32; dz += 2) {
                    mut.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (warrior.level().getBlockState(mut).is(ModBlocks.VANGUARD_POINT.get())) {
                        tavernPointPos = mut.immutable();
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void tick() {
        if (tavernPointPos == null) return;

        double distSq = warrior.distanceToSqr(tavernPointPos.getX() + 0.5D, tavernPointPos.getY(), tavernPointPos.getZ() + 0.5D);

        // 1. Se estiver muito longe do Vanguard Point (> 8 blocos), caminha de volta para a área
        if (distSq > 64.0D) {
            if (warrior.getPose() == Pose.SITTING) {
                warrior.setPose(Pose.STANDING);
            }
            if (warrior.getNavigation().isDone() || warrior.tickCount % 40 == 0) {
                warrior.getNavigation().moveTo(tavernPointPos.getX() + 0.5D, tavernPointPos.getY(), tavernPointPos.getZ() + 0.5D, this.speed);
            }
            return;
        }

        // 2. Dentro do raio do mural (<= 8 blocos): anda naturalmente pelas redondezas
        if (wanderCooldown > 0) {
            wanderCooldown--;
        }

        if (warrior.getNavigation().isDone() && wanderCooldown <= 0) {
            wanderCooldown = 60 + warrior.getRandom().nextInt(100);

            if (warrior.getRandom().nextFloat() < 0.45F) {
                if (warrior.getPose() == Pose.SITTING) {
                    warrior.setPose(Pose.STANDING);
                }
                int rx = tavernPointPos.getX() + warrior.getRandom().nextInt(11) - 5;
                int rz = tavernPointPos.getZ() + warrior.getRandom().nextInt(11) - 5;
                BlockPos target = warrior.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(rx, 0, rz));
                if (warrior.level().getBlockState(target.below()).isSolidRender(warrior.level(), target.below())) {
                    warrior.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, this.speed * 0.75D);
                }
            } else if (warrior.getRandom().nextFloat() < 0.25F) {
                sitTimer = 80 + warrior.getRandom().nextInt(80);
                warrior.setPose(Pose.SITTING);
            }
        }

        if (sitTimer > 0) {
            sitTimer--;
            if (sitTimer == 0) {
                warrior.setPose(Pose.STANDING);
            }
        }

        consumeTimer++;
        if (consumeTimer >= 220) {
            consumeTimer = 0;
            if (warrior.level() instanceof ServerLevel serverLevel && warrior.getRandom().nextFloat() < 0.35F) {
                serverLevel.playSound(null, warrior.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 0.8F, 1.0F);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, warrior.getX(), warrior.getY() + 1.2D, warrior.getZ(), 4, 0.2D, 0.2D, 0.2D, 0.02D);
            }
        }
    }

    @Override
    public void stop() {
        warrior.setPose(Pose.STANDING);
        super.stop();
    }
}
