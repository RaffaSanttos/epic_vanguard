package com.example.epicvanguard.compat.epicfight;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.init.ModEntityTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.forgeevent.EntityPatchRegistryEvent;
import yesman.epicfight.client.renderer.patched.entity.PCustomHumanoidEntityRenderer;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.provider.EntityPatchProvider;

import java.util.List;

/**
 * Integração com o Epic Fight. NÃO usa {@code @Mod.EventBusSubscriber}: o Forge registraria a classe
 * mesmo sem o Epic Fight instalado e o carregamento quebraria (NoClassDefFoundError nos tipos do Epic Fight).
 * É registrada manualmente por {@link #register(IEventBus)}, só quando {@code ModList.isLoaded("epicfight")}.
 * Fora deste pacote, só chame métodos daqui depois de checar {@link EpicVanguardMod#isEpicFightLoaded()}.
 */
public class EpicFightCompat {

    /** Registra os listeners do Epic Fight no barramento do mod. Só chamar com o Epic Fight carregado. */
    public static void register(IEventBus modEventBus) {
        modEventBus.register(EpicFightCompat.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(ClientEpicFightEvents.class);
        }
    }

    /** Armas do Epic Fight (WeaponItem ou item com capability de arma). */
    public static boolean isEpicFightWeapon(net.minecraft.world.item.ItemStack stack) {
        try {
            if (stack.getItem() instanceof yesman.epicfight.world.item.WeaponItem) {
                return true;
            }
            var cap = yesman.epicfight.world.capabilities.EpicFightCapabilities.getItemStackCapability(stack);
            return cap != null && !cap.isEmpty() && cap.getWeaponCategory() != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

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
        try {
            yesman.epicfight.world.capabilities.entitypatch.MobPatch<?> patch =
                    yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(entity, yesman.epicfight.world.capabilities.entitypatch.MobPatch.class);
            if (patch != null) {
                patch.playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BIPED_ROLL_BACKWARD, 0.0F);
            }
        } catch (Throwable ignored) {}
    }

    @OnlyIn(Dist.CLIENT)
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
