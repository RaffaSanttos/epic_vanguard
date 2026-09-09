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

import java.util.List;

@Mod.EventBusSubscriber(modid = EpicVanguardMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EpicFightCompat {

    private static List<net.minecraft.world.entity.EntityType<?>> getCompanionTypes() {
        return List.of(
                ModEntityTypes.WARRIOR_COMPANION.get(),
                ModEntityTypes.BERSERKER_COMPANION.get(),
                ModEntityTypes.GUARDIAN_COMPANION.get(),
                ModEntityTypes.DUELIST_COMPANION.get()
        );
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (var type : getCompanionTypes()) {
                EntityPatchProvider.putCustomEntityPatch(type, entity -> () -> new WarriorCompanionPatch());
                Armatures.registerEntityTypeArmature(type, Armatures.BIPED);
            }
        });
    }

    @SubscribeEvent
    public static void registerEntityPatch(EntityPatchRegistryEvent event) {
        for (var type : getCompanionTypes()) {
            Armatures.registerEntityTypeArmature(type, Armatures.BIPED);
            event.getTypeEntry().put(type, entity -> () -> new WarriorCompanionPatch());
        }
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
                for (var type : getCompanionTypes()) {
                    EntityPatchProvider.putCustomEntityPatch(type, entity -> () -> new WarriorCompanionPatch());
                    Armatures.registerEntityTypeArmature(type, Armatures.BIPED);
                }
            });
        }

        @SubscribeEvent
        public static void registerPatchedRenderers(PatchedRenderersEvent.Add event) {
            for (var type : getCompanionTypes()) {
                Armatures.registerEntityTypeArmature(type, Armatures.BIPED);
                event.addPatchedEntityRenderer(type,
                        entityType -> new PCustomHumanoidEntityRenderer(Meshes.BIPED_OUTLAYER, event.getContext(), entityType));
            }
        }
    }
}
