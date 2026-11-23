package io.github.raffasanttos.epicvanguard.entity.util;

import io.github.raffasanttos.epicvanguard.config.EpicVanguardConfig;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.entity.WarriorPerk;
import io.github.raffasanttos.epicvanguard.inventory.WarriorInventory;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Arco e besta dos guerreiros: munição, tempo de puxada e disparo. */
public final class WarriorRangedHelper {

    private WarriorRangedHelper() {}

    public static boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
    }

    /** Tem arco/besta na mão principal, combate à distância ativo e munição. */
    public static boolean canFightAtRange(WarriorCompanionEntity warrior) {
        if (!EpicVanguardConfig.rangedCombat()) return false;
        ItemStack weapon = warrior.getWarriorInventory().getItem(WarriorInventory.SLOT_WEAPON_MAIN);
        return isRangedWeapon(weapon) && hasAmmo(warrior, weapon);
    }

    private static boolean hasInfiniteAmmo(ItemStack weapon) {
        return EpicVanguardConfig.infiniteArrows()
                || (weapon.getItem() instanceof BowItem && weapon.getEnchantmentLevel(Enchantments.INFINITY_ARROWS) > 0);
    }

    public static boolean hasAmmo(WarriorCompanionEntity warrior, ItemStack weapon) {
        return hasInfiniteAmmo(weapon) || findArrowSlot(warrior) != -1;
    }

    /** Slot da mochila com flechas, ou -1. */
    public static int findArrowSlot(WarriorCompanionEntity warrior) {
        WarriorInventory inv = warrior.getWarriorInventory();
        for (int i = WarriorInventory.SLOT_BACKPACK_START; i <= WarriorInventory.SLOT_BACKPACK_END; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && (stack.is(ItemTags.ARROWS) || stack.getItem() instanceof ArrowItem)) {
                return i;
            }
        }
        return -1;
    }

    /** Ticks puxando antes de atirar (arco 20, besta 25; talento Atirador: -25%). */
    public static int getDrawTicks(WarriorCompanionEntity warrior) {
        int base = warrior.getMainHandItem().getItem() instanceof CrossbowItem ? 25 : 20;
        if (warrior.hasPerk(WarriorPerk.MARKSMAN)) {
            base = Math.round(base * 0.75F);
        }
        if (warrior.getSpecialization() == WarriorCompanionEntity.SPEC_ARCHER) {
            base = Math.round(base * 0.80F);
        }
        return Math.max(6, base);
    }

    /**
     * Troca de arma automática (a cada segundo): em combate, com arco/besta e sem flechas, saca a melhor arma
     * corpo a corpo da mochila; fora de combate, com flechas de novo, o Arqueiro volta para o arco.
     */
    public static void manageWeaponSwap(WarriorCompanionEntity warrior) {
        if (!EpicVanguardConfig.rangedCombat() || warrior.isUsingItem()) return;
        WarriorInventory inv = warrior.getWarriorInventory();
        ItemStack main = inv.getItem(WarriorInventory.SLOT_WEAPON_MAIN);
        boolean fighting = warrior.getTarget() != null && warrior.getTarget().isAlive();

        if (fighting && isRangedWeapon(main) && !hasAmmo(warrior, main)) {
            int melee = findBackpackWeapon(warrior, false);
            if (melee != -1) swapWithMain(inv, melee);
        } else if (!fighting && isArcher(warrior) && !isRangedWeapon(main)) {
            int ranged = findBackpackWeapon(warrior, true);
            if (ranged != -1 && hasAmmo(warrior, inv.getItem(ranged))) swapWithMain(inv, ranged);
        } else {
            return;
        }
        warrior.syncEquipmentWithInventory();
    }

    private static int findBackpackWeapon(WarriorCompanionEntity warrior, boolean ranged) {
        WarriorInventory inv = warrior.getWarriorInventory();
        int best = -1;
        double bestDamage = -1.0D;
        for (int i = WarriorInventory.SLOT_BACKPACK_START; i <= WarriorInventory.SLOT_BACKPACK_END; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !WarriorInventory.isWeaponOrShield(s)) continue;
            if (s.canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)) continue;
            if (isRangedWeapon(s) != ranged) continue;
            double dmg = 0.0D;
            for (var mod : s.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                    .get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
                dmg += mod.getAmount();
            }
            if (dmg > bestDamage) {
                bestDamage = dmg;
                best = i;
            }
        }
        return best;
    }

    private static void swapWithMain(WarriorInventory inv, int backpackSlot) {
        ItemStack fromBackpack = inv.getItem(backpackSlot);
        ItemStack main = inv.getItem(WarriorInventory.SLOT_WEAPON_MAIN);
        inv.setItem(WarriorInventory.SLOT_WEAPON_MAIN, fromBackpack);
        inv.setItem(backpackSlot, main);
        inv.setChanged();
    }

    public static boolean isArcher(WarriorCompanionEntity warrior) {
        return warrior.getSpecialization() == WarriorCompanionEntity.SPEC_ARCHER;
    }

    public static void shootAt(WarriorCompanionEntity warrior, LivingEntity target, float power) {
        shootAt(warrior, target, power, true);
        // Arqueiro com Tiro Duplo: 25% de chance de uma segunda flecha (não gasta munição)
        if (isArcher(warrior) && warrior.hasPerk(WarriorPerk.SPEC_T2) && warrior.getRandom().nextFloat() < 0.25F) {
            shootAt(warrior, target, power, false);
        }
    }

    private static void shootAt(WarriorCompanionEntity warrior, LivingEntity target, float power, boolean primary) {
        ItemStack weapon = warrior.getMainHandItem();
        if (!isRangedWeapon(weapon)) return;
        boolean crossbow = weapon.getItem() instanceof CrossbowItem;

        // Munição: flecha da mochila (com efeito, espectral...) ou flecha comum se for infinita
        int arrowSlot = primary ? findArrowSlot(warrior) : -1;
        boolean infinite = hasInfiniteAmmo(weapon) || !primary;
        ItemStack ammo = arrowSlot != -1 ? warrior.getWarriorInventory().getItem(arrowSlot) : ItemStack.EMPTY;
        if (ammo.isEmpty()) {
            if (!infinite) return;
            ammo = new ItemStack(Items.ARROW);
        }

        AbstractArrow arrow = ProjectileUtil.getMobArrow(warrior, ammo, power);
        if (weapon.getItem() instanceof BowItem bow) {
            arrow = bow.customArrow(arrow);
        }

        double dx = target.getX() - warrior.getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - warrior.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float velocity = crossbow ? 3.15F : 1.6F + power * 0.9F;
        float inaccuracy = (float) (14 - warrior.level().getDifficulty().getId() * 4) * 0.5F;
        arrow.shoot(dx, dy + horizontal * (crossbow ? 0.12D : 0.2D), dz, velocity, Math.max(1.0F, inaccuracy));

        // Dano: base vanilla da flecha * config * talento, + Força (Power) do arco
        double damage = arrow.getBaseDamage() * EpicVanguardConfig.rangedDamageMultiplier();
        if (crossbow) damage *= 1.25D;
        if (warrior.hasPerk(WarriorPerk.MARKSMAN)) damage *= 1.25D;
        if (isArcher(warrior)) damage *= 1.20D;
        int powerLvl = weapon.getEnchantmentLevel(Enchantments.POWER_ARROWS);
        if (powerLvl > 0) damage += powerLvl * 0.5D + 0.5D;
        arrow.setBaseDamage(damage);
        int punch = weapon.getEnchantmentLevel(Enchantments.PUNCH_ARROWS);
        if (punch > 0) arrow.setKnockback(punch);
        if (weapon.getEnchantmentLevel(Enchantments.FLAMING_ARROWS) > 0) arrow.setSecondsOnFire(100);
        int pierce = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, weapon);
        if (isArcher(warrior) && warrior.hasPerk(WarriorPerk.SPEC_T1)) pierce++; // Flecha Perfurante
        if (pierce > 0) arrow.setPierceLevel((byte) pierce);

        // Flecha gasta pode ser recolhida; a "infinita" não
        if (arrowSlot != -1 && !infinite) {
            ammo.shrink(1);
            warrior.getWarriorInventory().setChanged();
            arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        } else {
            arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        }

        warrior.level().playSound(null, warrior.getX(), warrior.getY(), warrior.getZ(),
                crossbow ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL,
                1.0F, 1.0F / (warrior.getRandom().nextFloat() * 0.4F + 0.8F));
        warrior.level().addFreshEntity(arrow);

        if (primary) {
            WarriorEquipmentWear.damageMainWeapon(warrior, 1);
        }
    }
}
