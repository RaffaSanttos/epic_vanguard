package io.github.raffasanttos.epicvanguard.networking.packet;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.networking.PacketGuards;
import io.github.raffasanttos.epicvanguard.screen.HonorContractMenu;
import io.github.raffasanttos.epicvanguard.screen.WarriorCompanionMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
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
            WarriorCompanionEntity warrior = PacketGuards.nearbyWarrior(player, entityId);
            if (warrior == null) return;

            // Usa o estado real do servidor, não o flag enviado pelo cliente.
            // Inventário de guerreiro recrutado: apenas o dono pode abrir.
            if (warrior.isRecruited()) {
                if (!warrior.isOwner(player)) return;
                NetworkHooks.openScreen(player, new SimpleMenuProvider(
                        (id, inv, p) -> new WarriorCompanionMenu(id, inv, entityId),
                        Component.translatable("epicvanguard.text.packet_open_warrior_g_u_i.1", warrior.getWarriorName())
                ), buf -> buf.writeInt(entityId));
            } else {
                NetworkHooks.openScreen(player, new SimpleMenuProvider(
                        (id, inv, p) -> new HonorContractMenu(id, inv, entityId),
                        Component.translatable("epicvanguard.text.packet_open_warrior_g_u_i.2", warrior.getWarriorName())
                ), buf -> buf.writeInt(entityId));
            }
        });
        return true;
    }
}
