package io.github.raffasanttos.epicvanguard.compat.wthit;

import io.github.raffasanttos.epicvanguard.compat.WarriorTooltipLines;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import net.minecraft.network.chat.Component;

/** Integração com o WTHIT (registrada por waila_plugins.json, só no cliente e só se o WTHIT estiver instalado). */
public class VanguardWthitPlugin implements IWailaPlugin {

    @Override
    public void register(IRegistrar registrar) {
        registrar.addComponent(WarriorProvider.INSTANCE, TooltipPosition.BODY, WarriorCompanionEntity.class);
    }

    public enum WarriorProvider implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendBody(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof WarriorCompanionEntity warrior) {
                for (Component line : WarriorTooltipLines.build(warrior, accessor.getPlayer())) {
                    tooltip.addLine(line);
                }
            }
        }
    }
}
