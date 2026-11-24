package io.github.raffasanttos.epicvanguard.networking.packet;

import io.github.raffasanttos.epicvanguard.entity.util.CompanyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Cliente -> servidor: pedidos da tela Minha Companhia. O servidor confere tudo (dono, modo válido)
 * e responde com a lista atualizada.
 */
public class PacketCompanyAction {
    public static final int OPEN = 0;       // pede a lista e abre a tela
    public static final int CALL = 1;       // traz um companheiro (como /companions bring)
    public static final int SET_MODE = 2;   // value = 0 Seguir, 1 Guarda, 2 Parado
    public static final int SET_FORMATION = 3; // value = WarriorFormation
    public static final int CALL_ALL = 4;   // traz todos (como /companions call)

    private final int action;
    @Nullable
    private final UUID companion;
    private final int value;

    public PacketCompanyAction(int action, @Nullable UUID companion, int value) {
        this.action = action;
        this.companion = companion;
        this.value = value;
    }

    public PacketCompanyAction(FriendlyByteBuf buf) {
        this.action = buf.readByte();
        this.companion = buf.readBoolean() ? buf.readUUID() : null;
        this.value = buf.readByte();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeByte(action);
        buf.writeBoolean(companion != null);
        if (companion != null) buf.writeUUID(companion);
        buf.writeByte(value);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            CompanyManager.handleAction(player, action, companion, value);
        });
        return true;
    }
}
