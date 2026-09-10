package com.example.epicvanguard.screen;

import com.example.epicvanguard.block.entity.VanguardPointBlockEntity;
import com.example.epicvanguard.init.ModBlocks;
import com.example.epicvanguard.init.ModItems;
import com.example.epicvanguard.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class VanguardPointMenu extends AbstractContainerMenu {
    private final VanguardPointBlockEntity blockEntity;
    private final ContainerData data;
    private final BlockPos pos;

    public VanguardPointMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, extraData.readBlockPos());
    }

    public VanguardPointMenu(int containerId, Inventory playerInv, BlockPos pos) {
        this(containerId, playerInv, getBlockEntityFromPos(playerInv, pos), new SimpleContainerData(6));
    }

    public VanguardPointMenu(int containerId, Inventory playerInv, VanguardPointBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.VANGUARD_POINT_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;
        this.pos = blockEntity.getBlockPos();

        checkContainerDataCount(data, 6);
        addDataSlots(data);

        IItemHandler handler = blockEntity.getItemHandler();

        // Slot 0: Depósito de Moedas de Ouro (Centralizado na HUD)
        this.addSlot(new SlotItemHandler(handler, 0, 87, 55) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return !isContractActive() && stack.is(ModItems.GOLD_COIN.get());
            }

            @Override
            public boolean mayPickup(Player playerIn) {
                return !isContractActive();
            }
        });

        // Slots 1-27: Inventário do Jogador (3 linhas de 9 colunas)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 15 + col * 18, 105 + row * 18));
            }
        }

        // Slots 28-36: Hotbar do Jogador (1 linha de 9 colunas)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 15 + col * 18, 167));
        }
    }

    private static VanguardPointBlockEntity getBlockEntityFromPos(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof VanguardPointBlockEntity vanguardEntity) {
            return vanguardEntity;
        }
        throw new IllegalStateException("BlockEntity na posição " + pos + " não é um VanguardPointBlockEntity!");
    }

    public boolean isContractActive() {
        return data.get(0) == 1;
    }

    public int getRemainingTicks() {
        int low = data.get(1) & 0xFFFF;
        int high = data.get(2) & 0xFFFF;
        return (high << 16) | low;
    }

    public int getTotalTicks() {
        int low = data.get(3) & 0xFFFF;
        int high = data.get(4) & 0xFFFF;
        int total = (high << 16) | low;
        return total > 0 ? total : VanguardPointBlockEntity.DEFAULT_CONTRACT_TICKS;
    }

    public int getStoredCoins() {
        return data.get(5);
    }

    public BlockPos getPos() {
        return pos;
    }

    public VanguardPointBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index == 0) {
                // Do slot do bloco para o inventário do jogador
                if (!this.moveItemStackTo(stackInSlot, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            } else {
                // Do jogador para o slot de moedas do bloco
                if (stackInSlot.is(ModItems.GOLD_COIN.get()) && !isContractActive()) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 1 && index < 28) {
                    // Da mochila para a hotbar
                    if (!this.moveItemStackTo(stackInSlot, 28, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 28 && index < 37) {
                    // Da hotbar para a mochila
                    if (!this.moveItemStackTo(stackInSlot, 1, 28, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), pos),
                player, ModBlocks.VANGUARD_POINT.get());
    }
}
