package io.github.raffasanttos.epicvanguard.networking.packet;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.entity.WarriorPerk;
import io.github.raffasanttos.epicvanguard.networking.PacketGuards;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente pede para desbloquear um talento da árvore de habilidades. O servidor revalida tudo. */
public class PacketUnlockPerk {
    private final int entityId;
    private final int perkOrdinal;

    public PacketUnlockPerk(int entityId, WarriorPerk perk) {
        this.entityId = entityId;
        this.perkOrdinal = perk.ordinal();
    }

    public PacketUnlockPerk(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.perkOrdinal = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVarInt(perkOrdinal);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            WarriorCompanionEntity warrior = PacketGuards.ownedWarrior(player, entityId);
            WarriorPerk perk = WarriorPerk.byOrdinal(perkOrdinal);
            if (warrior == null || perk == null) return;

            Component reason = warrior.getPerkLockReason(perk);
            if (reason != null) {
                player.displayClientMessage(reason.copy().withStyle(ChatFormatting.RED), true);
                return;
            }
            if (warrior.unlockPerk(perk)) {
                player.serverLevel().playSound(null, warrior.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.3F);
                player.displayClientMessage(Component.translatable("epicvanguard.perk.unlocked",
                        warrior.getWarriorName(), perk.getName(warrior.getSpecialization())).withStyle(ChatFormatting.GOLD), true);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
