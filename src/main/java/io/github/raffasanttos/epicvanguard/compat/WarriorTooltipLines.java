package io.github.raffasanttos.epicvanguard.compat;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Linhas de tooltip do guerreiro compartilhadas pelas integrações Jade e WTHIT.
 * Só usa dados sincronizados com o cliente (não precisa de pacote extra do servidor).
 */
public final class WarriorTooltipLines {

    private WarriorTooltipLines() {}

    public static List<Component> build(WarriorCompanionEntity warrior, Player viewer) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("epicvanguard.tooltip.waila.level",
                warrior.getWarriorLevel(), warrior.getFormattedSpecializationTitle()));

        if (warrior.isPrisoner()) {
            lines.add(Component.translatable("epicvanguard.tooltip.waila.prisoner"));
            return lines;
        }

        if (!warrior.isRecruited()) {
            lines.add(Component.translatable("epicvanguard.tooltip.waila.wild", warrior.getRecruitCost()));
            return lines;
        }

        Player owner = warrior.getOwner();
        if (owner != null) {
            lines.add(Component.translatable("epicvanguard.tooltip.waila.owner", owner.getDisplayName()));
        }
        String modeKey = switch (warrior.getCombatMode()) {
            case 1 -> "epicvanguard.mode.guard";
            case 2 -> "epicvanguard.mode.stay";
            default -> "epicvanguard.mode.follow";
        };
        lines.add(Component.translatable("epicvanguard.tooltip.waila.mode", Component.translatable(modeKey)));

        // Pontos de talento: só interessam ao dono
        if (warrior.isOwner(viewer) && warrior.getSkillPoints() > 0) {
            lines.add(Component.translatable("epicvanguard.tooltip.waila.points", warrior.getSkillPoints()));
        }
        return lines;
    }
}
