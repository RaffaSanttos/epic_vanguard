package io.github.raffasanttos.epicvanguard.event;

import io.github.raffasanttos.epicvanguard.EpicVanguardMod;
import io.github.raffasanttos.epicvanguard.command.VanguardCommand;
import io.github.raffasanttos.epicvanguard.dialogue.WarriorSpeechSystem;
import io.github.raffasanttos.epicvanguard.entity.CompanionSavedData;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.init.ModEntityTypes;
import io.github.raffasanttos.epicvanguard.networking.Messages;
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
            event.put(ModEntityTypes.BERSERKER_COMPANION.get(), WarriorCompanionEntity.createAttributes().build());
            event.put(ModEntityTypes.GUARDIAN_COMPANION.get(), WarriorCompanionEntity.createAttributes().build());
            event.put(ModEntityTypes.DUELIST_COMPANION.get(), WarriorCompanionEntity.createAttributes().build());
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
            if (warrior.isOwner(player)) {
                WarriorSpeechSystem.onFriendlyFire(warrior, player);
                if (player.getServer() != null) {
                    CompanionSavedData data = CompanionSavedData.get(player.getServer());
                    CompanionSavedData.FriendlyFireMode ffMode = data.getGlobalFriendlyFireMode();
                    if (ffMode == CompanionSavedData.FriendlyFireMode.DISABLED) {
                        event.setCanceled(true);
                    }
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

            if (victim instanceof Player || victim instanceof WarriorCompanionEntity) return;
            boolean warriorKill = attacker instanceof WarriorCompanionEntity;
            boolean warriorAssist = victim.getLastHurtByMob() instanceof WarriorCompanionEntity;
            if (!warriorKill && !warriorAssist && !(attacker instanceof Player)) return;

            int xp = calculateMobExperience(victim);
            if (xp <= 0) xp = 5;
            double xpMult = io.github.raffasanttos.epicvanguard.config.EpicVanguardConfig.xpMultiplier();

            // 1. Golpe fatal pelo guerreiro companheiro
            if (attacker instanceof WarriorCompanionEntity warrior) {
                warrior.addWarriorExperience((int) Math.round(Math.max(15, xp * 2) * xpMult));
                WarriorSpeechSystem.onKillTarget(warrior, victim);
                if (serverLevel.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT)) {
                    net.minecraft.world.entity.ExperienceOrb.award(serverLevel, victim.position(), xp);
                }
            }
            // 2. Assistência de combate pelo guerreiro (se ele infligiu dano recente ao monstro)
            else if (victim.getLastHurtByMob() instanceof WarriorCompanionEntity assistWarrior) {
                assistWarrior.addWarriorExperience((int) Math.round(Math.max(10, xp) * xpMult));
            }
            // 3. Jogador abateu monstro: companheiros recrutados próximos (raio de 24 blocos) ganham XP de combate conjunto.
            // Só monstros: antes matar galinhas ou aldeões também dava XP aos companheiros (farm fácil de nível).
            else if (attacker instanceof Player player && victim instanceof net.minecraft.world.entity.monster.Enemy) {
                java.util.List<WarriorCompanionEntity> nearbyCompanions = serverLevel.getEntitiesOfClass(
                        WarriorCompanionEntity.class,
                        player.getBoundingBox().inflate(io.github.raffasanttos.epicvanguard.config.EpicVanguardConfig.playerKillXpRadius()),
                        c -> c.isAlive() && c.isRecruited() && c.isOwner(player)
                );
                for (WarriorCompanionEntity companion : nearbyCompanions) {
                    companion.addWarriorExperience((int) Math.round(Math.max(10, xp) * xpMult));
                }
            }
        }
    }

    /** LivingEntity#getExperienceReward (protegido), resolvido uma única vez. */
    private static java.lang.reflect.Method experienceRewardMethod;
    private static boolean experienceRewardLookupDone = false;

    private static java.lang.reflect.Method getExperienceRewardMethod() {
        if (!experienceRewardLookupDone) {
            experienceRewardLookupDone = true;
            try {
                experienceRewardMethod = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(
                        LivingEntity.class, "m_213860_"); // getExperienceReward
            } catch (Exception e) {
                EpicVanguardMod.LOGGER.warn("Epic Vanguard: getExperienceReward not found, using XP estimates", e);
            }
        }
        return experienceRewardMethod;
    }

    private static int calculateMobExperience(LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            java.lang.reflect.Method m = getExperienceRewardMethod();
            if (m != null) {
                try {
                    return (int) m.invoke(mob);
                } catch (Exception ignored) {
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
