package io.github.raffasanttos.epicvanguard.entity.util;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.init.ModItems;
import io.github.raffasanttos.epicvanguard.inventory.WarriorInventory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Auto-equipar (melhor armadura e arma da mochila) e o filtro "itens úteis" da coleta.
 *
 * <p>Regras do auto-equipar:
 * <ul>
 *   <li>só troca por algo <b>melhor</b> (armadura + resistência + Proteção; dano + Afiação);</li>
 *   <li>arma corpo a corpo só substitui arma corpo a corpo (nunca troca o arco do guerreiro por espada);</li>
 *   <li>mão secundária vazia recebe escudo (ou estandarte, no Porta-estandarte);</li>
 *   <li>ignora peças quase quebradas (menos de 10% de durabilidade);</li>
 *   <li>a peça antiga vai para o lugar da nova na mochila (nada se perde);</li>
 *   <li>não mexe enquanto o dono está com a tela aberta, nem em luta.</li>
 * </ul>
 */
public final class WarriorEquipment {

    private WarriorEquipment() {}

    private static final int CHECK_INTERVAL = 40;

    private static final int[] ARMOR_SLOTS = {
            WarriorInventory.SLOT_HELMET, WarriorInventory.SLOT_CHEST, WarriorInventory.SLOT_LEGS, WarriorInventory.SLOT_BOOTS };
    private static final EquipmentSlot[] ARMOR_EQUIP = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    /** Chamado todo tick no servidor; trabalha a cada 2 s, espalhado pelo id. */
    public static void tickAutoEquip(WarriorCompanionEntity w) {
        if (w.level().isClientSide() || !w.isAlive() || !w.isRecruited() || w.isPrisoner()) return;
        if ((w.tickCount + w.getId()) % CHECK_INTERVAL != 0) return;
        if (w.isInventoryOpen() || w.isFighting() || w.isUsingItem()) return;

        WarriorInventory inv = w.getWarriorInventory();
        boolean changed = false;

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            changed |= upgradeSlot(inv, ARMOR_SLOTS[i], ARMOR_EQUIP[i]);
        }
        changed |= upgradeMainWeapon(inv);
        changed |= equipOffhandIfEmpty(w, inv);

