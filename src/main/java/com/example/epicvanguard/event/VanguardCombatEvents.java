package com.example.epicvanguard.event;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.command.VanguardCommand;
import com.example.epicvanguard.entity.CompanionSavedData;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModEntityTypes;
import com.example.epicvanguard.networking.Messages;
import com.example.epicvanguard.world.VillageCompanionSpawner;
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
            VillageCompanionSpawner.tick(serverLevel);
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
}
