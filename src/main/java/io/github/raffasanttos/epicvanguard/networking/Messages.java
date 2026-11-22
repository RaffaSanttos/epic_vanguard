package io.github.raffasanttos.epicvanguard.networking;

import io.github.raffasanttos.epicvanguard.EpicVanguardMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketOpenWarriorGUI;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketRecruitWarrior;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketWarriorCancelAction;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketWarriorCombatMode;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketWarriorTactics;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketPostVanguardContract;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketRenameWarrior;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketChooseSpecialization;

public class Messages {
    /**
     * Versão do protocolo de rede. Cliente e servidor precisam ter a mesma; aumente sempre que
     * um pacote mudar de formato ou for adicionado/removido.
     */
    private static final String PROTOCOL_VERSION = "4"; // 4: coleta com 3 modos (desligada / úteis / tudo)

    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(EpicVanguardMod.MOD_ID, "messages"))
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
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

        net.messageBuilder(PacketWarriorTactics.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketWarriorTactics::new)
                .encoder(PacketWarriorTactics::toBytes)
                .consumerMainThread(PacketWarriorTactics::handle)
                .add();

        net.messageBuilder(PacketPostVanguardContract.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketPostVanguardContract::new)
                .encoder(PacketPostVanguardContract::toBytes)
                .consumerMainThread(PacketPostVanguardContract::handle)
                .add();

        net.messageBuilder(PacketRenameWarrior.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketRenameWarrior::new)
                .encoder(PacketRenameWarrior::toBytes)
                .consumerMainThread(PacketRenameWarrior::handle)
                .add();

        net.messageBuilder(PacketChooseSpecialization.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketChooseSpecialization::new)
                .encoder(PacketChooseSpecialization::toBytes)
                .consumerMainThread(PacketChooseSpecialization::handle)
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
