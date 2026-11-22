package io.github.raffasanttos.epicvanguard.block.entity;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.event.VillageWarriorSpawner;
import io.github.raffasanttos.epicvanguard.init.ModBlockEntities;
import io.github.raffasanttos.epicvanguard.init.ModItems;
import io.github.raffasanttos.epicvanguard.screen.VanguardPointMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class VanguardPointBlockEntity extends BlockEntity implements MenuProvider {
    public static final int REQUIRED_COINS = 20;
    public static final int DEFAULT_CONTRACT_TICKS = 7 * 24000; // 7 dias in-game = 168.000 ticks

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(ModItems.GOLD_COIN.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            // Slot vazio: as moedas não pertencem mais a ninguém
            if (getStackInSlot(0).isEmpty()) {
                depositorUUID = null;
            }
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private boolean contractActive = false;
    private int remainingTicks = 0;
    private int totalTicks = DEFAULT_CONTRACT_TICKS;
    private UUID requesterUUID = null;
    private String requesterName = "";
    private long lastDayTime = -1L;
    /** Quem depositou as moedas que estão no slot: só ele pode retirá-las, completá-las ou usá-las no edital. */
    private UUID depositorUUID = null;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> contractActive ? 1 : 0;
                case 1 -> remainingTicks & 0xFFFF;
                case 2 -> (remainingTicks >> 16) & 0xFFFF;
                case 3 -> totalTicks & 0xFFFF;
                case 4 -> (totalTicks >> 16) & 0xFFFF;
                case 5 -> getStoredCoins();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> contractActive = (value == 1);
                case 1 -> remainingTicks = (remainingTicks & 0xFFFF0000) | (value & 0xFFFF);
                case 2 -> remainingTicks = (remainingTicks & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                case 3 -> totalTicks = (totalTicks & 0xFFFF0000) | (value & 0xFFFF);
                case 4 -> totalTicks = (totalTicks & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public VanguardPointBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VANGUARD_POINT_BE.get(), pos, state);
    }

    public boolean isContractActive() {
        return contractActive;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public int getStoredCoins() {
        return itemHandler.getStackInSlot(0).getCount();
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Nullable
    public UUID getDepositorUUID() {
        return depositorUUID;
    }

    public void setDepositor(@Nullable UUID uuid) {
        this.depositorUUID = uuid;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * O jogador pode mexer nas moedas do slot (vazio, são dele, ou é operador). Criativo não basta: num servidor
     * com criativo liberado, qualquer um tiraria as moedas dos outros. Criativo continua contratando de graça.
     */
    public boolean canAccessCoins(Player player) {
        return depositorUUID == null || depositorUUID.equals(player.getUUID())
                || player.hasPermissions(2);
    }

    /**
     * Quebrar o bloco dropa as moedas e cancela o edital. Só pode quem depositou as moedas que estão lá,
     * quem pagou o edital em andamento, ou um operador. Assim ninguém rouba moedas nem cancela o edital alheio.
     */
    public boolean canBreak(Player player) {
        if (player.hasPermissions(2)) return true;
        if (contractActive && requesterUUID != null && !requesterUUID.equals(player.getUUID())) return false;
        return getStoredCoins() == 0 || canAccessCoins(player);
    }

    public boolean canStartContract(Player player) {
        if (contractActive) return false;
        if (player.isCreative()) return true;
        // Não pode usar moedas que outro jogador depositou
        return getStoredCoins() >= REQUIRED_COINS && canAccessCoins(player);
    }

    public boolean startContract(Player player) {
        if (!canStartContract(player)) return false;

        if (!player.isCreative()) {
            itemHandler.extractItem(0, REQUIRED_COINS, false);
            // Devolve imediatamente qualquer moeda excedente (troco) ao jogador!
            int remaining = itemHandler.getStackInSlot(0).getCount();
            if (remaining > 0) {
                ItemStack change = itemHandler.extractItem(0, remaining, false);
                if (!player.getInventory().add(change)) {
                    player.drop(change, false);
                }
            }
        }

        this.contractActive = true;
        // Duração vem da config (contractDays, padrão 7 dias)
        this.totalTicks = io.github.raffasanttos.epicvanguard.config.EpicVanguardConfig.contractTicks();
        this.remainingTicks = this.totalTicks;
        this.requesterUUID = player.getUUID();
        this.requesterName = player.getName().getString();
        this.lastDayTime = (level != null) ? level.getDayTime() : -1L;

        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    public void completeContractInstantly() {
        if (contractActive) {
            this.remainingTicks = 1;
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VanguardPointBlockEntity entity) {
        if (!entity.contractActive) return;

        // Devolve moedas excedentes presas no bloco ao dono delas, se ele estiver por perto
        if (entity.getStoredCoins() > 0) {
            Player nearbyPlayer = entity.depositorUUID != null ? level.getPlayerByUUID(entity.depositorUUID) : null;
            if (nearbyPlayer != null && nearbyPlayer.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
                nearbyPlayer = null;
            }
            if (nearbyPlayer != null) {
                int left = entity.getStoredCoins();
                ItemStack change = entity.getItemHandler().extractItem(0, left, false);
                if (!change.isEmpty()) {
                    if (!nearbyPlayer.getInventory().add(change)) {
                        nearbyPlayer.drop(change, false);
                    }
                    entity.setChanged();
                }
            }
        }

        // Suporte ao avanço de tempo caso jogadores durmam na cama (skipping night)
        long currentDayTime = level.getDayTime();
        if (entity.lastDayTime != -1L && currentDayTime > entity.lastDayTime) {
            long elapsed = currentDayTime - entity.lastDayTime;
            if (elapsed > 1 && elapsed < 240000) {
                entity.remainingTicks -= (int) (elapsed - 1);
            }
        }
        entity.lastDayTime = currentDayTime;

        entity.remainingTicks--;

        if (entity.remainingTicks % 40 == 0) {
            entity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }

        if (entity.remainingTicks <= 0) {
            entity.finishArrival((ServerLevel) level, pos);
        }
    }

    private void finishArrival(ServerLevel serverLevel, BlockPos pos) {
        BlockPos spawnPos = VillageWarriorSpawner.findDirectPointSpawnPos(serverLevel, pos);
        if (spawnPos == null) {
            spawnPos = pos.above();
        }

        WarriorCompanionEntity companion = VillageWarriorSpawner.createRandomSpecializedCompanion(serverLevel);
        if (companion != null) {
            companion.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                    serverLevel.random.nextFloat() * 360.0F, 0.0F);
            companion.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null, null);
            companion.applyEquipmentTier(WarriorCompanionEntity.rollRandomTier(serverLevel.random));

            // Chega como mercenário neutro, pronto para ser recrutado no posto!
            // O pagamento no posto serviu exclusivamente para convocar a marcha da companhia.
            companion.setRecruited(false);
            companion.setCombatMode(1); // Postura de guarda/patrulha do posto enquanto aguarda contratação

            serverLevel.addFreshEntity(companion);

            // 🎆 Fogos de artifício dourados
            VillageWarriorSpawner.launchArrivalFirework(serverLevel, spawnPos);

            // 📯 Sons de celebração épica
            serverLevel.playSound(null, pos, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).get(), SoundSource.NEUTRAL, 1.5F, 1.0F);
            serverLevel.playSound(null, pos, SoundEvents.RAID_HORN.get(), SoundSource.NEUTRAL, 1.8F, 1.0F);

            // ✨ Partículas de fumaça e triunfo
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D,
                    25, 0.6D, 0.6D, 0.6D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    15, 0.2D, 0.4D, 0.2D, 0.02D);

            // 💬 Notificação para o jogador contratante e arredores anunciando a chegada para recrutamento
            Component specTitle = companion.getFormattedSpecializationTitle();
            Component arrivalMsg = Component.translatable("epicvanguard.text.vanguard_point_block_entity.1", specTitle, companion.getWarriorName());

            if (this.requesterUUID != null) {
                ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(this.requesterUUID);
                if (player != null) {
                    player.sendSystemMessage(arrivalMsg);
                }
            }

            // Notifica também quem estiver nas proximidades
            for (ServerPlayer nearby : serverLevel.getPlayers(p -> p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 48.0D * 48.0D)) {
                if (this.requesterUUID == null || !nearby.getUUID().equals(this.requesterUUID)) {
                    nearby.sendSystemMessage(arrivalMsg);
                }
            }
        }

        // Reseta o estado do edital para novas convocações
        this.contractActive = false;
        this.remainingTicks = 0;
        this.requesterUUID = null;
        this.requesterName = "";
        setChanged();
        serverLevel.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
    }

    // O slot de moedas não é exposto a funis e canos (sem ITEM_HANDLER): por eles qualquer um tiraria
    // as moedas de outro jogador, passando por cima da checagem de dono do menu.

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putBoolean("ContractActive", contractActive);
        tag.putInt("RemainingTicks", remainingTicks);
        tag.putInt("TotalTicks", totalTicks);
        if (requesterUUID != null) {
            tag.putUUID("RequesterUUID", requesterUUID);
        }
        tag.putString("RequesterName", requesterName);
        tag.putLong("LastDayTime", lastDayTime);
        if (depositorUUID != null) {
            tag.putUUID("DepositorUUID", depositorUUID);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        }
        contractActive = tag.getBoolean("ContractActive");
        remainingTicks = tag.getInt("RemainingTicks");
        totalTicks = tag.contains("TotalTicks") ? tag.getInt("TotalTicks") : DEFAULT_CONTRACT_TICKS;
        if (tag.hasUUID("RequesterUUID")) {
            requesterUUID = tag.getUUID("RequesterUUID");
        } else {
            requesterUUID = null;
        }
        requesterName = tag.getString("RequesterName");
        lastDayTime = tag.getLong("LastDayTime");
        depositorUUID = tag.hasUUID("DepositorUUID") ? tag.getUUID("DepositorUUID") : null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("epicvanguard.text.vanguard_point_block_entity.2");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new VanguardPointMenu(containerId, playerInventory, this, this.dataAccess);
    }
}
