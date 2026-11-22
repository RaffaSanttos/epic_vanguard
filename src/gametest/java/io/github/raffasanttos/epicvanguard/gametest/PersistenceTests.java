package io.github.raffasanttos.epicvanguard.gametest;

import io.github.raffasanttos.epicvanguard.EpicVanguardMod;
import io.github.raffasanttos.epicvanguard.entity.CompanionSavedData;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static io.github.raffasanttos.epicvanguard.gametest.TestUtil.*;

/**
 * Registro de companheiros entre carregamentos de chunk: rename, cópias antigas, mortos e dispensados.
 * Cada teste simula "chunk descarregado" salvando o NBT, removendo a entidade e recriando a partir do NBT.
 */
@GameTestHolder(EpicVanguardMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class PersistenceTests {

    /** Guerreiro recrutado e registrado; devolve o NBT salvo dele e já o remove do mundo. */
    private static CompoundTag recruitAndUnload(GameTestHelper helper, WarriorCompanionEntity warrior, String name) {
        recruit(warrior, UUID.randomUUID());
        warrior.setWarriorName(name);
        warrior.updateSavedData();
        CompoundTag tag = saveEntity(warrior);
        warrior.discard();
        return tag;
    }

    @GameTest(template = PLATFORM)
    public static void renameWhileUnloadedSurvivesReload(GameTestHelper helper) {
        CompanionSavedData data = data(helper);
        WarriorCompanionEntity warrior = warrior(helper, 4, 4);
        UUID uuid = warrior.getUUID();
        CompoundTag tag = recruitAndUnload(helper, warrior, "OldName");

        // O que o comando /companions rename faz com o guerreiro descarregado
        data.getCompanion(uuid).name = "NewName";

        WarriorCompanionEntity copy = loadCopy(helper, tag);
        checkEquals("NewName", data.getCompanion(uuid).name, "nome no registro depois de ler o NBT antigo");

        helper.getLevel().addFreshEntity(copy);
        checkEquals("NewName", copy.getWarriorName(), "nome do guerreiro depois de voltar ao mundo");

        copy.discard();
        data.unregister(uuid);
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void staleCopyIsDiscarded(GameTestHelper helper) {
        CompanionSavedData data = data(helper);
        WarriorCompanionEntity warrior = warrior(helper, 4, 4);
        UUID uuid = warrior.getUUID();
        CompoundTag tag = recruitAndUnload(helper, warrior, "Stale");

        // Outra cópia virou a oficial (ex.: foi rematerializada ao lado do dono)
        data.bumpGeneration(data.getCompanion(uuid));

        WarriorCompanionEntity oldCopy = loadCopy(helper, tag);
        helper.getLevel().addFreshEntity(oldCopy);
        check(oldCopy.isRemoved(), "a cópia antiga deveria ser descartada ao entrar no mundo (duplicação de itens)");

        data.unregister(uuid);
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void deadCompanionDoesNotReturn(GameTestHelper helper) {
        CompanionSavedData data = data(helper);
        WarriorCompanionEntity warrior = warrior(helper, 4, 4);
        UUID uuid = warrior.getUUID();
        CompoundTag tag = recruitAndUnload(helper, warrior, "Fallen");

        data.retire(uuid); // o que acontece quando ele morre

        WarriorCompanionEntity ghost = loadCopy(helper, tag);
        helper.getLevel().addFreshEntity(ghost);
        check(ghost.isRemoved(), "uma cópia de guerreiro morto não pode voltar ao mundo");
        check(data.getCompanion(uuid) == null, "guerreiro morto não deveria estar no registro");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void dismissedWhileUnloadedIsReleased(GameTestHelper helper) {
        CompanionSavedData data = data(helper);
        WarriorCompanionEntity warrior = warrior(helper, 4, 4);
        UUID uuid = warrior.getUUID();
        CompoundTag tag = recruitAndUnload(helper, warrior, "Released");

        data.dismiss(uuid); // /companions dismiss com ele descarregado

        WarriorCompanionEntity copy = loadCopy(helper, tag);
        helper.getLevel().addFreshEntity(copy);
        check(!copy.isRemoved(), "o guerreiro dispensado deveria continuar no mundo");
        check(!copy.isRecruited(), "o guerreiro dispensado deveria voltar a ser mercenário livre");
        check(copy.getOwnerUUID().isEmpty(), "o guerreiro dispensado não deveria ter dono");
        check(data.getCompanion(uuid) == null, "o guerreiro dispensado não deveria voltar ao registro");

        copy.discard();
        helper.succeed();
    }
}
