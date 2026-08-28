package com.example.epicvanguard.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.fml.ModList;

/**
 * Inventário do Guerreiro Companheiro (Epic Vanguard).
 *
 * Layout dos 21 slots:
 *   0  – Elmo          (ArmorItem HELMET)
 *   1  – Peitoral      (ArmorItem CHESTPLATE)
 *   2  – Calças        (ArmorItem LEGGINGS)
 *   3  – Botas         (ArmorItem BOOTS)
 *   4  – Arma Principal (Espadas, Machados, Arcos, etc.)
 *   5  – Arma Secundária / Escudo (Escudos, Armas secundárias)
 *   6–20 – Mochila livre (15 slots - aceita qualquer item comum)
 */
public class WarriorInventory extends SimpleContainer {

    public static final int SLOT_HELMET         = 0;
    public static final int SLOT_CHEST          = 1;
    public static final int SLOT_LEGS           = 2;
    public static final int SLOT_BOOTS          = 3;
    public static final int SLOT_WEAPON_MAIN    = 4;
    public static final int SLOT_WEAPON_OFF     = 5;
    public static final int SLOT_BACKPACK_START = 6;
    public static final int SLOT_BACKPACK_END   = 20;

    private static final int SIZE = 21;

    public WarriorInventory() {
        super(SIZE);
    }

    public static boolean isArmorForSlot(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ArmorItem armor) {
            return armor.getEquipmentSlot() == slot;
        }
        return LivingEntity.getEquipmentSlotForItem(stack) == slot;
    }

    public static boolean isWeaponOrShield(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();

        if (item instanceof SwordItem ||
            item instanceof AxeItem ||
            item instanceof ShieldItem ||
            item instanceof BowItem ||
            item instanceof CrossbowItem ||
            item instanceof TridentItem) {
            return true;
        }

        // Tags de armas do Minecraft
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) {
            return true;
        }

        // Compatibilidade com armas do Epic Fight
        if (ModList.get().isLoaded("epicfight")) {
            try {
                if (item instanceof yesman.epicfight.world.item.WeaponItem) {
                    return true;
                }
                var cap = yesman.epicfight.world.capabilities.EpicFightCapabilities.getItemStackCapability(stack);
                if (cap != null && !cap.isEmpty() && cap.getWeaponCategory() != null) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        // Itens que possuem dano de ataque (ex: armas de outros mods)
        var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        if (modifiers.containsKey(Attributes.ATTACK_DAMAGE)) {
            for (var mod : modifiers.get(Attributes.ATTACK_DAMAGE)) {
                if (mod.getAmount() > 1.0D) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) return true;
        return switch (slot) {
            case SLOT_HELMET -> isArmorForSlot(stack, EquipmentSlot.HEAD);
            case SLOT_CHEST  -> isArmorForSlot(stack, EquipmentSlot.CHEST);
            case SLOT_LEGS   -> isArmorForSlot(stack, EquipmentSlot.LEGS);
            case SLOT_BOOTS  -> isArmorForSlot(stack, EquipmentSlot.FEET);
            case SLOT_WEAPON_MAIN, SLOT_WEAPON_OFF -> isWeaponOrShield(stack);
            default          -> true; // slots 6-20 (mochila) aceitam qualquer item
        };
    }

    public void saveToNBT(CompoundTag tag) {
        ListTag listTag = new ListTag();
        for (int i = 0; i < SIZE; i++) {
            ItemStack stack = this.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putByte("Slot", (byte) i);
                stack.save(slotTag);
                listTag.add(slotTag);
            }
        }
        tag.put("Items", listTag);
    }

    public void loadFromNBT(CompoundTag tag) {
        for (int i = 0; i < SIZE; i++) {
            this.setItem(i, ItemStack.EMPTY);
        }

        ListTag listTag = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag slotTag = listTag.getCompound(i);
            int slot = slotTag.getByte("Slot") & 0xFF;
            if (slot < SIZE) {
                this.setItem(slot, ItemStack.of(slotTag));
            }
        }
    }
}
