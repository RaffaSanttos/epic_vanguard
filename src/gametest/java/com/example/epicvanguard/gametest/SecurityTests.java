package com.example.epicvanguard.gametest;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.command.VanguardCommand;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModItems;
import com.example.epicvanguard.networking.PacketGuards;
import com.example.epicvanguard.networking.packet.PacketRecruitWarrior;
import com.example.epicvanguard.screen.WarriorCompanionMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.example.epicvanguard.gametest.TestUtil.*;

/** Multiplayer: quem pode comandar, abrir e renomear o guerreiro de quem; recrutamento e resgate. */
@GameTestHolder(EpicVanguardMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class SecurityTests {

    @GameTest(template = PLATFORM)
    public static void packetGuardsRejectNonOwner(GameTestHelper helper) {
        ServerPlayer owner = player(helper, false, 2, 2);
        ServerPlayer stranger = player(helper, false, 6, 2);
        withPlayers(() -> {
            WarriorCompanionEntity warrior = warrior(helper, 4, 4);
            recruit(warrior, owner.getUUID());

            check(PacketGuards.ownedWarrior(owner, warrior.getId()) != null, "o dono perto deveria poder comandar");
            check(PacketGuards.ownedWarrior(stranger, warrior.getId()) == null, "outro jogador não pode comandar");
            check(PacketGuards.nearbyWarrior(stranger, warrior.getId()) != null, "outro jogador perto só pode ver/contratar");

            // Longe demais: nem o dono comanda pela tela
            owner.teleportTo(owner.getX() + 30.0D, owner.getY(), owner.getZ());
            check(PacketGuards.ownedWarrior(owner, warrior.getId()) == null, "o dono a 30 blocos não deveria comandar pela tela");

            data(helper).unregister(warrior.getUUID());
        }, owner, stranger);
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void onlyOwnerKeepsInventoryOpen(GameTestHelper helper) {
        ServerPlayer owner = player(helper, false, 2, 2);
        ServerPlayer stranger = player(helper, false, 6, 2);
        withPlayers(() -> {
            WarriorCompanionEntity warrior = warrior(helper, 4, 3);
            recruit(warrior, owner.getUUID());

            WarriorCompanionMenu ownerMenu = new WarriorCompanionMenu(1, owner.getInventory(), warrior.getId());
            WarriorCompanionMenu strangerMenu = new WarriorCompanionMenu(2, stranger.getInventory(), warrior.getId());
            check(ownerMenu.stillValid(owner), "o dono deveria poder manter o inventário aberto");
            check(!strangerMenu.stillValid(stranger), "outro jogador não pode mexer no inventário (roubo de itens)");

            data(helper).unregister(warrior.getUUID());
        }, owner, stranger);
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void creativeNonOpCannotRenameOthersCompanion(GameTestHelper helper) {
        ServerPlayer owner = player(helper, false, 2, 2);
        ServerPlayer creative = player(helper, true, 6, 2);
        withPlayers(() -> {
            WarriorCompanionEntity warrior = warrior(helper, 4, 4);
            recruit(warrior, owner.getUUID());
            warrior.setWarriorName("Aldric");
            String id = warrior.getUUID().toString();

            CommandSourceStack creativeSource = creative.createCommandSourceStack();
            check(!creativeSource.hasPermission(2), "o jogador de teste não deveria ser operador");
            check(!VanguardCommand.hasAdminAccess(creativeSource), "criativo sem ser operador não é admin");

            run(helper, creativeSource, "companions rename " + id + " Hacked");
            checkEquals("Aldric", warrior.getWarriorName(), "nome depois do rename de outro jogador");

            // O dono consegue (garante que o comando em si funciona)
            run(helper, owner.createCommandSourceStack(), "companions rename " + id + " Gareth");
            checkEquals("Gareth", warrior.getWarriorName(), "nome depois do rename do dono");

            data(helper).unregister(warrior.getUUID());
        }, owner, creative);
        helper.succeed();
    }

    private static void run(GameTestHelper helper, CommandSourceStack source, String command) {
        helper.getLevel().getServer().getCommands().performPrefixedCommand(source, command);
    }

    @GameTest(template = PLATFORM)
    public static void goldCoinsAreChargedCorrectly(GameTestHelper helper) {
        ServerPlayer survival = player(helper, false, 2, 2);
        ServerPlayer creative = player(helper, true, 6, 2);
        withPlayers(() -> {
            survival.getInventory().clearContent();
            survival.getInventory().add(new ItemStack(ModItems.GOLD_COIN.get(), 30));

            check(PacketRecruitWarrior.consumeGoldCoins(survival, 25), "deveria pagar 25 tendo 30");
            checkEquals(5, PacketRecruitWarrior.countGoldCoins(survival), "moedas depois de pagar 25");

            check(!PacketRecruitWarrior.consumeGoldCoins(survival, 10), "não deveria pagar 10 tendo 5");
            checkEquals(5, PacketRecruitWarrior.countGoldCoins(survival), "moedas depois de não conseguir pagar");

            creative.getInventory().clearContent();
            check(PacketRecruitWarrior.consumeGoldCoins(creative, 60), "criativo contrata de graça");
        }, survival, creative);
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void prisonerIsRescuedWithFood(GameTestHelper helper) {
        ServerPlayer rescuer = player(helper, false, 2, 2);
        withPlayers(() -> {
            WarriorCompanionEntity prisoner = warrior(helper, 4, 3);
            prisoner.applyEquipmentTier(-1);
            check(prisoner.isPrisoner(), "deveria começar como prisioneiro");

            rescuer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 3));
            prisoner.mobInteract(rescuer, InteractionHand.MAIN_HAND);

            check(!prisoner.isPrisoner(), "deveria deixar de ser prisioneiro");
            check(prisoner.isRecruited() && prisoner.isOwner(rescuer), "deveria jurar lealdade a quem o resgatou");
            checkEquals(2, rescuer.getMainHandItem().getCount(), "pães depois de entregar 1");

            data(helper).unregister(prisoner.getUUID());
        }, rescuer);
        helper.succeed();
    }
}
