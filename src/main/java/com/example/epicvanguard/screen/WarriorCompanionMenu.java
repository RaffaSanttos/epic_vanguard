package com.example.epicvanguard.screen;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModMenus;
import com.example.epicvanguard.inventory.WarriorInventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public class WarriorCompanionMenu extends AbstractContainerMenu {
    private final int entityId;
    private final WarriorInventory warriorInv;

    public WarriorCompanionMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, extraData.readInt());
    }

    public WarriorCompanionMenu(int containerId, Inventory playerInv, int entityId) {
        super(ModMenus.WARRIOR_COMPANION_MENU.get(), containerId);
        this.entityId = entityId;

        Player player = playerInv.player;
        Entity entity = player.level().getEntity(entityId);
        if (entity instanceof WarriorCompanionEntity warrior) {
            this.warriorInv = warrior.getWarriorInventory();
        } else {
            this.warriorInv = new WarriorInventory();
        }

        // ── Equipment Slots (Left Panel) ───────────────────────────────────────
        // Slot 0 – Helmet (x=13, y=21)
        this.addSlot(new Slot(this.warriorInv, WarriorInventory.SLOT_HELMET, 13, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return WarriorInventory.isArmorForSlot(stack, EquipmentSlot.HEAD);
            }
        });
        // Slot 1 – Chestplate (x=13, y=39)
        this.addSlot(new Slot(this.warriorInv, WarriorInventory.SLOT_CHEST, 13, 39) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return WarriorInventory.isArmorForSlot(stack, EquipmentSlot.CHEST);
            }
        });
        // Slot 2 – Leggings (x=13, y=57)
        this.addSlot(new Slot(this.warriorInv, WarriorInventory.SLOT_LEGS, 13, 57) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return WarriorInventory.isArmorForSlot(stack, EquipmentSlot.LEGS);
            }
        });
        // Slot 3 – Boots (x=13, y=75)
        this.addSlot(new Slot(this.warriorInv, WarriorInventory.SLOT_BOOTS, 13, 75) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return WarriorInventory.isArmorForSlot(stack, EquipmentSlot.FEET);
            }
        });
        // Slot 4 – Main Weapon (x=35, y=21)
        this.addSlot(new Slot(this.warriorInv, WarriorInventory.SLOT_WEAPON_MAIN, 35, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return WarriorInventory.isWeaponOrShield(stack);
            }
        });
        // Slot 5 – Off Weapon / Shield (x=35, y=39)
        this.addSlot(new Slot(this.warriorInv, WarriorInventory.SLOT_WEAPON_OFF, 35, 39) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return WarriorInventory.isWeaponOrShield(stack);
            }
        });

        // ── Backpack slots 6-20 (5 cols x 3 rows, starting x=83, y=21) ────────
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                int slotIndex = WarriorInventory.SLOT_BACKPACK_START + row * 5 + col;
                this.addSlot(new Slot(this.warriorInv, slotIndex, 83 + col * 18, 21 + row * 18));
            }
        }

        // ── Player inventory (9 cols x 3 rows, x=17, y=105) ───────────────────
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 17 + col * 18, 105 + row * 18));
            }
        }

        // ── Player hotbar (9 cols x 1 row, x=17, y=163) ───────────────────────
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 17 + col * 18, 163));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            int warriorSlots = 21; // indices 0 a 20
            int playerInvStart = 21;
            int playerInvEnd = this.slots.size(); // 57

            if (index < warriorSlots) {
                // Do Guerreiro -> Vai para o Inventário do Jogador
                if (!this.moveItemStackTo(stackInSlot, playerInvStart, playerInvEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Do Jogador -> Vai para o Guerreiro com roteamento inteligente
                boolean moved = false;

                // 1. Armaduras vão diretamente para seus respectivos slots de armadura (0-3)
                if (WarriorInventory.isArmorForSlot(stackInSlot, EquipmentSlot.HEAD)) {
                    moved = this.moveItemStackTo(stackInSlot, WarriorInventory.SLOT_HELMET, WarriorInventory.SLOT_HELMET + 1, false);
                } else if (WarriorInventory.isArmorForSlot(stackInSlot, EquipmentSlot.CHEST)) {
                    moved = this.moveItemStackTo(stackInSlot, WarriorInventory.SLOT_CHEST, WarriorInventory.SLOT_CHEST + 1, false);
                } else if (WarriorInventory.isArmorForSlot(stackInSlot, EquipmentSlot.LEGS)) {
                    moved = this.moveItemStackTo(stackInSlot, WarriorInventory.SLOT_LEGS, WarriorInventory.SLOT_LEGS + 1, false);
                } else if (WarriorInventory.isArmorForSlot(stackInSlot, EquipmentSlot.FEET)) {
                    moved = this.moveItemStackTo(stackInSlot, WarriorInventory.SLOT_BOOTS, WarriorInventory.SLOT_BOOTS + 1, false);
                }
                // 2. Armas / Escudos vão diretamente para os slots de mãos (4 e 5)
                else if (WarriorInventory.isWeaponOrShield(stackInSlot)) {
                    // Tenta colocar na Arma Principal (Slot 4)
                    moved = this.moveItemStackTo(stackInSlot, WarriorInventory.SLOT_WEAPON_MAIN, WarriorInventory.SLOT_WEAPON_MAIN + 1, false);
                    // Se não couber e ainda houver itens, tenta colocar na Arma Secundária / Escudo (Slot 5)
                    if (!stackInSlot.isEmpty() && !moved) {
                        moved = this.moveItemStackTo(stackInSlot, WarriorInventory.SLOT_WEAPON_OFF, WarriorInventory.SLOT_WEAPON_OFF + 1, false);
                    }
                }

                // 3. Se não couber nos slots de equipamento OU se for qualquer outro item comum (comida, blocos, etc.),
                // vai direto para a Mochila (slots 6 a 20)
                if (!stackInSlot.isEmpty() && !moved) {
                    if (!this.moveItemStackTo(stackInSlot, WarriorInventory.SLOT_BACKPACK_START, WarriorInventory.SLOT_BACKPACK_END + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        Entity entity = player.level().getEntity(entityId);
        return entity instanceof WarriorCompanionEntity warrior && warrior.isAlive() && warrior.distanceTo(player) < 8.0F;
    }

    public int getEntityId() {
        return entityId;
    }
}
