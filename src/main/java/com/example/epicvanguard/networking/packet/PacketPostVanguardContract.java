package com.example.epicvanguard.networking.packet;

import com.example.epicvanguard.block.entity.VanguardPointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketPostVanguardContract {
    private final BlockPos pos;
    private final boolean instantComplete;

    public PacketPostVanguardContract(BlockPos pos) {
        this(pos, false);
    }

    public PacketPostVanguardContract(BlockPos pos, boolean instantComplete) {
        this.pos = pos;
        this.instantComplete = instantComplete;
    }

    public PacketPostVanguardContract(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.instantComplete = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(instantComplete);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
                return;
            }

            BlockEntity be = player.serverLevel().getBlockEntity(pos);
            if (be instanceof VanguardPointBlockEntity vanguardEntity) {
                if (instantComplete && player.isCreative()) {
                    vanguardEntity.completeContractInstantly();
                    player.sendSystemMessage(Component.literal("§e⚡ [Criativo] Marcha da Vanguarda acelerada instantaneamente!"));
                    return;
                }

                if (vanguardEntity.canStartContract(player)) {
                    boolean started = vanguardEntity.startContract(player);
                    if (started) {
                        player.serverLevel().playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        player.serverLevel().playSound(null, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0F, 1.0F);
                        player.sendSystemMessage(Component.literal("§6✦ [Posto da Vanguarda] §aEdital publicado! Uma companhia da Vanguarda (Berserker, Guardião ou Duelista) marchará até o posto. Chegada estimada em 7 dias."));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§cVocê precisa depositar 20 Moedas de Ouro no posto para convocar uma nova companhia."));
                }
            }
        });
        return true;
    }
}
