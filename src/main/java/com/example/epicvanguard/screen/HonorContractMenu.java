package com.example.epicvanguard.screen;

import com.example.epicvanguard.init.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
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
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public int getEntityId() {
        return entityId;
    }
}
