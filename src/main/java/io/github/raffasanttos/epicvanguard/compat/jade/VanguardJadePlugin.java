package io.github.raffasanttos.epicvanguard.compat.jade;

import io.github.raffasanttos.epicvanguard.EpicVanguardMod;
import io.github.raffasanttos.epicvanguard.compat.WarriorTooltipLines;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** Integração com o Jade: nível, especialização, dono, modo e pontos de talento do guerreiro. Só carregada se o Jade estiver instalado. */
@WailaPlugin
public class VanguardJadePlugin implements IWailaPlugin {

    public static final ResourceLocation WARRIOR_INFO = new ResourceLocation(EpicVanguardMod.MOD_ID, "warrior_info");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(WarriorProvider.INSTANCE, WarriorCompanionEntity.class);
    }

    public enum WarriorProvider implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof WarriorCompanionEntity warrior) {
                for (Component line : WarriorTooltipLines.build(warrior, accessor.getPlayer())) {
                    tooltip.add(line);
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return WARRIOR_INFO;
        }
    }
}