        if (changed) {
            w.syncEquipmentWithInventory();
        }
    }

    private static boolean upgradeSlot(WarriorInventory inv, int slot, EquipmentSlot equip) {
        ItemStack current = inv.getItem(slot);
        double best = current.isEmpty() ? -1.0D : armorScore(current, equip);
        int bestIndex = -1;
        for (int i = WarriorInventory.SLOT_BACKPACK_START; i <= WarriorInventory.SLOT_BACKPACK_END; i++) {
            ItemStack cand = inv.getItem(i);
            if (!WarriorInventory.isArmorForSlot(cand, equip) || nearlyBroken(cand)) continue;
            double score = armorScore(cand, equip);
            if (score > best + 0.01D) {
                best = score;
                bestIndex = i;
            }
        }
        return bestIndex >= 0 && swap(inv, slot, bestIndex);
    }

    private static boolean upgradeMainWeapon(WarriorInventory inv) {
        ItemStack current = inv.getItem(WarriorInventory.SLOT_WEAPON_MAIN);
        if (!current.isEmpty() && !isMelee(current)) return false; // arco/besta/tridente: escolha do dono
        double best = current.isEmpty() ? -1.0D : weaponScore(current);
        int bestIndex = -1;
        for (int i = WarriorInventory.SLOT_BACKPACK_START; i <= WarriorInventory.SLOT_BACKPACK_END; i++) {
            ItemStack cand = inv.getItem(i);
            if (!WarriorInventory.isWeaponOrShield(cand) || !isMelee(cand) || nearlyBroken(cand)) continue;
            double score = weaponScore(cand);
            if (score > best + 0.01D) {
                best = score;
                bestIndex = i;
            }
        }
        return bestIndex >= 0 && swap(inv, WarriorInventory.SLOT_WEAPON_MAIN, bestIndex);
    }

    /** Porta-estandarte usa estandarte na mão secundária (aura); os outros usam escudo. */
    private static boolean isStandardBearer(WarriorCompanionEntity w) {
        return w.getSpecialization() == WarriorCompanionEntity.SPEC_STANDARD_BEARER;
    }

    private static boolean isOffhandItemFor(WarriorCompanionEntity w, ItemStack stack) {
        return isStandardBearer(w) ? stack.getItem() instanceof BannerItem : stack.getItem() instanceof ShieldItem;
    }

    private static boolean equipOffhandIfEmpty(WarriorCompanionEntity w, WarriorInventory inv) {
        if (!inv.getItem(WarriorInventory.SLOT_WEAPON_OFF).isEmpty()) return false;
        for (int i = WarriorInventory.SLOT_BACKPACK_START; i <= WarriorInventory.SLOT_BACKPACK_END; i++) {
            ItemStack cand = inv.getItem(i);
            if (isOffhandItemFor(w, cand) && !nearlyBroken(cand)) {
                return swap(inv, WarriorInventory.SLOT_WEAPON_OFF, i);
            }
        }
        return false;
    }

    private static boolean swap(WarriorInventory inv, int equipSlot, int backpackSlot) {
        ItemStack equipped = inv.getItem(equipSlot);
        ItemStack fromBackpack = inv.getItem(backpackSlot);
        if (fromBackpack.getCount() > 1) {
            // Pilha (raro em equipamento): equipa um e devolve o antigo para a mochila
            ItemStack one = fromBackpack.split(1);
            inv.setItem(equipSlot, one);
            if (!equipped.isEmpty()) {
                ItemStack rest = inv.addItemToBackpack(equipped);
                if (!rest.isEmpty()) { // sem espaço: desfaz
                    fromBackpack.grow(1);
                    inv.setItem(equipSlot, equipped);
                    return false;
                }
            }
        } else {
            inv.setItem(equipSlot, fromBackpack);
            inv.setItem(backpackSlot, equipped);
        }
        inv.setChanged();
        return true;
    }

    private static boolean isMelee(ItemStack stack) {
        return !(stack.getItem() instanceof BowItem) && !(stack.getItem() instanceof CrossbowItem)
                && !(stack.getItem() instanceof TridentItem) && !(stack.getItem() instanceof ShieldItem);
    }

    private static boolean nearlyBroken(ItemStack stack) {
        return stack.isDamageableItem() && stack.getMaxDamage() > 0
                && (stack.getMaxDamage() - stack.getDamageValue()) < stack.getMaxDamage() * 0.10D;
    }

    private static double sumModifiers(ItemStack stack, EquipmentSlot slot, Attribute attribute) {
        double total = 0.0D;
        for (AttributeModifier mod : stack.getAttributeModifiers(slot).get(attribute)) {
            if (mod.getOperation() == AttributeModifier.Operation.ADDITION) {
                total += mod.getAmount();
            }
        }
        return total;
    }

    /** Armadura + resistência (vale metade) + Proteção (meio ponto por nível). */
    public static double armorScore(ItemStack stack, EquipmentSlot slot) {
        return sumModifiers(stack, slot, Attributes.ARMOR)
                + sumModifiers(stack, slot, Attributes.ARMOR_TOUGHNESS) * 0.5D
                + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, stack) * 0.5D;
    }

    /** Dano de ataque + Afiação (0,5 por nível + 0,5, como no vanilla). */
    public static double weaponScore(ItemStack stack) {
        int sharp = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, stack);
        return sumModifiers(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE)
                + (sharp > 0 ? 0.5D * sharp + 0.5D : 0.0D);
    }

    /**
     * Item útil para o modo "só úteis" da coleta: comida, poções de cura/regeneração, moedas de ouro,
     * flechas, e equipamento melhor que o que ele está usando.
     */
    public static boolean isUseful(WarriorCompanionEntity w, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.isEdible()) return true;
        if (stack.is(ModItems.GOLD_COIN.get())) return true;
        if (stack.is(net.minecraft.tags.ItemTags.ARROWS)) return true;
        if (stack.getItem() instanceof PotionItem) {
            for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
                if (effect.getEffect() == MobEffects.HEAL || effect.getEffect() == MobEffects.REGENERATION) return true;
            }
            return false;
        }
        WarriorInventory inv = w.getWarriorInventory();
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            if (WarriorInventory.isArmorForSlot(stack, ARMOR_EQUIP[i])) {
                ItemStack current = inv.getItem(ARMOR_SLOTS[i]);
                return current.isEmpty() || armorScore(stack, ARMOR_EQUIP[i]) > armorScore(current, ARMOR_EQUIP[i]) + 0.01D;
            }
        }
        if (stack.getItem() instanceof ShieldItem || stack.getItem() instanceof BannerItem) {
            return isOffhandItemFor(w, stack) && inv.getItem(WarriorInventory.SLOT_WEAPON_OFF).isEmpty();
        }
        if (WarriorInventory.isWeaponOrShield(stack) && isMelee(stack)) {
            ItemStack current = inv.getItem(WarriorInventory.SLOT_WEAPON_MAIN);
            return current.isEmpty() || (isMelee(current) && weaponScore(stack) > weaponScore(current) + 0.01D);
        }
        return false;
    }
}
