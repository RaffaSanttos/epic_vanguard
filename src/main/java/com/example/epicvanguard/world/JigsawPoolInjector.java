package com.example.epicvanguard.world;

import com.example.epicvanguard.EpicVanguardMod;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = EpicVanguardMod.MOD_ID)
public class JigsawPoolInjector {

    private static final ResourceLocation[] VILLAGE_POOLS = {};

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        // Taverns removed - companions spawn directly and naturally in villages
    }

    @SuppressWarnings("unchecked")
    public static void injectPoolElement(StructureTemplatePool pool, ResourceLocation poolId, String structureLocation, int weight) {
        StructurePoolElement element = StructurePoolElement.single(structureLocation)
                .apply(StructureTemplatePool.Projection.RIGID);

        try {
            Field rawTemplatesField = null;
            Field templatesField = null;

            for (Field field : StructureTemplatePool.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    if (field.getType().equals(List.class)) {
                        rawTemplatesField = field;
                    } else if (field.getType().equals(ObjectArrayList.class)) {
                        templatesField = field;
                    }
                }
            }

            if (rawTemplatesField != null) {
                List<Pair<StructurePoolElement, Integer>> rawList = (List<Pair<StructurePoolElement, Integer>>) rawTemplatesField.get(pool);
                List<Pair<StructurePoolElement, Integer>> newRawList = new ArrayList<>(rawList);
                newRawList.add(Pair.of(element, weight));
                rawTemplatesField.set(pool, newRawList);
            }

            if (templatesField != null) {
                ObjectArrayList<StructurePoolElement> templatesList = (ObjectArrayList<StructurePoolElement>) templatesField.get(pool);
                for (int i = 0; i < weight; i++) {
                    templatesList.add(element);
                }
            }
        } catch (Exception e) {
            EpicVanguardMod.LOGGER.error("Failed to inject {} into jigsaw pool {}: {}", structureLocation, poolId, e.getMessage());
        }
    }
}
