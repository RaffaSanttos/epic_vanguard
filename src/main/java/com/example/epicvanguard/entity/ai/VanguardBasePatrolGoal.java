package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.dialogue.WarriorSpeechSystem;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModBlocks;
import com.example.epicvanguard.init.ModPoiTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumSet;

public class VanguardBasePatrolGoal extends Goal {

    private final WarriorCompanionEntity warrior;
    private BlockPos vanguardPointPos = null;
    private BlockPos currentWaypoint = null;
    private int patrolCooldown = 0;
    private int guardWatchTicks = 0;
    private int findPoiCooldown = 0;
    private boolean ownerWasFar = true;

    public VanguardBasePatrolGoal(WarriorCompanionEntity warrior) {
        this.warrior = warrior;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!warrior.isRecruited() || warrior.isTalking() || warrior.isDuelMode() || warrior.isEmergencyRetreating()) {
            return false;
        }

        int mode = warrior.getCombatMode();
        if (mode != 1 && mode != 2) { // 1 = Guarda, 2 = Parado
            return false;
        }

        if (warrior.getTarget() != null || warrior.getLastHurtByMob() != null) {
            return false;
        }

        if (vanguardPointPos == null || !warrior.level().getBlockState(vanguardPointPos).is(ModBlocks.VANGUARD_POINT.get())) {
            if (findPoiCooldown > 0) {
                findPoiCooldown--;
                return false;
            }
            findPoiCooldown = 60; // 3 segundos entre buscas
            findVanguardPoint();
        }

        return vanguardPointPos != null;
    }

    private void findVanguardPoint() {
        if (warrior.level() instanceof ServerLevel serverLevel) {
            var opt = serverLevel.getPoiManager().getInRange(
                    holder -> holder.is(ModPoiTypes.VANGUARD_POI.getKey()),
                    warrior.blockPosition(),
                    32,
                    PoiManager.Occupancy.ANY
            ).map(PoiRecord::getPos).findFirst();

            if (opt.isPresent()) {
                vanguardPointPos = opt.get();
                return;
            }
        }

        BlockPos origin = warrior.blockPosition();
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int dx = -20; dx <= 20; dx += 2) {
            for (int dy = -6; dy <= 6; dy++) {
                for (int dz = -20; dz <= 20; dz += 2) {
                    mut.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (warrior.level().getBlockState(mut).is(ModBlocks.VANGUARD_POINT.get())) {
                        vanguardPointPos = mut.immutable();
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (!warrior.isRecruited() || warrior.isTalking() || warrior.isDuelMode() || warrior.isEmergencyRetreating()) {
            return false;
        }
        int mode = warrior.getCombatMode();
        if (mode != 1 && mode != 2) return false;
        if (warrior.getTarget() != null || warrior.getLastHurtByMob() != null) return false;

        return vanguardPointPos != null && warrior.level().getBlockState(vanguardPointPos).is(ModBlocks.VANGUARD_POINT.get());
    }

    @Override
    public void start() {
        guardWatchTicks = 0;
        patrolCooldown = 20;
    }

    @Override
    public void tick() {
        if (vanguardPointPos == null) return;

        // 1. Verificação de retorno do Dono à base
        Player owner = warrior.getOwner();
        if (owner != null && owner.isAlive()) {
            double distToOwnerSq = warrior.distanceToSqr(owner);
            if (distToOwnerSq > 900.0D) { // > 30 blocos
                ownerWasFar = true;
            } else if (distToOwnerSq <= 100.0D && ownerWasFar) { // <= 10 blocos e estava longe
                ownerWasFar = false;
                warrior.getLookControl().setLookAt(owner, 30.0F, 30.0F);
                WarriorSpeechSystem.onBaseWelcome(warrior);
            }
        }

        // 2. Se estiver em posto de vigia (parado vigiando)
        if (guardWatchTicks > 0) {
            guardWatchTicks--;
            // Olha em direção para fora da base (horizonte)
            if (currentWaypoint != null) {
                double lookX = currentWaypoint.getX() + (currentWaypoint.getX() - vanguardPointPos.getX()) * 2.0D;
                double lookZ = currentWaypoint.getZ() + (currentWaypoint.getZ() - vanguardPointPos.getZ()) * 2.0D;
                warrior.getLookControl().setLookAt(lookX, warrior.getY() + 1.2D, lookZ, 10.0F, 10.0F);
            }
            return;
        }

        // 3. Se estiver em cooldown antes de escolher o próximo waypoint
        if (patrolCooldown > 0) {
            patrolCooldown--;
            return;
        }

        // 4. Seleção e navegação de waypoint
        if (currentWaypoint == null || warrior.distanceToSqr(currentWaypoint.getX() + 0.5D, currentWaypoint.getY(), currentWaypoint.getZ() + 0.5D) < 4.0D) {
            // Chegou ao waypoint: entra em vigia por 10 a 20 segundos (200-400 ticks)
            if (currentWaypoint != null) {
                guardWatchTicks = 200 + warrior.getRandom().nextInt(200);
                patrolCooldown = 40;
                currentWaypoint = null;
                return;
            }

            // Sorteia novo waypoint em um raio de 4 a 8 blocos em torno do Ponto de Vanguarda
            int rx = vanguardPointPos.getX() + warrior.getRandom().nextInt(15) - 7;
            int rz = vanguardPointPos.getZ() + warrior.getRandom().nextInt(15) - 7;
            BlockPos groundPos = warrior.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(rx, 0, rz));

            BlockState stateBelow = warrior.level().getBlockState(groundPos.below());
            if (stateBelow.isSolidRender(warrior.level(), groundPos.below())) {
                currentWaypoint = groundPos;
                warrior.getNavigation().moveTo(currentWaypoint.getX() + 0.5D, currentWaypoint.getY(), currentWaypoint.getZ() + 0.5D, 0.70D);
            } else {
                patrolCooldown = 20;
            }
        }
    }

    @Override
    public void stop() {
        if (warrior.getPose() == Pose.SITTING) {
            warrior.setPose(Pose.STANDING);
        }
        currentWaypoint = null;
        guardWatchTicks = 0;
    }
}
