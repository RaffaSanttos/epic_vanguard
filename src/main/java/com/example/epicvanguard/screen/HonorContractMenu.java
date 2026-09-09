package com.example.epicvanguard.screen;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class HonorContractMenu extends AbstractContainerMenu {
    private final int entityId;

    public HonorContractMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, extraData.readInt());
    }

    public HonorContractMenu(int containerId, Inventory playerInv, int entityId) {
        super(ModMenus.HONOR_CONTRACT_MENU.get(), containerId);
        this.entityId = entityId;

        Player player = playerInv.player;
        if (!player.level().isClientSide) {
            Entity entity = player.level().getEntity(entityId);
            if (entity instanceof WarriorCompanionEntity warrior) {
                warrior.setInventoryOpenPlayer(player);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        Entity entity = player.level().getEntity(entityId);
        if (entity instanceof WarriorCompanionEntity warrior && warrior.isAlive() && warrior.distanceTo(player) < 8.0F) {
            if (!player.level().isClientSide) {
                warrior.setInventoryOpenPlayer(player);
            }
            return true;
        }
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            Entity entity = player.level().getEntity(entityId);
            if (entity instanceof WarriorCompanionEntity warrior) {
                warrior.setInventoryOpenPlayer(null);
            }
        }
    }

    public int getEntityId() {
        return entityId;
    }
}
