package com.example.epicvanguard.networking;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.networking.packet.PacketOpenWarriorGUI;
import com.example.epicvanguard.networking.packet.PacketRecruitWarrior;
import com.example.epicvanguard.networking.packet.PacketWarriorCancelAction;
import com.example.epicvanguard.networking.packet.PacketWarriorCombatMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class Messages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(EpicVanguardMod.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(PacketWarriorCombatMode.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketWarriorCombatMode::new)
                .encoder(PacketWarriorCombatMode::toBytes)
                .consumerMainThread(PacketWarriorCombatMode::handle)
                .add();

        net.messageBuilder(PacketWarriorCancelAction.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketWarriorCancelAction::new)
                .encoder(PacketWarriorCancelAction::toBytes)
                .consumerMainThread(PacketWarriorCancelAction::handle)
                .add();

        net.messageBuilder(PacketOpenWarriorGUI.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketOpenWarriorGUI::new)
                .encoder(PacketOpenWarriorGUI::toBytes)
                .consumerMainThread(PacketOpenWarriorGUI::handle)
                .add();

        net.messageBuilder(PacketRecruitWarrior.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketRecruitWarrior::new)
                .encoder(PacketRecruitWarrior::toBytes)
                .consumerMainThread(PacketRecruitWarrior::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        if (INSTANCE != null) {
            INSTANCE.sendToServer(message);
        }
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        if (INSTANCE != null) {
            INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }
}
