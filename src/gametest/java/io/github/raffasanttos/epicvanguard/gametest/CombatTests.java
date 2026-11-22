package io.github.raffasanttos.epicvanguard.gametest;

import io.github.raffasanttos.epicvanguard.EpicVanguardMod;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.inventory.WarriorInventory;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.raffasanttos.epicvanguard.gametest.TestUtil.*;

/** Ataque do guerreiro: encantamentos e durabilidade da arma (MELHORIAS 1.2). */
@GameTestHolder(EpicVanguardMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class CombatTests {

    private static float damageDealt(GameTestHelper helper, ItemStack weapon, int x) {
        WarriorCompanionEntity warrior = warrior(helper, x, 2);
        Zombie zombie = zombie(helper, x, 4);
        giveWeapon(warrior, weapon);
        warrior.doHurtTarget(zombie);
        return zombie.getMaxHealth() - zombie.getHealth();
    }

    @GameTest(template = PLATFORM)
    public static void sharpnessIncreasesDamage(GameTestHelper helper) {
        float plain = damageDealt(helper, new ItemStack(Items.IRON_SWORD), 1);
        ItemStack sharp = new ItemStack(Items.IRON_SWORD);
        sharp.enchant(Enchantments.SHARPNESS, 5);
        float enchanted = damageDealt(helper, sharp, 6);

        check(plain > 0.0F, "o golpe sem encantamento não causou dano");
        // Afiação V soma 3 de dano; a armadura do zumbi reduz um pouco
        check(enchanted >= plain + 2.0F,
                "Afiação V deveria aumentar o dano: sem = " + plain + ", com = " + enchanted);
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void weaponLosesDurability(GameTestHelper helper) {
        WarriorCompanionEntity warrior = warrior(helper, 2, 2);
        Zombie zombie = zombie(helper, 2, 4);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        giveWeapon(warrior, sword);

        warrior.doHurtTarget(zombie);

        ItemStack inSlot = warrior.getWarriorInventory().getItem(WarriorInventory.SLOT_WEAPON_MAIN);
        checkEquals(1, inSlot.getDamageValue(), "dano na espada depois de 1 golpe");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void brokenWeaponLeavesInventory(GameTestHelper helper) {
        WarriorCompanionEntity warrior = warrior(helper, 2, 2);
        Zombie zombie = zombie(helper, 2, 4);
        ItemStack sword = new ItemStack(Items.WOODEN_SWORD);
        sword.setDamageValue(sword.getMaxDamage() - 1); // 1 de durabilidade restante
        giveWeapon(warrior, sword);

        warrior.doHurtTarget(zombie);

        check(warrior.getWarriorInventory().getItem(WarriorInventory.SLOT_WEAPON_MAIN).isEmpty(),
                "a espada quebrada deveria sumir do inventário");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void fireAspectIgnitesTarget(GameTestHelper helper) {
        WarriorCompanionEntity warrior = warrior(helper, 2, 2);
        Zombie zombie = zombie(helper, 2, 4);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(Enchantments.FIRE_ASPECT, 2);
        giveWeapon(warrior, sword);

        warrior.doHurtTarget(zombie);

        check(zombie.getRemainingFireTicks() > 0, "Aspecto Flamejante deveria pôr fogo no alvo");
        helper.succeed();
    }
}
