package io.github.raffasanttos.epicvanguard.gametest;

import io.github.raffasanttos.epicvanguard.EpicVanguardMod;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.entity.CompanionSavedData;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorEquipment;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorFormation;
import net.minecraft.world.phys.Vec3;
import io.github.raffasanttos.epicvanguard.init.ModEntityTypes;
import io.github.raffasanttos.epicvanguard.inventory.WarriorInventory;
import io.github.raffasanttos.epicvanguard.item.VanguardHornItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * O que entrou na 1.0.20: auto-equipar, coleta ligada/desligada, alvos da Corneta, regra da Guarda,
 * itens da morte e o marco do nível 20. Rodar com {@code ./gradlew runGameTestServer}.
 */
@GameTestHolder(EpicVanguardMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class Release120Tests {

    private static final String PLATFORM = TestUtil.PLATFORM;
    private static final BlockPos CENTER = new BlockPos(4, 1, 4);

    private static WarriorCompanionEntity spawn(GameTestHelper helper, EntityType<? extends WarriorCompanionEntity> type, int dx, int dz) {
        return helper.spawnWithNoFreeWill(type, CENTER.offset(dx, 0, dz));
    }

    private static void recruit(WarriorCompanionEntity w, UUID owner) {
        w.setOwnerUUID(owner);
        w.setRecruited(true);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }

    // ── auto-equipar ─────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void autoEquipWearsBetterArmorAndKeepsOldOne(GameTestHelper helper) {
        WarriorCompanionEntity w = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 0, 0);
        recruit(w, UUID.randomUUID());
        WarriorInventory inv = w.getWarriorInventory();
        inv.setItem(WarriorInventory.SLOT_HELMET, new ItemStack(Items.LEATHER_HELMET));
        inv.setItem(WarriorInventory.SLOT_BACKPACK_START, new ItemStack(Items.IRON_HELMET));

        WarriorEquipment.autoEquipNow(w);
        check(helper, inv.getItem(WarriorInventory.SLOT_HELMET).is(Items.IRON_HELMET), "deveria vestir o capacete de ferro");
        check(helper, inv.getItem(WarriorInventory.SLOT_BACKPACK_START).is(Items.LEATHER_HELMET), "o capacete de couro deveria voltar para a mochila; slot=" + inv.getItem(WarriorInventory.SLOT_BACKPACK_START) + " inv=" + java.util.stream.IntStream.range(0, inv.getContainerSize()).mapToObj(i -> i + ":" + inv.getItem(i)).toList());
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void archerNeverGetsShieldFromAutoEquip(GameTestHelper helper) {
        WarriorCompanionEntity archer = spawn(helper, ModEntityTypes.ARCHER_COMPANION.get(), 0, 0);
        recruit(archer, UUID.randomUUID());
        WarriorInventory inv = archer.getWarriorInventory();
        inv.setItem(WarriorInventory.SLOT_WEAPON_OFF, ItemStack.EMPTY);
        inv.setItem(WarriorInventory.SLOT_BACKPACK_END, new ItemStack(Items.SHIELD));

        WarriorEquipment.autoEquipNow(archer);
        check(helper, inv.getItem(WarriorInventory.SLOT_WEAPON_OFF).isEmpty(), "o Arqueiro não deveria receber escudo");
        check(helper, inv.getItem(WarriorInventory.SLOT_BACKPACK_END).is(Items.SHIELD), "o escudo deveria continuar na mochila");
        helper.succeed();
    }

    // ── coleta: só ligada ou desligada ───────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void pickupIsOnlyOnOrOff(GameTestHelper helper) {
        WarriorCompanionEntity w = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 0, 0);
        check(helper, w.getPickupMode() == WarriorCompanionEntity.PICKUP_OFF, "a coleta deveria começar desligada");

        w.setPickupMode(1); // antigo "só úteis"
        check(helper, w.getPickupMode() == WarriorCompanionEntity.PICKUP_ALL, "o antigo modo só úteis deveria virar 'tudo'");
        w.setPickupMode(WarriorCompanionEntity.PICKUP_OFF);
        check(helper, !w.isPickupItems(), "desligada não deveria pegar nada");

        // Save antigo (1.0.18) com o liga/desliga
        CompoundTag tag = new CompoundTag();
        w.saveWithoutId(tag);
        tag.remove("PickupMode");
        tag.putBoolean("PickupItems", true);
        WarriorCompanionEntity copy = ModEntityTypes.WARRIOR_COMPANION.get().create(helper.getLevel());
        check(helper, copy != null, "não criou a cópia");
        copy.load(tag);
        check(helper, copy.getPickupMode() == WarriorCompanionEntity.PICKUP_ALL, "PickupItems=true antigo deveria virar 'tudo'");
        helper.succeed();
    }

    // ── corneta ──────────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void hornRefusesProtectedTargets(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, CENTER.offset(2, 0, 0));
        check(helper, VanguardHornItem.isValidOrderTarget(zombie), "zumbi deveria ser alvo válido");

        check(helper, !VanguardHornItem.isValidOrderTarget(helper.spawnWithNoFreeWill(EntityType.VILLAGER, CENTER.offset(-2, 0, 0))), "aldeão não pode ser alvo");
        check(helper, !VanguardHornItem.isValidOrderTarget(helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, CENTER.offset(0, 0, 2))), "golem de ferro não pode ser alvo");
        check(helper, !VanguardHornItem.isValidOrderTarget(helper.spawn(EntityType.ARMOR_STAND, CENTER.offset(0, 0, -2))), "suporte de armadura não pode ser alvo");
        check(helper, !VanguardHornItem.isValidOrderTarget(spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 2, 2)), "companheiro não pode ser alvo");

        Wolf wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, CENTER.offset(-2, 0, -2));
        check(helper, VanguardHornItem.isValidOrderTarget(wolf), "lobo selvagem pode ser alvo");
        wolf.setTame(true);
        wolf.setOwnerUUID(UUID.randomUUID()); // pet de OUTRO jogador
        check(helper, !VanguardHornItem.isValidOrderTarget(wolf), "pet de outro jogador não pode ser alvo");
        helper.succeed();
    }

    // ── Guarda: a mesma regra para escolher e para manter o alvo ──────────────

    @GameTest(template = PLATFORM)
    public static void guardOnlyEngagesNearItsPost(GameTestHelper helper) {
        WarriorCompanionEntity w = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 0, 0);
        recruit(w, UUID.randomUUID());
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, CENTER.offset(2, 0, 0));

        w.setCombatMode(WarriorCompanionEntity.MODE_FOLLOW);
        check(helper, w.canEngageFromGuard(zombie), "fora da Guarda não há limite");

        w.setCombatMode(WarriorCompanionEntity.MODE_GUARD);
        w.setGuardPos(zombie.blockPosition());
        check(helper, w.canEngageFromGuard(zombie), "alvo junto do posto deveria ser aceito");

        w.setGuardPos(zombie.blockPosition().offset(12, 0, 0)); // posto a 12 blocos: dentro do raio padrão (16)
        check(helper, w.canEngageFromGuard(zombie), "monstro a 12 blocos do posto deveria ser atacado");

        w.setGuardPos(zombie.blockPosition().offset(30, 0, 0)); // posto a 30 blocos do zumbi
        check(helper, !w.canEngageFromGuard(zombie), "alvo longe do posto não deveria ser escolhido");

        w.setTarget(zombie);
        w.setHornOrderTarget(zombie);
        check(helper, w.canEngageFromGuard(zombie), "ordem da Corneta deveria valer mesmo longe do posto");
        check(helper, w.isOnHornOrder(), "deveria estar cumprindo a ordem");
        w.clearCombatTarget();
        check(helper, !w.isOnHornOrder(), "sem alvo a ordem acaba (e ele volta ao posto)");
        helper.succeed();
    }

    // ── morte e nível 20 ─────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void deathDropsEverythingOnce(GameTestHelper helper) {
        WarriorCompanionEntity w = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 0, 0);
        WarriorInventory inv = w.getWarriorInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) inv.setItem(i, ItemStack.EMPTY);
        inv.setItem(WarriorInventory.SLOT_WEAPON_MAIN, new ItemStack(Items.IRON_SWORD));
        inv.setItem(WarriorInventory.SLOT_BACKPACK_START, new ItemStack(Items.DIAMOND, 3));

        w.hurt(w.damageSources().genericKill(), 10000.0F);
        check(helper, !w.isAlive(), "o guerreiro deveria morrer");
        helper.assertItemEntityCountIs(Items.IRON_SWORD, CENTER, 4.0D, 1);
        helper.assertItemEntityCountIs(Items.DIAMOND, CENTER, 4.0D, 3);
        check(helper, inv.isEmpty(), "o inventário deveria ficar vazio depois da morte");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void levelTwentyShortensWarcry(GameTestHelper helper) {
        WarriorCompanionEntity w = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 0, 0);
        int base = w.getWarcryCooldownTotal();
        w.setWarriorLevel(20);
        int at20 = w.getWarcryCooldownTotal();
        check(helper, at20 == Math.max(100, (int) (base * 0.80D)), "nível 20 deveria reduzir o grito em 20%: " + base + " -> " + at20);
        helper.succeed();
    }

    // ── formações ────────────────────────────────────────────────────────────

    private static boolean near(Vec3 a, double x, double z) {
        return a != null && Math.abs(a.x - x) < 0.01D && Math.abs(a.z - z) < 0.01D;
    }

    @GameTest(template = PLATFORM)
    public static void formationSlotsFollowOwnerFacing(GameTestHelper helper) {
        Vec3 o = Vec3.ZERO;
        // Dono olhando para o sul (yaw 0): frente = +Z, direita = -X
        check(helper, WarriorFormation.slotPosition(WarriorFormation.FREE, 0, 1, o, 0.0F) == null, "livre não tem lugar fixo");
        check(helper, near(WarriorFormation.slotPosition(WarriorFormation.COLUMN, 0, 3, o, 0.0F), 0.0D, -2.5D), "coluna: 1º atrás do dono");
        check(helper, near(WarriorFormation.slotPosition(WarriorFormation.COLUMN, 1, 3, o, 0.0F), 0.0D, -5.0D), "coluna: 2º atrás do 1º");
        check(helper, near(WarriorFormation.slotPosition(WarriorFormation.LINE, 1, 3, o, 0.0F), 0.0D, -2.5D), "linha: o do meio atrás do dono");
        check(helper, near(WarriorFormation.slotPosition(WarriorFormation.LINE, 0, 3, o, 0.0F), 2.0D, -2.5D), "linha: o 1º à esquerda do dono");
        // Girando o dono para o leste (yaw -90): a coluna fica a oeste dele
        check(helper, near(WarriorFormation.slotPosition(WarriorFormation.COLUMN, 0, 1, o, -90.0F), -2.5D, 0.0D), "coluna deveria girar com o dono");
        // Círculo: todos a 3 blocos e em lugares diferentes
        Vec3 a = WarriorFormation.slotPosition(WarriorFormation.CIRCLE, 0, 4, o, 0.0F);
        Vec3 b = WarriorFormation.slotPosition(WarriorFormation.CIRCLE, 1, 4, o, 0.0F);
        check(helper, Math.abs(a.length() - 3.0D) < 0.01D && Math.abs(b.length() - 3.0D) < 0.01D, "círculo de raio 3");
        check(helper, a.distanceTo(b) > 1.0D, "lugares do círculo deveriam ser diferentes");
        check(helper, WarriorFormation.next(WarriorFormation.CIRCLE) == WarriorFormation.FREE, "depois do círculo volta para livre");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void formationIsSavedPerOwner(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        CompanionSavedData data = new CompanionSavedData();
        check(helper, data.getFormation(owner) == WarriorFormation.FREE, "sem escolha deveria ser livre");
        data.setFormation(owner, WarriorFormation.LINE);
        CompanionSavedData loaded = CompanionSavedData.load(data.save(new CompoundTag()));
        check(helper, loaded.getFormation(owner) == WarriorFormation.LINE, "a formação deveria sobreviver ao save");
        check(helper, loaded.getFormation(UUID.randomUUID()) == WarriorFormation.FREE, "outro dono continua livre");
        helper.succeed();
    }
}
