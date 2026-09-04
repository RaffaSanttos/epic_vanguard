package com.example.epicvanguard.event;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.command.VanguardCommand;
import com.example.epicvanguard.entity.CompanionSavedData;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModEntityTypes;
import com.example.epicvanguard.networking.Messages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = EpicVanguardMod.MOD_ID)
public class VanguardCombatEvents {

    @Mod.EventBusSubscriber(modid = EpicVanguardMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(Messages::register);
        }

        @SubscribeEvent
        public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
            event.put(ModEntityTypes.WARRIOR_COMPANION.get(), WarriorCompanionEntity.createAttributes().build());
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        VanguardCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            VillageWarriorSpawner.tick(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();

        if (attacker instanceof Player player && victim instanceof WarriorCompanionEntity warrior) {
            if (warrior.isOwner(player) && player.getServer() != null) {
                CompanionSavedData data = CompanionSavedData.get(player.getServer());
                CompanionSavedData.FriendlyFireMode ffMode = data.getGlobalFriendlyFireMode();
                if (ffMode == CompanionSavedData.FriendlyFireMode.DISABLED) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level() instanceof ServerLevel serverLevel) {
            Entity attacker = event.getSource().getEntity();
            if (attacker == null) {
                attacker = event.getSource().getDirectEntity();
            }

            if (attacker instanceof WarriorCompanionEntity warrior && !(victim instanceof Player) && !(victim instanceof WarriorCompanionEntity)) {
                if (serverLevel.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT)) {
                    int xp = calculateMobExperience(victim);
                    if (xp > 0) {
                        net.minecraft.world.entity.ExperienceOrb.award(serverLevel, victim.position(), xp);
                    }
                }
            }
        }
    }

    private static int calculateMobExperience(LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            try {
                java.lang.reflect.Method m = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(
                        net.minecraft.world.entity.Mob.class, "m_213860_"); // getExperienceReward
                m.setAccessible(true);
                return (int) m.invoke(mob);
            } catch (Exception ignored) {
                try {
                    java.lang.reflect.Method m = net.minecraft.world.entity.Mob.class.getDeclaredMethod("getExperienceReward");
                    m.setAccessible(true);
                    return (int) m.invoke(mob);
                } catch (Exception ignored2) {
                }
            }

            if (entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss) return 50;
            if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) return 500;
            if (entity instanceof net.minecraft.world.entity.monster.ElderGuardian) return 20;
            if (entity instanceof net.minecraft.world.entity.monster.Ravager || entity instanceof net.minecraft.world.entity.monster.Evoker) return 20;
            if (entity instanceof net.minecraft.world.entity.monster.Blaze || entity instanceof net.minecraft.world.entity.monster.Guardian) return 10;
            if (entity instanceof net.minecraft.world.entity.monster.Monster) {
                int base = 5;
                for (net.minecraft.world.item.ItemStack armor : entity.getArmorSlots()) {
                    if (!armor.isEmpty()) base += 1;
                }
                return base;
            }
            if (entity instanceof net.minecraft.world.entity.animal.Animal) {
                return entity.isBaby() ? 0 : (1 + entity.level().random.nextInt(3));
            }
            return 3;
        }
        return 0;
    }
}
