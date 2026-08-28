package com.example.epicvanguard.compat.epicfight;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.init.ModEntityTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.forgeevent.EntityPatchRegistryEvent;
import yesman.epicfight.client.renderer.patched.entity.PCustomHumanoidEntityRenderer;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.provider.EntityPatchProvider;

@Mod.EventBusSubscriber(modid = EpicVanguardMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EpicFightCompat {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            EntityPatchProvider.putCustomEntityPatch(ModEntityTypes.WARRIOR_COMPANION.get(), entity -> () -> new WarriorCompanionPatch());
            Armatures.registerEntityTypeArmature(ModEntityTypes.WARRIOR_COMPANION.get(), Armatures.BIPED);
        });
    }

    @SubscribeEvent
    public static void registerEntityPatch(EntityPatchRegistryEvent event) {
        Armatures.registerEntityTypeArmature(ModEntityTypes.WARRIOR_COMPANION.get(), Armatures.BIPED);
        event.getTypeEntry().put(ModEntityTypes.WARRIOR_COMPANION.get(), entity -> () -> new WarriorCompanionPatch());
    }

    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        WarriorCompanionPatch.initAttributes(event);
    }

    public static void playDodgeRollAnimation(com.example.epicvanguard.entity.WarriorCompanionEntity entity) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("epicfight")) return;
        try {
            yesman.epicfight.world.capabilities.entitypatch.MobPatch<?> patch =
                    yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(entity, yesman.epicfight.world.capabilities.entitypatch.MobPatch.class);
            if (patch != null) {
                patch.playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BIPED_ROLL_BACKWARD, 0.0F);
            }
        } catch (Throwable ignored) {}
    }

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = EpicVanguardMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientEpicFightEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                EntityPatchProvider.putCustomEntityPatch(ModEntityTypes.WARRIOR_COMPANION.get(), entity -> () -> new WarriorCompanionPatch());
                Armatures.registerEntityTypeArmature(ModEntityTypes.WARRIOR_COMPANION.get(), Armatures.BIPED);
            });
        }

        @SubscribeEvent
        public static void registerPatchedRenderers(PatchedRenderersEvent.Add event) {
            Armatures.registerEntityTypeArmature(ModEntityTypes.WARRIOR_COMPANION.get(), Armatures.BIPED);
            event.addPatchedEntityRenderer(ModEntityTypes.WARRIOR_COMPANION.get(),
                    type -> new PCustomHumanoidEntityRenderer(Meshes.BIPED_OUTLAYER, event.getContext(), type));
        }
    }
}
