package io.github.raffasanttos.epicvanguard.entity;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * Talentos da árvore de habilidades. O guerreiro ganha 1 ponto a cada 2 níveis (10 no nível 20)
 * e cada talento custa 1 ponto. Os desbloqueados ficam num bitmask sincronizado ({@code Perks} no NBT).
 *
 * <p>Os talentos {@link #SPEC_T1} e {@link #SPEC_T2} mudam de efeito conforme a especialização;
 * por isso o nome e a descrição vêm de chaves {@code epicvanguard.perk.<id>.<spec>.*}.</p>
 *
 * <p>NÃO mude a ordem/IDs: o bit de cada talento é salvo no mundo.</p>
 */
public enum WarriorPerk {
    VITALITY(0, "vitality", 2, null, false),
    FIELD_MEDIC(1, "field_medic", 4, null, false),
    IRON_SKIN(2, "iron_skin", 4, VITALITY, false),
    MARKSMAN(3, "marksman", 6, null, false),
    CAREFUL_GEAR(4, "careful_gear", 6, null, false),
    SWIFT_FEET(5, "swift_feet", 8, null, false),
    SPEC_T1(6, "spec_t1", 10, null, true),
    SPEC_T2(7, "spec_t2", 14, SPEC_T1, true),
    SECOND_WIND(8, "second_wind", 16, FIELD_MEDIC, false),
    LEGEND(9, "legend", 20, SPEC_T2, false);

    /** Níveis por ponto de talento. */
    public static final int LEVELS_PER_POINT = 2;

    private final int bit;
    private final String id;
    private final int requiredLevel;
    @Nullable
    private final WarriorPerk prerequisite;
    private final boolean specSpecific;

    WarriorPerk(int bit, String id, int requiredLevel, @Nullable WarriorPerk prerequisite, boolean specSpecific) {
        this.bit = bit;
        this.id = id;
        this.requiredLevel = requiredLevel;
        this.prerequisite = prerequisite;
        this.specSpecific = specSpecific;
    }

    public int mask() { return 1 << bit; }
    public String getId() { return id; }
    public int getRequiredLevel() { return requiredLevel; }
    @Nullable public WarriorPerk getPrerequisite() { return prerequisite; }

    private String keyBase(int specialization) {
        if (!specSpecific) return "epicvanguard.perk." + id;
        String spec = switch (specialization) {
            case WarriorCompanionEntity.SPEC_BERSERKER -> "berserker";
            case WarriorCompanionEntity.SPEC_GUARDIAN -> "guardian";
            case WarriorCompanionEntity.SPEC_DUELIST -> "duelist";
            case WarriorCompanionEntity.SPEC_ARCHER -> "archer";
            case WarriorCompanionEntity.SPEC_STANDARD_BEARER -> "standard_bearer";
            default -> "warrior";
        };
        return "epicvanguard.perk." + id + "." + spec;
    }

    public Component getName(int specialization) {
        return Component.translatable(keyBase(specialization) + ".name");
    }

    public Component getDescription(int specialization) {
        return Component.translatable(keyBase(specialization) + ".desc");
    }

    @Nullable
    public static WarriorPerk byOrdinal(int ordinal) {
        WarriorPerk[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    /** Pontos totais ganhos até este nível. */
    public static int pointsForLevel(int level) {
        return Math.max(0, level / LEVELS_PER_POINT);
    }

    /** Remove bits desconhecidos (ex.: de uma versão futura) e limita ao que cabe. */
    public static int sanitize(int mask) {
        int valid = 0;
        for (WarriorPerk perk : values()) valid |= perk.mask();
        return mask & valid;
    }
}
