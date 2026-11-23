package io.github.raffasanttos.epicvanguard.entity.util;

import io.github.raffasanttos.epicvanguard.config.EpicVanguardConfig;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.entity.WarriorPerk;
import io.github.raffasanttos.epicvanguard.inventory.WarriorInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ToolActions;

/**
 * Desgaste do equipamento dos guerreiros (config {@code equipment.equipmentWear}).
 *
 * <p>Os slots de equipamento da entidade apontam para os mesmos ItemStacks do {@link WarriorInventory};
 * por isso o desgaste é aplicado direto no inventário e a quebra (stack vazio) reflete nos dois.</p>
 *
 * <p>Também cuida do aviso de durabilidade baixa, da troca automática do item quebrado por outro da
 * mochila e do reparo em descanso (sentado na fogueira ou na taverna) com materiais da mochila.</p>
 */
public final class WarriorEquipmentWear {

    private WarriorEquipmentWear() {}

    /** Marca no NBT do item: o dono já foi avisado da durabilidade baixa (removida no reparo). */
    public static final String LOW_WARNED_TAG = "EpicVanguardLowWarned";

    private static final int[] EQUIP_SLOTS = {
            WarriorInventory.SLOT_HELMET, WarriorInventory.SLOT_CHEST, WarriorInventory.SLOT_LEGS,
            WarriorInventory.SLOT_BOOTS, WarriorInventory.SLOT_WEAPON_MAIN, WarriorInventory.SLOT_WEAPON_OFF};

    /** Quanto de durabilidade realmente sai, com multiplicador da config e o talento Cuidado com o Equipamento. */
    private static int scaledAmount(WarriorCompanionEntity warrior, float base) {
        if (!EpicVanguardConfig.equipmentWear() || warrior.level().isClientSide) return 0;
        double amount = base * EpicVanguardConfig.wearMultiplier();
        if (warrior.hasPerk(WarriorPerk.CAREFUL_GEAR)) amount *= 0.5D;
        int whole = (int) amount;
        // Parte fracionária vira chance (ex.: 0,5 = 50% de gastar 1)
        if (warrior.getRandom().nextDouble() < amount - whole) whole++;
        return whole;
    }

    private static void damage(WarriorCompanionEntity warrior, int slot, EquipmentSlot equipSlot, int amount) {
        if (amount <= 0) return;
        WarriorInventory inv = warrior.getWarriorInventory();
        ItemStack stack = inv.getItem(slot);
        if (stack.isEmpty() || !stack.isDamageableItem()) return;
        ItemStack before = stack.copy();
        stack.hurtAndBreak(amount, warrior, w -> w.broadcastBreakEvent(equipSlot));
        if (stack.isEmpty()) {
            inv.setItem(slot, ItemStack.EMPTY);
            warnOwner(warrior, before);
            if (EpicVanguardConfig.autoSwapBroken()) {
                equipReplacement(warrior, slot, before);
            }
            warrior.syncEquipmentWithInventory();
        } else {
            checkLowDurability(warrior, stack);
            inv.setChanged();
        }
    }

