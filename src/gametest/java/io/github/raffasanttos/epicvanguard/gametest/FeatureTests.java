package io.github.raffasanttos.epicvanguard.gametest;

import io.github.raffasanttos.epicvanguard.EpicVanguardMod;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.entity.WarriorPerk;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorEquipmentWear;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorMountHelper;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorRangedHelper;
import io.github.raffasanttos.epicvanguard.init.ModEntityTypes;
import io.github.raffasanttos.epicvanguard.inventory.WarriorInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * Recursos novos: talentos, desgaste/aviso/reparo, combate à distância, Arqueiro, Porta-estandarte e montarias.
 * Rodar com {@code ./gradlew runGameTestServer}.
 */
@GameTestHolder(EpicVanguardMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class FeatureTests {

    private static final String PLATFORM = TestUtil.PLATFORM;
    private static final BlockPos CENTER = new BlockPos(4, 1, 4);

    /** Guerreiro sem IA (testa a lógica, não a IA andando por aí). */
    private static <T extends WarriorCompanionEntity> T spawn(GameTestHelper helper, EntityType<T> type, int dx, int dz) {
        return helper.spawnWithNoFreeWill(type, CENTER.offset(dx, 0, dz));
    }

    /*
     * O dono é só um UUID: o jogador falso do GameTest não tem conexão de rede e qualquer pacote
     * enviado a ele (efeito, mensagem, modo de jogo) derruba o teste.
     */
    private static void recruit(WarriorCompanionEntity w, UUID owner) {
        w.setOwnerUUID(owner);
        w.setRecruited(true);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }

    // ── árvore de talentos ───────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void perksApplyAndPersist(GameTestHelper helper) {
        WarriorCompanionEntity w = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 0, 0);
        w.setWarriorLevel(20);
        float before = w.getMaxHealth();
        check(helper, w.getSkillPoints() == 10, "nível 20 deveria dar 10 pontos, deu " + w.getSkillPoints());
        check(helper, w.unlockPerk(WarriorPerk.VITALITY), "Vitalidade deveria desbloquear");
        check(helper, Math.abs(w.getMaxHealth() - (before + 4.0F)) < 0.01F, "Vitalidade deveria dar +4 de vida máxima");
        check(helper, w.unlockPerk(WarriorPerk.IRON_SKIN), "Pele de Ferro deveria desbloquear depois de Vitalidade");

        CompoundTag tag = new CompoundTag();
        w.saveWithoutId(tag);
        WarriorCompanionEntity copy = ModEntityTypes.WARRIOR_COMPANION.get().create(helper.getLevel());
        check(helper, copy != null, "não criou a cópia");
        copy.load(tag);
        check(helper, copy.hasPerk(WarriorPerk.VITALITY) && copy.hasPerk(WarriorPerk.IRON_SKIN), "talentos não sobreviveram ao NBT");
        check(helper, copy.getSkillPoints() == 8, "pontos depois do NBT deveriam ser 8");
        check(helper, Math.abs(copy.getMaxHealth() - w.getMaxHealth()) < 0.01F, "vida máxima diferente depois do NBT");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void perksRespectRequirements(GameTestHelper helper) {
        WarriorCompanionEntity w = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 0, 0);
        w.setWarriorLevel(4); // 2 pontos
        check(helper, w.getPerkLockReason(WarriorPerk.IRON_SKIN) != null, "Pele de Ferro sem Vitalidade deveria estar bloqueada");
        check(helper, !w.unlockPerk(WarriorPerk.MARKSMAN), "Atirador exige nível 6");
        check(helper, w.unlockPerk(WarriorPerk.VITALITY), "Vitalidade deveria desbloquear");
        check(helper, w.unlockPerk(WarriorPerk.FIELD_MEDIC), "Médico de Campo deveria desbloquear");
        check(helper, w.getSkillPoints() == 0, "deveria ter gasto os 2 pontos");
        check(helper, !w.unlockPerk(WarriorPerk.IRON_SKIN), "sem pontos não deveria desbloquear");
        check(helper, !w.unlockPerk(WarriorPerk.VITALITY), "não deveria desbloquear duas vezes");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void secondWindCancelsDeathAndKeepsInventory(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        WarriorCompanionEntity w = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 1, 0);
        recruit(w, owner);
        w.setWarriorLevel(16);
        w.unlockPerk(WarriorPerk.FIELD_MEDIC);
        check(helper, w.unlockPerk(WarriorPerk.SECOND_WIND), "Segundo Fôlego deveria desbloquear");
        w.getWarriorInventory().setItem(WarriorInventory.SLOT_BACKPACK_START, new ItemStack(Items.DIAMOND));

        w.hurt(w.damageSources().generic(), 1000.0F);
        check(helper, w.isAlive() && w.getHealth() > 0.0F, "o guerreiro deveria sobreviver ao golpe fatal");
        check(helper, w.getSecondWindCooldown() > 0, "a recarga do Segundo Fôlego deveria começar");
        check(helper, w.getWarriorInventory().getItem(WarriorInventory.SLOT_BACKPACK_START).is(Items.DIAMOND),
                "a mochila não deveria ser esvaziada quando a morte é cancelada");
        helper.succeed();
    }

    // ── desgaste, aviso e reparo ─────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void brokenWeaponIsReplacedFromBackpack(GameTestHelper helper) {
        WarriorCompanionEntity w = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 0, 0);
        WarriorInventory inv = w.getWarriorInventory();
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.setDamageValue(sword.getMaxDamage() - 1);
        inv.setItem(WarriorInventory.SLOT_WEAPON_MAIN, sword);
        inv.setItem(WarriorInventory.SLOT_BACKPACK_START, new ItemStack(Items.WOODEN_SWORD));
        inv.setItem(WarriorInventory.SLOT_BACKPACK_START + 1, new ItemStack(Items.STONE_SWORD));

        WarriorEquipmentWear.damageMainWeapon(w, 1);
        check(helper, inv.getItem(WarriorInventory.SLOT_WEAPON_MAIN).is(Items.STONE_SWORD),
                "deveria equipar a melhor espada da mochila (pedra), equipou " + inv.getItem(WarriorInventory.SLOT_WEAPON_MAIN));
        check(helper, inv.getItem(WarriorInventory.SLOT_BACKPACK_START).is(Items.WOODEN_SWORD), "a espada de madeira deveria ficar na mochila");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void lowDurabilityIsFlaggedOnce(GameTestHelper helper) {
        WarriorCompanionEntity w = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 0, 0);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.setDamageValue(sword.getMaxDamage() - 12); // ~5% restante
        w.getWarriorInventory().setItem(WarriorInventory.SLOT_WEAPON_MAIN, sword);

        WarriorEquipmentWear.damageMainWeapon(w, 1);
        ItemStack after = w.getWarriorInventory().getItem(WarriorInventory.SLOT_WEAPON_MAIN);
        check(helper, after.hasTag() && after.getTag().getBoolean(WarriorEquipmentWear.LOW_WARNED_TAG),
                "item com pouca durabilidade deveria ser marcado como avisado");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void fieldRepairUsesBackpackMaterial(GameTestHelper helper) {
        WarriorCompanionEntity w = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 0, 0);
        WarriorInventory inv = w.getWarriorInventory();
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.setDamageValue(200);
        sword.getOrCreateTag().putBoolean(WarriorEquipmentWear.LOW_WARNED_TAG, true);
        inv.setItem(WarriorInventory.SLOT_WEAPON_MAIN, sword);
        inv.setItem(WarriorInventory.SLOT_BACKPACK_START, new ItemStack(Items.IRON_INGOT, 2));

        check(helper, WarriorEquipmentWear.tryFieldRepair(w), "deveria reparar");
        ItemStack after = inv.getItem(WarriorInventory.SLOT_WEAPON_MAIN);
        int expected = 200 - after.getMaxDamage() / 4;
        check(helper, after.getDamageValue() == expected, "dano esperado " + expected + ", ficou " + after.getDamageValue());
        check(helper, inv.getItem(WarriorInventory.SLOT_BACKPACK_START).getCount() == 1, "deveria gastar 1 lingote");
        check(helper, !after.hasTag() || !after.getTag().getBoolean(WarriorEquipmentWear.LOW_WARNED_TAG),
                "o aviso deveria ser liberado depois do reparo");

        inv.setItem(WarriorInventory.SLOT_BACKPACK_START, ItemStack.EMPTY);
        check(helper, !WarriorEquipmentWear.tryFieldRepair(w), "sem material não deveria reparar");
        helper.succeed();
    }

    // ── combate à distância e fogo amigo ─────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void warriorArrowsSpareAllies(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        WarriorCompanionEntity w = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 1, 0);
        recruit(w, owner);
        WarriorCompanionEntity ally = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 2, 0);
        recruit(ally, owner);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, CENTER.offset(-2, 0, 0));

        float allyHealth = ally.getHealth();
        boolean allyHurt = ally.hurt(w.damageSources().arrow(new Arrow(helper.getLevel(), w), w), 5.0F);
        check(helper, !allyHurt && ally.getHealth() == allyHealth, "a flecha do guerreiro não deveria ferir um aliado do mesmo dono");
        boolean zombieHurt = zombie.hurt(w.damageSources().arrow(new Arrow(helper.getLevel(), w), w), 5.0F);
        check(helper, zombieHurt, "a flecha do guerreiro deveria ferir o zumbi");
        helper.succeed();
    }

    // ── especializações ──────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void archerStartsWithBowAndArrows(GameTestHelper helper) {
        WarriorCompanionEntity archer = spawn(helper, ModEntityTypes.ARCHER_COMPANION.get(), 0, 0);
        archer.applyEquipmentTier(0);
        ItemStack main = archer.getWarriorInventory().getItem(WarriorInventory.SLOT_WEAPON_MAIN);
        check(helper, main.getItem() instanceof BowItem || main.getItem() instanceof CrossbowItem, "Arqueiro deveria ter arco, tem " + main);
        check(helper, WarriorRangedHelper.findArrowSlot(archer) != -1, "Arqueiro deveria ter flechas");
        check(helper, WarriorRangedHelper.canFightAtRange(archer), "Arqueiro deveria lutar à distância");
        check(helper, archer.getSpecialization() == WarriorCompanionEntity.SPEC_ARCHER, "especialização errada");

        WarriorCompanionEntity plain = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 2, 0);
        plain.getWarriorInventory().setItem(WarriorInventory.SLOT_WEAPON_MAIN, new ItemStack(Items.BOW));
        plain.syncEquipmentWithInventory();
        archer.syncEquipmentWithInventory();
        if (archer.getMainHandItem().getItem() instanceof BowItem) {
            check(helper, WarriorRangedHelper.getDrawTicks(archer) < WarriorRangedHelper.getDrawTicks(plain),
                    "Arqueiro deveria puxar o arco mais rápido");
        }
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void archerSwapsToMeleeWithoutArrows(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        WarriorCompanionEntity archer = spawn(helper, ModEntityTypes.ARCHER_COMPANION.get(), 0, 0);
        recruit(archer, owner);
        WarriorInventory inv = archer.getWarriorInventory();
        inv.setItem(WarriorInventory.SLOT_WEAPON_MAIN, new ItemStack(Items.BOW));
        inv.setItem(WarriorInventory.SLOT_BACKPACK_START, new ItemStack(Items.IRON_SWORD));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, CENTER.offset(-2, 0, 0));
        archer.setTarget(zombie);

        WarriorRangedHelper.manageWeaponSwap(archer);
        check(helper, inv.getItem(WarriorInventory.SLOT_WEAPON_MAIN).is(Items.IRON_SWORD), "sem flechas deveria sacar a espada");

        archer.setTarget(null);
        inv.setItem(WarriorInventory.SLOT_BACKPACK_START + 1, new ItemStack(Items.ARROW, 8));
        WarriorRangedHelper.manageWeaponSwap(archer);
        check(helper, inv.getItem(WarriorInventory.SLOT_WEAPON_MAIN).is(Items.BOW), "com flechas e fora de combate deveria voltar ao arco");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void standardBearerAuraProtectsAllies(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        WarriorCompanionEntity bearer = spawn(helper, ModEntityTypes.STANDARD_BEARER_COMPANION.get(), 0, 0);
        recruit(bearer, owner);
        bearer.applyEquipmentTier(0);
        check(helper, bearer.getWarriorInventory().getItem(WarriorInventory.SLOT_WEAPON_OFF).getItem() instanceof BannerItem,
                "Porta-estandarte deveria começar com estandarte");

        WarriorCompanionEntity ally = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 2, 0);
        recruit(ally, owner);
        WarriorCompanionEntity stranger = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), -2, 0);

        bearer.applyBannerAura();
        check(helper, ally.hasEffect(MobEffects.DAMAGE_RESISTANCE), "aliado deveria receber Resistência");
        check(helper, bearer.hasEffect(MobEffects.DAMAGE_RESISTANCE), "o próprio Porta-estandarte deveria receber Resistência");
        check(helper, !stranger.hasEffect(MobEffects.DAMAGE_RESISTANCE), "guerreiro de outro dono não deveria receber a aura");

        bearer.getWarriorInventory().setItem(WarriorInventory.SLOT_WEAPON_OFF, ItemStack.EMPTY);
        check(helper, !bearer.hasBannerRaised(), "sem estandarte não há aura");
        helper.succeed();
    }

    // ── montarias ────────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void warriorMountsOwnersSaddledHorse(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        WarriorCompanionEntity w = spawn(helper, ModEntityTypes.WARRIOR_COMPANION.get(), 1, 0);
        recruit(w, owner);

        Horse wild = helper.spawn(EntityType.HORSE, CENTER.offset(-2, 0, -2));
        check(helper, !WarriorMountHelper.isSuitableMount(w, wild), "cavalo selvagem não serve");

        Horse horse = helper.spawn(EntityType.HORSE, CENTER.offset(2, 0, 2));
        horse.setAge(0);
        horse.setTamed(true);
        horse.setOwnerUUID(owner);
        check(helper, !WarriorMountHelper.isSuitableMount(w, horse), "cavalo sem sela não serve");
        horse.equipSaddle(null);
        check(helper, WarriorMountHelper.isSuitableMount(w, horse), "cavalo domado e selado do dono deveria servir");

        Horse otherOwners = helper.spawn(EntityType.HORSE, CENTER.offset(-2, 0, 2));
        otherOwners.setAge(0);
        otherOwners.setTamed(true);
        otherOwners.setOwnerUUID(UUID.randomUUID());
        otherOwners.equipSaddle(null);
        check(helper, !WarriorMountHelper.isSuitableMount(w, otherOwners), "cavalo de outro jogador não serve");

        WarriorMountHelper.ride(w, horse);
        check(helper, w.getVehicle() == horse, "o guerreiro deveria estar montado");
        check(helper, horse.getControllingPassenger() == w, "o guerreiro deveria controlar o cavalo");
        check(helper, horse.getUUID().equals(w.getMountUUID()), "a montaria deveria ser lembrada");

        // Dono ausente/a pé: o guerreiro desmonta
        WarriorMountHelper.tickMounted(w);
        check(helper, !w.isPassenger(), "sem o dono montado o guerreiro deveria desmontar");
        helper.succeed();
    }
}
