package com.example.epicvanguard.gametest;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.block.entity.VanguardPointBlockEntity;
import com.example.epicvanguard.entity.CompanionSavedData;
import com.example.epicvanguard.entity.CompanionSavedData.FriendlyFireMode;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModBlocks;
import com.example.epicvanguard.inventory.WarriorInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.example.epicvanguard.gametest.TestUtil.*;

/** Correções de 22/11/2026: duplicação em saves antigos, desgaste de armadura, moedas do Posto e migração do save. */
@GameTestHolder(EpicVanguardMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class FixesTests {

    private static ListTag chances(float value, int count) {
        ListTag list = new ListTag();
        for (int i = 0; i < count; i++) list.add(FloatTag.valueOf(value));
        return list;
    }

    @GameTest(template = PLATFORM)
    public static void oldSaveDoesNotDuplicateEquipment(GameTestHelper helper) {
        ServerPlayer killer = player(helper, false, 1, 1);
        withPlayers(() -> {
            WarriorCompanionEntity original = warrior(helper, 4, 4);
            original.getWarriorInventory().setItem(WarriorInventory.SLOT_CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            original.syncEquipmentWithInventory();

            // Save antigo: chance de drop gravada no NBT. 2.0 = drop garantido, para o teste não depender de sorte.
            CompoundTag tag = saveEntity(original);
            tag.put("ArmorDropChances", chances(2.0F, 4));
            tag.put("HandDropChances", chances(2.0F, 2));
            original.discard();

            WarriorCompanionEntity loaded = loadCopy(helper, tag);
            helper.getLevel().addFreshEntity(loaded);
            loaded.setLastHurtByPlayer(killer);
            loaded.die(helper.getLevel().damageSources().playerAttack(killer));

            int chestplates = 0;
            AABB area = new AABB(helper.absolutePos(new BlockPos(4, 1, 4))).inflate(4.0D);
            for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area)) {
                if (item.getItem().is(Items.DIAMOND_CHESTPLATE)) chestplates += item.getItem().getCount();
            }
            checkEquals(1, chestplates, "peitorais dropados (2 = duplicou)");
        }, killer);
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void armorWearsWhenHit(GameTestHelper helper) {
        WarriorCompanionEntity warrior = warrior(helper, 4, 4);
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        warrior.getWarriorInventory().setItem(WarriorInventory.SLOT_CHEST, chest);
        warrior.syncEquipmentWithInventory();
        Zombie zombie = zombie(helper, 5, 4);

        check(warrior.hurt(helper.getLevel().damageSources().mobAttack(zombie), 8.0F), "o golpe deveria acertar");
        ItemStack after = warrior.getWarriorInventory().getItem(WarriorInventory.SLOT_CHEST);
        check(after.getDamageValue() > 0, "a armadura deveria perder durabilidade (dano: " + after.getDamageValue() + ")");
        check(warrior.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getDamageValue() == after.getDamageValue(),
                "o slot de equipamento e o inventário deveriam mostrar a mesma peça");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void creativeCannotTakeOthersCoins(GameTestHelper helper) {
        ServerPlayer depositor = player(helper, false, 2, 2);
        ServerPlayer creative = player(helper, true, 6, 2);
        withPlayers(() -> {
            BlockPos pos = new BlockPos(4, 1, 6);
            helper.setBlock(pos, ModBlocks.VANGUARD_POINT.get());
            check(helper.getBlockEntity(pos) instanceof VanguardPointBlockEntity, "o Posto da Vanguarda deveria ter block entity");
            VanguardPointBlockEntity point = (VanguardPointBlockEntity) helper.getBlockEntity(pos);
            point.setDepositor(depositor.getUUID());

            check(point.canAccessCoins(depositor), "quem depositou mexe nas próprias moedas");
            check(!point.canAccessCoins(creative), "criativo sem ser operador não mexe nas moedas dos outros");
            check(point.canStartContract(creative), "criativo continua contratando de graça");
        }, depositor, creative);
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void legacyCompanionDataIsMigrated(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        Path dataDir = server.getWorldPath(LevelResource.ROOT).resolve("data");
        Path legacy = dataDir.resolve(CompanionSavedData.LEGACY_DATA_NAME + ".dat");
        // O mundo de teste (run/world) é reaproveitado entre execuções e pode ter um arquivo antigo de verdade
        byte[] previous = Files.exists(legacy) ? Files.readAllBytes(legacy) : null;

        CompanionSavedData old = new CompanionSavedData();
        old.setGlobalFriendlyFireMode(FriendlyFireMode.ALL);
        CompoundTag root = new CompoundTag();
        root.put("data", old.save(new CompoundTag()));
        Files.createDirectories(dataDir);
        try {
            NbtIo.writeCompressed(root, legacy.toFile());
            CompanionSavedData migrated = CompanionSavedData.migrateLegacy(server);
            checkEquals(FriendlyFireMode.ALL, migrated.getGlobalFriendlyFireMode(), "fogo amigo lido do arquivo antigo");
            check(migrated.isDirty(), "os dados migrados deveriam ser salvos com o nome novo");
            Files.delete(legacy);
            checkEquals(FriendlyFireMode.DISABLED, CompanionSavedData.migrateLegacy(server).getGlobalFriendlyFireMode(),
                    "sem arquivo antigo começa vazio");
        } finally {
            if (previous != null) Files.write(legacy, previous);
            else Files.deleteIfExists(legacy);
        }
        helper.succeed();
    }
}
