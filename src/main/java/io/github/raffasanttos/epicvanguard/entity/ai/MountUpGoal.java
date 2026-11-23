package io.github.raffasanttos.epicvanguard.entity.ai;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorMountHelper;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

import java.util.EnumSet;

/** Quando o dono monta, o guerreiro anda até um cavalo domado e selado do dono e monta. */
public class MountUpGoal extends Goal {

    private final WarriorCompanionEntity warrior;
    private AbstractHorse horse;
    private int giveUpTicks;
    private int searchCooldown;

    public MountUpGoal(WarriorCompanionEntity warrior) {
        this.warrior = warrior;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (warrior.isPassenger() || !WarriorMountHelper.shouldRide(warrior)) return false;
        if (searchCooldown > 0) {
            searchCooldown--;
            return false;
        }
        searchCooldown = 20;
        horse = WarriorMountHelper.findMount(warrior);
        return horse != null;
    }

    @Override
    public boolean canContinueToUse() {
        return horse != null && !warrior.isPassenger() && giveUpTicks > 0
                && WarriorMountHelper.shouldRide(warrior) && WarriorMountHelper.isSuitableMount(warrior, horse);
    }

    @Override
    public void start() {
        giveUpTicks = 200;
        warrior.getNavigation().moveTo(horse, 1.3D);
    }

    @Override
    public void stop() {
        horse = null;
        warrior.getNavigation().stop();
    }

    @Override
    public void tick() {
        giveUpTicks--;
        warrior.getLookControl().setLookAt(horse, 30.0F, 30.0F);
        if (warrior.distanceToSqr(horse) < 2.5D * 2.5D) {
            WarriorMountHelper.mount(warrior, horse);
            return;
        }
        if (giveUpTicks % 10 == 0) {
            warrior.getNavigation().moveTo(horse, 1.3D);
        }
    }
}