    /** Fração de durabilidade que ainda resta (1 = novo). */
    public static float remainingFraction(ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) return 1.0F;
        return 1.0F - (float) stack.getDamageValue() / stack.getMaxDamage();
    }

    /** Avisa o dono uma única vez quando o item passa do limite de durabilidade baixa. */
    public static void checkLowDurability(WarriorCompanionEntity warrior, ItemStack stack) {
        double threshold = EpicVanguardConfig.lowDurabilityWarning();
        if (threshold <= 0.0D || stack.isEmpty() || !stack.isDamageableItem()) return;
        if (remainingFraction(stack) > threshold) return;
        if (stack.hasTag() && stack.getTag().getBoolean(LOW_WARNED_TAG)) return;
        stack.getOrCreateTag().putBoolean(LOW_WARNED_TAG, true);
        Player owner = warrior.getOwner();
        if (owner != null) {
            int pct = Math.max(1, Math.round(remainingFraction(stack) * 100.0F));
            owner.displayClientMessage(Component.translatable("epicvanguard.text.wear.low",
                    warrior.getWarriorName(), stack.getHoverName(), pct), false);
        }
    }

    // ── Troca automática ───────────────────────────────────────────────────────

    /** Coloca no slot vazio o melhor item compatível da mochila. Devolve true se trocou. */
    public static boolean equipReplacement(WarriorCompanionEntity warrior, int slot, ItemStack broken) {
        WarriorInventory inv = warrior.getWarriorInventory();
        if (!inv.getItem(slot).isEmpty()) return false;
        int best = -1;
        double bestScore = -1.0D;
        for (int i = WarriorInventory.SLOT_BACKPACK_START; i <= WarriorInventory.SLOT_BACKPACK_END; i++) {
            ItemStack candidate = inv.getItem(i);
            if (candidate.isEmpty() || !inv.canPlaceItem(slot, candidate)) continue;
            double score = replacementScore(slot, candidate, broken);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        if (best == -1) return false;
        ItemStack chosen = inv.getItem(best);
        inv.setItem(slot, chosen.split(1));
        if (chosen.isEmpty()) inv.setItem(best, ItemStack.EMPTY);
        inv.setChanged();
        Player owner = warrior.getOwner();
        if (owner != null && EpicVanguardConfig.warnOnBreak()) {
            owner.displayClientMessage(Component.translatable("epicvanguard.text.wear.swapped",
                    warrior.getWarriorName(), inv.getItem(slot).getHoverName()), false);
        }
        return true;
    }

    /** Pontuação do candidato: armadura pela defesa; arma pelo dano (arco só substitui arco/besta e vice-versa); escudo é escudo. */
    private static double replacementScore(int slot, ItemStack candidate, ItemStack broken) {
        if (slot <= WarriorInventory.SLOT_BOOTS) {
            return candidate.getItem() instanceof ArmorItem armor ? armor.getDefense() + armor.getToughness() * 0.5D : 0.0D;
        }
        boolean candidateShield = candidate.canPerformAction(ToolActions.SHIELD_BLOCK);
        if (slot == WarriorInventory.SLOT_WEAPON_OFF) {
            return candidateShield ? 1.0D + remainingFraction(candidate) : -1.0D;
        }
        if (candidateShield) return -1.0D;
        boolean brokenRanged = WarriorRangedHelper.isRangedWeapon(broken);
        if (WarriorRangedHelper.isRangedWeapon(candidate) != brokenRanged) return -1.0D;
        return attackDamageOf(candidate) + remainingFraction(candidate);
    }

    private static double attackDamageOf(ItemStack stack) {
        double total = 0.0D;
        for (var mod : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            total += mod.getAmount();
        }
        return total;
    }

    // ── Reparo em descanso ─────────────────────────────────────────────────────

    /**
     * Repara um item equipado usando um material válido da mochila (o mesmo da bigorna: lingote, couro, tábuas...).
     * Cada material devolve 25% da durabilidade máxima, como na bigorna, sem custo de XP.
     * @return true se reparou algo
     */
    public static boolean tryFieldRepair(WarriorCompanionEntity warrior) {
        if (!EpicVanguardConfig.fieldRepair() || warrior.level().isClientSide) return false;
        WarriorInventory inv = warrior.getWarriorInventory();
        // Repara primeiro o item mais gasto
        int worstSlot = -1;
        float worst = 1.0F;
        for (int slot : EQUIP_SLOTS) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || !stack.isDamageableItem() || !stack.isDamaged()) continue;
            if (findRepairMaterial(inv, stack) == -1) continue;
            float frac = remainingFraction(stack);
            if (frac < worst) {
                worst = frac;
                worstSlot = slot;
            }
        }
        if (worstSlot == -1) return false;

        ItemStack stack = inv.getItem(worstSlot);
        int materialSlot = findRepairMaterial(inv, stack);
        ItemStack material = inv.getItem(materialSlot);
        int repair = Math.max(1, stack.getMaxDamage() / 4);
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
        material.shrink(1);
        if (material.isEmpty()) inv.setItem(materialSlot, ItemStack.EMPTY);
        if (stack.hasTag() && remainingFraction(stack) > EpicVanguardConfig.lowDurabilityWarning()) {
            stack.getTag().remove(LOW_WARNED_TAG);
            if (stack.getTag().isEmpty()) stack.setTag(null);
        }
        inv.setChanged();
        warrior.level().playSound(null, warrior.getX(), warrior.getY(), warrior.getZ(),
                SoundEvents.SMITHING_TABLE_USE, SoundSource.NEUTRAL, 0.5F, 1.1F);
        return true;
    }

    /** Slot da mochila com material que repara o item, ou -1. */
    public static int findRepairMaterial(WarriorInventory inv, ItemStack toRepair) {
        for (int i = WarriorInventory.SLOT_BACKPACK_START; i <= WarriorInventory.SLOT_BACKPACK_END; i++) {
            ItemStack m = inv.getItem(i);
            if (!m.isEmpty() && toRepair.getItem().isValidRepairItem(toRepair, m)) return i;
        }
        return -1;
    }

    private static void warnOwner(WarriorCompanionEntity warrior, ItemStack broken) {
        if (!EpicVanguardConfig.warnOnBreak()) return;
        Player owner = warrior.getOwner();
        if (owner != null) {
            owner.displayClientMessage(Component.translatable("epicvanguard.text.wear.broken",
                    warrior.getWarriorName(), broken.getHoverName()), false);
        }
    }

    /** Armadura: mesma regra do jogador (1 de durabilidade a cada 4 de dano, mínimo 1). */
    public static void damageArmor(WarriorCompanionEntity warrior, float damageTaken) {
        damageArmor(warrior, null, damageTaken);
    }

    /** Com a fonte: dano de fogo não gasta peças à prova de fogo (netherite). */
    public static void damageArmor(WarriorCompanionEntity warrior, @org.jetbrains.annotations.Nullable net.minecraft.world.damagesource.DamageSource source, float damageTaken) {
        boolean fire = source != null && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE);
        float base = Math.max(1.0F, damageTaken / 4.0F);
        int[] slots = {WarriorInventory.SLOT_HELMET, WarriorInventory.SLOT_CHEST, WarriorInventory.SLOT_LEGS, WarriorInventory.SLOT_BOOTS};
        EquipmentSlot[] equip = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = warrior.getWarriorInventory().getItem(slots[i]);
            if (stack.getItem() instanceof ArmorItem && !(fire && stack.getItem().isFireResistant())) {
                damage(warrior, slots[i], equip[i], scaledAmount(warrior, base));
            }
        }
    }

    /** Só o elmo (ex.: bigorna ou estalactite caindo). */
    public static void damageHelmet(WarriorCompanionEntity warrior, float damageTaken) {
        damage(warrior, WarriorInventory.SLOT_HELMET, EquipmentSlot.HEAD,
                scaledAmount(warrior, Math.max(1.0F, damageTaken / 4.0F)));
    }

    /** Arma principal: 1 por golpe (ou por flecha). */
    public static void damageMainWeapon(WarriorCompanionEntity warrior, int amount) {
        damage(warrior, WarriorInventory.SLOT_WEAPON_MAIN, EquipmentSlot.MAINHAND, scaledAmount(warrior, amount));
    }

    /** Escudo ao aparar: 1 + dano aparado (regra do jogador, a partir de 3 de dano). */
    public static void damageShield(WarriorCompanionEntity warrior, float damageBlocked) {
        if (damageBlocked < 3.0F) return;
        float base = 1.0F + (float) Math.floor(damageBlocked);
        ItemStack off = warrior.getWarriorInventory().getItem(WarriorInventory.SLOT_WEAPON_OFF);
        if (off.canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)) {
            damage(warrior, WarriorInventory.SLOT_WEAPON_OFF, EquipmentSlot.OFFHAND, scaledAmount(warrior, base));
        } else {
            ItemStack main = warrior.getWarriorInventory().getItem(WarriorInventory.SLOT_WEAPON_MAIN);
            if (main.canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)) {
                damage(warrior, WarriorInventory.SLOT_WEAPON_MAIN, EquipmentSlot.MAINHAND, scaledAmount(warrior, base));
            }
        }
    }
}
