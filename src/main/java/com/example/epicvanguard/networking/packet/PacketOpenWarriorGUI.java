package com.example.epicvanguard.networking.packet;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.screen.HonorContractMenu;
import com.example.epicvanguard.screen.WarriorCompanionMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class PacketOpenWarriorGUI {
    private final int entityId;
    private final boolean recruited;

    public PacketOpenWarriorGUI(int entityId, boolean recruited) {
        this.entityId  = entityId;
        this.recruited = recruited;
    }

    public PacketOpenWarriorGUI(FriendlyByteBuf buf) {
        this.entityId  = buf.readInt();
        this.recruited = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(recruited);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            Entity entity = player.serverLevel().getEntity(entityId);
            if (!(entity instanceof WarriorCompanionEntity warrior)) return;

            if (recruited) {
                NetworkHooks.openScreen(player, new SimpleMenuProvider(
                        (id, inv, p) -> new WarriorCompanionMenu(id, inv, entityId),
                        Component.literal(warrior.getWarriorName() + " - Inventário")
                ), buf -> buf.writeInt(entityId));
            } else {
                NetworkHooks.openScreen(player, new SimpleMenuProvider(
                        (id, inv, p) -> new HonorContractMenu(id, inv, entityId),
                        Component.literal("Contrato de Honra: " + warrior.getWarriorName())
                ), buf -> buf.writeInt(entityId));
            }
        });
        return true;
    }
}
