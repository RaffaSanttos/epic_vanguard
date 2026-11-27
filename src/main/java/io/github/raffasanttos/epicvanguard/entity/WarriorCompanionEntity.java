package io.github.raffasanttos.epicvanguard.entity;

import io.github.raffasanttos.epicvanguard.init.ModBlocks;
import io.github.raffasanttos.epicvanguard.init.ModItems;
import io.github.raffasanttos.epicvanguard.init.ModPoiTypes;
import io.github.raffasanttos.epicvanguard.inventory.WarriorInventory;
import io.github.raffasanttos.epicvanguard.networking.Messages;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketOpenWarriorGUI;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketRecruitWarrior;
import io.github.raffasanttos.epicvanguard.dialogue.PersonalityArchetype;
import io.github.raffasanttos.epicvanguard.dialogue.SpeechTrigger;
import io.github.raffasanttos.epicvanguard.dialogue.WarriorSpeechSystem;
import io.github.raffasanttos.epicvanguard.entity.ai.*;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorCombatHelper;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorHealingHelper;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorInteraction;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorTeleport;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorThreatSensor;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorWarcry;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorProgression;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorEquipmentWear;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorMountHelper;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorRangedHelper;
import io.github.raffasanttos.epicvanguard.config.EpicVanguardConfig;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ShieldItem;
import io.github.raffasanttos.epicvanguard.screen.HonorContractMenu;
import io.github.raffasanttos.epicvanguard.screen.WarriorCompanionMenu;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WarriorCompanionEntity extends PathfinderMob {

    public static final int SKIN_COUNT = 3;
    /** Nome usado quando não há nenhum (fallback). */
    public static final String DEFAULT_NAME = "Warrior";

    /** Nome padrão antigo ou atual: guerreiros sem recrutar com esse nome ganham um nome sorteado. */
    public static boolean isDefaultName(String name) {
        return name == null || name.isEmpty() || "Guerreiro".equalsIgnoreCase(name) || DEFAULT_NAME.equalsIgnoreCase(name);
    }
    public static final String[] WARRIOR_NAMES = {
            "Aldric", "Arthur", "Baldwin", "Bram", "Caelan", "Cedric", "Dante", "Dorian",
            "Elric", "Eudes", "Faolan", "Finnian", "Gareth", "Gawain", "Hadwin", "Hector",
            "Ivar", "Jorik", "Kael", "Kane", "Leon", "Lyam", "Magnus", "Maren",
            "Nolann", "Orion", "Oryn", "Percival", "Phelan", "Ragnar", "Revan", "Siegfried",
            "Sorin", "Theron", "Tristan", "Ulric", "Vane", "Veron", "Wulf", "Yorick", "Zarek", "Zephyr"
    };

    // ── Synced Entity Data Accessors ──────────────────────────────────────────
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> SKIN_ID =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COMBAT_MODE =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TARGET_HOSTILES =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TARGET_PASSIVES =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PICKUP_MODE =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RECRUITED =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> WARRIOR_NAME =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DUEL_MODE =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> QUEST_STARTED =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> QUEST_TYPE =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RECRUIT_COST =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EQUIPMENT_TIER =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PRISONER =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LEVEL =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EXPERIENCE =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPECIALIZATION =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WARCRY_COOLDOWN =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PERSONALITY =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.INT);
    /** Bitmask dos talentos desbloqueados ({@link WarriorPerk}). */
    private static final EntityDataAccessor<Integer> PERKS =
            SynchedEntityData.defineId(WarriorCompanionEntity.class, EntityDataSerializers.INT);

    // ── Constantes de Especialização ──────────────────────────────────────────
    public static final int SPEC_NONE = 0;
    public static final int SPEC_BERSERKER = 1;
    public static final int SPEC_GUARDIAN = 2;
    public static final int SPEC_DUELIST = 3;
    public static final int SPEC_ARCHER = 4;
    public static final int SPEC_STANDARD_BEARER = 5;
    /** Maior id de especialização válido. */
    public static final int SPEC_MAX = SPEC_STANDARD_BEARER;

    private final WarriorInventory warriorInventory = new WarriorInventory();
    private final WarriorThreatSensor.Cache threatCache = new WarriorThreatSensor.Cache();
    private int stamina = 100;
    private int staminaRegenCooldown = 0;
    private boolean inStaminaRegen = false;
    private int dodgeCooldown = 0;
    private int emergencyRetreatCooldown = 0;
    private BlockPos guardPos = null;
    private LivingEntity duelTarget = null;
    private ItemStack[] questItems = new ItemStack[0];
    private Player talkingPlayer = null;
    private int talkingTicks = 0;
    private int globalSpeechCooldown = 0;
    private final int[] triggerSpeechCooldowns = new int[SpeechTrigger.values().length];
    /** Recarga do talento Segundo Fôlego (ticks). */
    private int secondWindCooldown = 0;
    /** Última montaria usada (preferida na próxima vez). */
    @Nullable
    private UUID mountUUID = null;
    /** Duelista com Contra-ataque: próximo golpe depois de uma esquiva causa +50%. */
    private boolean counterReady = false;

    public static final int SECOND_WIND_COOLDOWN_TICKS = 6000; // 5 minutos

    @Nullable
    public UUID getMountUUID() { return mountUUID; }
    public void setMountUUID(@Nullable UUID uuid) { this.mountUUID = uuid; }

    /** Mesmo deslocamento do jogador: o modelo é o de jogador. */
    @Override
    public double getMyRidingOffset() {
        return -0.35D;
    }

    // ── Árvore de habilidades ─────────────────────────────────────────────────
    public int getPerks() {
        return WarriorPerk.sanitize(this.entityData.get(PERKS));
    }

    public void setPerks(int mask) {
        this.entityData.set(PERKS, WarriorPerk.sanitize(mask));
        recalculateAttributes();
    }

    public boolean hasPerk(WarriorPerk perk) {
        return (getPerks() & perk.mask()) != 0;
    }

    /** Pontos de talento livres: 1 a cada 2 níveis, menos os já gastos. */
    public int getSkillPoints() {
        return Math.max(0, WarriorPerk.pointsForLevel(getWarriorLevel()) - Integer.bitCount(getPerks()));
    }

    /** Motivo pelo qual o talento não pode ser desbloqueado, ou {@code null} se pode. */
    @Nullable
    public Component getPerkLockReason(WarriorPerk perk) {
        if (hasPerk(perk)) return Component.translatable("epicvanguard.perk.reason.owned");
        if (getWarriorLevel() < perk.getRequiredLevel())
            return Component.translatable("epicvanguard.perk.reason.level", perk.getRequiredLevel());
        WarriorPerk pre = perk.getPrerequisite();
        if (pre != null && !hasPerk(pre))
            return Component.translatable("epicvanguard.perk.reason.prerequisite", pre.getName(getSpecialization()));
        if (getSkillPoints() <= 0) return Component.translatable("epicvanguard.perk.reason.points");
        return null;
    }

    /** Desbloqueia o talento se possível (servidor). */
    public boolean unlockPerk(WarriorPerk perk) {
        if (this.level().isClientSide || getPerkLockReason(perk) != null) return false;
        float oldMax = this.getMaxHealth();
        setPerks(getPerks() | perk.mask());
        if (this.getMaxHealth() > oldMax) {
            this.heal(this.getMaxHealth() - oldMax);
        }
        updateSavedData();
        return true;
    }

    public int getSecondWindCooldown() { return secondWindCooldown; }
    public void setSecondWindCooldown(int ticks) { this.secondWindCooldown = Math.max(0, ticks); }

    public void setCounterReady(boolean ready) { this.counterReady = ready; }

    /** Inimigo que os gritos de guerra afetam: hostil e não um neutro calmo (Enderman, piglin zumbi, etc.). */
    public static boolean isWarcryTarget(LivingEntity entity) {
        if (!entity.isAlive() || !(entity instanceof Enemy)) return false;
        if (entity instanceof NeutralMob neutral && !neutral.isAngry()) return false;
        return true;
    }

    public boolean isInStaminaRegen() {
        return this.inStaminaRegen;
    }

    public void setInStaminaRegen(boolean inStaminaRegen) {
        this.inStaminaRegen = inStaminaRegen;
    }

    public int getStaminaRegenCooldown() {
        return this.staminaRegenCooldown;
    }

    public void setStaminaRegenCooldown(int cooldown) {
        this.staminaRegenCooldown = cooldown;
    }

    public int getDodgeCooldown() {
        return this.dodgeCooldown;
    }

    public void setDodgeCooldown(int cooldown) {
        this.dodgeCooldown = cooldown;
    }

    public boolean isLowHealth() {
        float threshold = switch (this.getSpecialization()) {
            case SPEC_BERSERKER -> 0.20F; // Berserker luta frenético até 20%
            case SPEC_GUARDIAN -> 0.30F;  // Guardião segura a linha até 30%
            case SPEC_DUELIST -> 0.35F;   // Duelista é ágil e preserva integridade física
            case SPEC_ARCHER -> 0.35F;    // Arqueiro não gosta de briga de perto
            default -> 0.30F;
        };
        return this.getHealth() <= (this.getMaxHealth() * threshold);
    }

    public boolean isEmergencyRetreating() {
        return this.emergencyRetreatCooldown > 0;
    }

    /** Tem comida/poção que serve para se curar em emergência. */
    public boolean hasEmergencyConsumable() {
        return WarriorHealingHelper.findBestConsumableSlot(this, true) != -1;
    }

    /**
     * Deve evitar combate: recuando, ou com vida baixa e algo para se curar.
     * Sem nada para comer ele luta mesmo com vida baixa; antes ficava passivo para sempre.
     */
    public boolean shouldAvoidCombat() {
        return this.isEmergencyRetreating() || (this.isLowHealth() && this.hasEmergencyConsumable());
    }

    public void setEmergencyRetreatCooldown(int ticks) {
        this.emergencyRetreatCooldown = ticks;
    }

    @Override
    public void setTarget(@javax.annotation.Nullable LivingEntity target) {
        if (target != null && this.shouldAvoidCombat()) {
            super.setTarget(null);
            return;
        }
        LivingEntity prevTarget = this.getTarget();
        super.setTarget(target);
        if (target != null && target.isAlive() && prevTarget == null) {
            WarriorSpeechSystem.onSpotTarget(this, target);
        }
    }

    public void performEmergencyDodgeRoll(@Nullable Vec3 threatPos) {
        WarriorCombatHelper.performEmergencyDodgeRoll(this, threatPos);
    }

    public void applyRollImpulse(Vec3 dir) {
        WarriorCombatHelper.applyRollImpulse(this, dir);
    }

    public boolean hasShield() {
        return this.getItemBySlot(EquipmentSlot.OFFHAND).getItem() instanceof ShieldItem ||
               this.getItemBySlot(EquipmentSlot.MAINHAND).getItem() instanceof ShieldItem;
    }

    @Nullable
    public InteractionHand getShieldHand() {
        if (this.getItemBySlot(EquipmentSlot.OFFHAND).getItem() instanceof ShieldItem) {
            return InteractionHand.OFF_HAND;
        }
        if (this.getItemBySlot(EquipmentSlot.MAINHAND).getItem() instanceof ShieldItem) {
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    public boolean isActivelyBlocking() {
        return this.isBlocking();
    }

    public void raiseShield() {
        InteractionHand hand = getShieldHand();
        if (hand != null && !this.isUsingItem()) {
            this.startUsingItem(hand);
        }
    }

    public void lowerShield() {
        if (this.isUsingItem() && this.getUseItem().getItem() instanceof ShieldItem) {
            this.stopUsingItem();
        }
    }

    public void performTacticalStep(Vec3 direction, double strength) {
        if (this.hasImpulse || this.dodgeCooldown > 0) return;
        this.setDeltaMovement(direction.x * strength, 0.12D, direction.z * strength);
        this.hasImpulse = true;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 0.6F, 1.6F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.1D, this.getZ(),
                    4, 0.15D, 0.05D, 0.15D, 0.02D);
        }
    }

    @Nullable
    public Creeper getImminentCreeperThreat(double radius) {
        return WarriorThreatSensor.imminentCreeper(this, radius);
    }

    public WarriorThreatSensor.Cache getThreatCache() {
        return threatCache;
    }

    public boolean isThreatenedByProjectiles(double radius) {
        return WarriorThreatSensor.threatenedByProjectiles(this, radius);
    }

    @Nullable
    public Entity getIncomingProjectileThreat(double radius) {
        return WarriorThreatSensor.incomingProjectile(this, radius);
    }

    public boolean isOwnerInCriticalDanger() {
        if (!isRecruited() || getCombatMode() == 2) return false;
        Player owner = getOwner();
        if (owner == null || !owner.isAlive() || owner.isCreative() || owner.isSpectator()) return false;
        if (this.distanceToSqr(owner) > 256.0D) return false;
        return owner.getHealth() <= 6.0F;
    }

    @Nullable
    public LivingEntity getOwnerAttacker() {
        Player owner = getOwner();
        if (owner == null) return null;
        LivingEntity lastHurt = owner.getLastHurtByMob();
        if (lastHurt != null && lastHurt.isAlive() && lastHurt != this && lastHurt != owner) {
            return lastHurt;
        }
        List<Mob> nearbyThreats = this.level().getEntitiesOfClass(
                Mob.class,
                owner.getBoundingBox().inflate(8.0D),
                m -> m.isAlive() && m.getTarget() == owner
        );
        return nearbyThreats.isEmpty() ? null : nearbyThreats.get(0);
    }

    public boolean isFighting() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && target != this.getOwner()) {
            return true;
        }
        LivingEntity lastHurt = this.getLastHurtByMob();
        if (lastHurt != null && lastHurt.isAlive() && lastHurt != this.getOwner()
                && (this.tickCount - this.getLastHurtByMobTimestamp()) < 60) {
            return true;
        }
        return false;
    }

    private Player inventoryOpenPlayer = null;
    /** Geração desta cópia do companheiro (ver {@link CompanionSavedData.CompanionInfo#generation}). */
    private int restoreGeneration = 0;

    public int getRestoreGeneration() { return restoreGeneration; }
    public void setRestoreGeneration(int generation) { this.restoreGeneration = generation; }

    /** True se existe uma cópia mais nova deste companheiro registrada no SavedData. */
    public boolean isStaleCopy() {
        if (this.getServer() == null) return false;
        CompanionSavedData.CompanionInfo info = CompanionSavedData.get(this.getServer()).getCompanion(this.getUUID());
        return info != null && this.restoreGeneration < info.generation;
    }

    /** Fecha a "tela aberta" só se quem fechou for quem abriu (dois jogadores podem olhar ao mesmo tempo). */
    public void clearInventoryOpenPlayer(Player player) {
        if (this.inventoryOpenPlayer == null || this.inventoryOpenPlayer == player) {
            this.inventoryOpenPlayer = null;
        }
    }

    public void setInventoryOpenPlayer(@Nullable Player player) {
        this.inventoryOpenPlayer = player;
        if (player != null) {
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            lookAtPlayerDirectly(player);
        }
    }

    public boolean isInventoryOpen() {
        return this.inventoryOpenPlayer != null && this.inventoryOpenPlayer.isAlive() && this.distanceToSqr(this.inventoryOpenPlayer) < 64.0D;
    }

    @Nullable
    public Player getInventoryOpenPlayer() {
        return isInventoryOpen() ? this.inventoryOpenPlayer : null;
    }

    public void lookAtPlayerDirectly(Player player) {
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        double dy = (player.getY() + player.getEyeHeight()) - (this.getY() + this.getEyeHeight());
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float targetYRot = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float targetXRot = (float) (-(Mth.atan2(dy, distXZ) * (180.0D / Math.PI)));

        this.getLookControl().setLookAt(player, 100.0F, 100.0F);
        this.setYRot(targetYRot);
        this.setXRot(targetXRot);
        this.setYHeadRot(targetYRot);
        this.setYBodyRot(targetYRot);
    }

    public void startTalkingWith(Player player, int durationTicks) {
        this.talkingPlayer = player;
        this.talkingTicks = durationTicks;
        this.getNavigation().stop();
        this.getLookControl().setLookAt(player, 100.0F, 100.0F);
    }

    public boolean isTalking() {
        return isInventoryOpen() || (this.talkingTicks > 0 && this.talkingPlayer != null && this.talkingPlayer.isAlive());
    }

    public WarriorCompanionEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_OTHER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_OTHER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.STICKY_HONEY, -1.0F);

        if (this.getNavigation() instanceof GroundPathNavigation groundNav) {
            groundNav.setCanOpenDoors(true);
            groundNav.setCanPassDoors(true);
        }

        // Os slots de equipamento só espelham o WarriorInventory, que é dropado inteiro na morte.
        // Sem zerar a chance de drop do Mob, armadura/arma podiam cair duas vezes (duplicação).
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }

        if (!pLevel.isClientSide && this.entityData.get(WARRIOR_NAME).isEmpty()) {
            String name = WARRIOR_NAMES[this.random.nextInt(WARRIOR_NAMES.length)];
            this.setWarriorName(name);
            this.setSkinId(this.random.nextInt(SKIN_COUNT));
            this.setPersonalityId(this.random.nextInt(PersonalityArchetype.values().length));
            this.setRecruitCost(scaledCost(35 + this.random.nextInt(11))); // 35 a 45 Peças de Ouro (Média ~40)
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D) // 10 corações (padrão humanoide / jogador)
                .add(Attributes.ARMOR, 0.0D) // Sem armadura base embutida (depende de armaduras equipadas)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D) // Dureza de armadura escalonável
                .add(Attributes.ATTACK_DAMAGE, 1.0D) // Dano de soco básico 1.0 (o dano vem da arma equipada)
                .add(Attributes.MOVEMENT_SPEED, 0.28D) // Velocidade de caminhada balanceada
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D); // Pode ser empurrado por golpes normais
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(SKIN_ID, 0);
        this.entityData.define(COMBAT_MODE, 0); // 0=Seguir (padrão inicial), 1=Guarda, 2=Parado
        this.entityData.define(TARGET_HOSTILES, true);
        this.entityData.define(TARGET_PASSIVES, false);
        this.entityData.define(PICKUP_MODE, PICKUP_OFF);
        this.entityData.define(RECRUITED, false);
        this.entityData.define(WARRIOR_NAME, "");
        this.entityData.define(DUEL_MODE, false);
        this.entityData.define(QUEST_STARTED, false);
        this.entityData.define(QUEST_TYPE, 0);
        this.entityData.define(RECRUIT_COST, 40);
        this.entityData.define(EQUIPMENT_TIER, 0);
        this.entityData.define(PRISONER, false);
        this.entityData.define(LEVEL, 1);
        this.entityData.define(EXPERIENCE, 0);
        this.entityData.define(SPECIALIZATION, SPEC_NONE);
        this.entityData.define(WARCRY_COOLDOWN, 0);
        this.entityData.define(PERSONALITY, 0);
        this.entityData.define(PERKS, 0);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public boolean isTargetHostiles() {
        return this.entityData.get(TARGET_HOSTILES);
    }

    public void setTargetHostiles(boolean targetHostiles) {
        this.entityData.set(TARGET_HOSTILES, targetHostiles);
    }

    public boolean isTargetPassives() {
        return this.entityData.get(TARGET_PASSIVES);
    }

    public void setTargetPassives(boolean targetPassives) {
        this.entityData.set(TARGET_PASSIVES, targetPassives);
    }

    /**
     * Coleta de itens do chão (botão ✋ da tela): desligada (padrão) ou pega tudo, inclusive o que o dono joga.
     * Guardado como número: o valor 1 era o antigo modo "só úteis", que agora vale como "tudo".
     */
    public static final int PICKUP_OFF = 0;
    public static final int PICKUP_ALL = 2;

    public int getPickupMode() {
        return this.entityData.get(PICKUP_MODE);
    }

    public void setPickupMode(int mode) {
        this.entityData.set(PICKUP_MODE, mode == PICKUP_OFF ? PICKUP_OFF : PICKUP_ALL);
    }

    public boolean isPickupItems() {
        return getPickupMode() != PICKUP_OFF;
    }

    /** Tick do último dano de qualquer fonte (queda, fogo, veneno incluídos), para a regeneração natural. */
    private int lastDamageTick = -10000;

    public int getLastDamageTick() {
        return lastDamageTick;
    }

    /** Ticks seguidos longe do dono fora de combate (usado por {@link WarriorTeleport#tickFollowTeleport}). */
    private int farFromOwnerTicks = 0;

    public int getFarFromOwnerTicks() {
        return farFromOwnerTicks;
    }

    public void setFarFromOwnerTicks(int ticks) {
        this.farFromOwnerTicks = ticks;
    }

    /** Modos de combate (posturas). */
    public static final int MODE_FOLLOW = 0;
    public static final int MODE_GUARD = 1;
    public static final int MODE_STAY = 2;

    /**
     * Estado de quem acabou de ser recrutado (moedas, resgate ou duelo): segue o dono e fica pacífico, sem caçar
     * monstros nem animais. Continua defendendo o dono e a si mesmo. O jogador liga o ataque pela tela.
     */
    public void applyRecruitDefaults() {
        this.setCombatMode(MODE_FOLLOW);
        this.setTargetHostiles(false);
        this.setTargetPassives(false);
        this.setTarget(null);
    }

    public int getEquipmentTier() {
        return this.entityData.get(EQUIPMENT_TIER);
    }

    public void setEquipmentTier(int tier) {
        this.entityData.set(EQUIPMENT_TIER, tier);
    }

    public boolean isPrisoner() {
        return this.entityData.get(PRISONER);
    }

    public void setPrisoner(boolean prisoner) {
        this.entityData.set(PRISONER, prisoner);
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(OWNER_UUID);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    @Nullable
    public Player getOwner() {
        return this.getOwnerUUID().map(uuid -> {
            Player p = this.level().getPlayerByUUID(uuid);
            if (p == null && this.level().getServer() != null) {
                p = this.level().getServer().getPlayerList().getPlayer(uuid);
            }
            return p;
        }).orElse(null);
    }

    public int getSkinId() {
        return this.entityData.get(SKIN_ID);
    }

    public void setSkinId(int id) {
        this.entityData.set(SKIN_ID, id);
    }

    public int getCombatMode() {
        return this.entityData.get(COMBAT_MODE);
    }

    public void setCombatMode(int mode) {
        this.entityData.set(COMBAT_MODE, mode);
        this.clearCombatTarget();
        if (mode == 1) { // 1 = Guarda
            this.setGuardPos(this.blockPosition());
        } else if (mode == 2) { // 2 = Parado
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            this.setSpeed(0.0F);
        }
        this.updateSavedData();
    }

    /** Até onde quem está de Guarda vai atrás de um alvo, medido a partir do posto (config: guardMeleeRadius / guardRangedRadius). */
    public static double guardChaseRadius() {
        return io.github.raffasanttos.epicvanguard.config.EpicVanguardConfig.guardMeleeRadius();
    }

    public static double guardRangedRadius() {
        return io.github.raffasanttos.epicvanguard.config.EpicVanguardConfig.guardRangedRadius();
    }

    /**
     * Regra única da Guarda, usada pela escolha de alvo E pelos goals de combate. Antes a escolha de alvo achava
     * monstros a até 32 blocos e o combate largava qualquer um a mais de 8 blocos do posto: o guerreiro mirava,
     * dava uns passos, largava, voltava ao posto e mirava de novo (parecia hesitar).
     *
     * @param extra folga (em blocos) para quem já está lutando, para não largar o alvo na beirada do raio
     */
    public boolean canEngageFromGuard(@Nullable LivingEntity target, double extra) {
        if (target == null || !this.isRecruited() || this.getCombatMode() != MODE_GUARD || this.guardPos == null) return true;
        if (target == this.hornOrderTarget && target.isAlive()) return true; // ordem da Corneta vai até o fim
        double r = (io.github.raffasanttos.epicvanguard.entity.util.WarriorRangedHelper.canFightAtRange(this)
                ? guardRangedRadius() : guardChaseRadius()) + extra;
        if (target == this.getLastHurtByMob()) r += 6.0D; // quem atacou o guarda pode ser respondido um pouco mais longe
        return target.distanceToSqr(Vec3.atBottomCenterOf(this.guardPos)) <= r * r;
    }

    public boolean canEngageFromGuard(@Nullable LivingEntity target) {
        return canEngageFromGuard(target, 0.0D);
    }

    /** Alvo mandado pela Corneta: quem está de Guarda persegue até o fim, mesmo longe do posto, e depois volta. */
    @Nullable
    private LivingEntity hornOrderTarget;

    public void setHornOrderTarget(@Nullable LivingEntity target) {
        this.hornOrderTarget = target;
    }

    /** Está cumprindo a ordem da Corneta (o alvo atual é o da ordem e ainda está vivo). */
    public boolean isOnHornOrder() {
        LivingEntity order = this.hornOrderTarget;
        if (order == null) return false;
        if (!order.isAlive() || order.isRemoved() || this.getTarget() != order) {
            this.hornOrderTarget = null;
            return false;
        }
        return true;
    }

    public void clearCombatTarget() {
        this.hornOrderTarget = null;
        this.setTarget(null);
        this.setLastHurtByMob(null);
        this.getNavigation().stop();
        this.duelTarget = null;
        this.inStaminaRegen = false;
    }

    public boolean isOwner(Player player) {
        return player != null && this.getOwnerUUID().map(uuid -> uuid.equals(player.getUUID())).orElse(false);
    }

    public boolean isRecruited() {
        return this.entityData.get(RECRUITED);
    }

    public void setRecruited(boolean recruited) {
        this.entityData.set(RECRUITED, recruited);
        String name = this.getWarriorName();
        String prefix = recruited ? "§f" : "§7";
        super.setCustomName(Component.literal(prefix + name));
        this.setCustomNameVisible(true);
    }

    public String getWarriorName() {
        if (this.hasCustomName()) {
            Component customComp = this.getCustomName();
            if (customComp != null) {
                String raw = customComp.getString();
                String clean = raw.replaceAll("§[0-9a-fk-or]", "").trim();
                if (!clean.isEmpty()) {
                    return clean;
                }
            }
        }
        String synched = this.entityData.get(WARRIOR_NAME);
        if (synched != null && !synched.isEmpty()) {
            return synched;
        }
        return DEFAULT_NAME;
    }

    public void setWarriorName(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = DEFAULT_NAME;
        }
        String clean = name.replaceAll("§[0-9a-fk-or]", "").trim();
        if (clean.length() > 20) {
            clean = clean.substring(0, 20);
        }
        this.entityData.set(WARRIOR_NAME, clean);
        String prefix = this.isRecruited() ? "§f" : "§7";
        super.setCustomName(Component.literal(prefix + clean));
        this.setCustomNameVisible(true);
        // Durante o carregamento do NBT o nome vem do disco e pode estar desatualizado (rename por
        // comando com o guerreiro descarregado, ou cópia antiga). Nesse caso o registro é a fonte oficial
        // e não pode ser sobrescrito; o EntityJoinLevelEvent aplica o nome do registro depois.
        if (!this.loadingFromNbt && !this.level().isClientSide() && this.getServer() != null) {
            CompanionSavedData.get(this.getServer()).updateName(this.getUUID(), clean);
        }
    }

    /** True enquanto {@link #load(CompoundTag)} está lendo o NBT (inclui CustomName e readAdditionalSaveData). */
    private boolean loadingFromNbt = false;

    @Override
    public void load(CompoundTag pCompound) {
        this.loadingFromNbt = true;
        try {
            super.load(pCompound);
        } finally {
            this.loadingFromNbt = false;
        }
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        if (name != null) {
            String clean = name.getString().replaceAll("§[0-9a-fk-or]", "").trim();
            if (!clean.isEmpty() && !clean.equals(this.entityData.get(WARRIOR_NAME))) {
                setWarriorName(clean);
            }
        }
    }

    public boolean isDuelMode() {
        return this.entityData.get(DUEL_MODE);
    }

    public void setDuelMode(boolean duel) {
        this.entityData.set(DUEL_MODE, duel);
    }

    public boolean isQuestStarted() {
        return this.entityData.get(QUEST_STARTED);
    }

    public void setQuestStarted(boolean started) {
        this.entityData.set(QUEST_STARTED, started);
    }

    public int getQuestType() {
        return this.entityData.get(QUEST_TYPE);
    }

    public void setQuestType(int type) {
        this.entityData.set(QUEST_TYPE, type);
    }

    public int getRecruitCost() {
        return this.entityData.get(RECRUIT_COST);
    }

    public void setRecruitCost(int cost) {
        this.entityData.set(RECRUIT_COST, cost);
    }

    /** Custo sorteado ajustado pela config (recruitCostMultiplier). */
    private static int scaledCost(int baseCost) {
        return Math.max(0, (int) Math.round(baseCost * io.github.raffasanttos.epicvanguard.config.EpicVanguardConfig.recruitCostMultiplier()));
    }

    public void setGuardPos(BlockPos pos) {
        this.guardPos = pos;
    }

    public BlockPos getGuardPos() {
        return this.guardPos;
    }

    public void setDuelTarget(LivingEntity target) {
        this.duelTarget = target;
    }

    public LivingEntity getDuelTarget() {
        return this.duelTarget;
    }

    public void setQuestItems(ItemStack... items) {
        this.questItems = items;
    }

    public ItemStack[] getQuestItems() {
        return this.questItems;
    }

    public WarriorInventory getWarriorInventory() {
        return this.warriorInventory;
    }

    // ── Personalidade & Diálogos (Fase 4) ─────────────────────────────────────
    public int getPersonalityId() {
        return this.entityData.get(PERSONALITY);
    }

    public void setPersonalityId(int id) {
        this.entityData.set(PERSONALITY, id);
    }

    public PersonalityArchetype getPersonality() {
        return PersonalityArchetype.byId(getPersonalityId());
    }

    public void setPersonality(PersonalityArchetype archetype) {
        this.setPersonalityId(archetype.getId());
    }

    public int getGlobalSpeechCooldown() {
        return globalSpeechCooldown;
    }

    public void setGlobalSpeechCooldown(int ticks) {
        this.globalSpeechCooldown = ticks;
    }

    public int getTriggerSpeechCooldown(SpeechTrigger trigger) {
        return triggerSpeechCooldowns[trigger.ordinal()];
    }

    public void setTriggerSpeechCooldown(SpeechTrigger trigger, int ticks) {
        this.triggerSpeechCooldowns[trigger.ordinal()] = ticks;
    }

    // ── RPG Progression & Leveling (Fase 1) ───────────────────────────────────
    public int getWarriorLevel() {
        return this.entityData.get(LEVEL);
    }

    public void setWarriorLevel(int level) {
        this.entityData.set(LEVEL, Math.max(1, Math.min(20, level)));
        recalculateAttributes();
    }

    public int getWarriorExperience() {
        return this.entityData.get(EXPERIENCE);
    }

    public void setWarriorExperience(int exp) {
        this.entityData.set(EXPERIENCE, Math.max(0, exp));
    }

    public void addWarriorExperience(int amount) {
        if (amount <= 0 || this.level().isClientSide) return;
        int currentLvl = getWarriorLevel();
        if (currentLvl >= 20) return;

        int currentExp = getWarriorExperience() + amount;
        int needed = getXpForNextLevel(currentLvl);
        boolean leveledUp = false;

        while (currentExp >= needed && currentLvl < 20) {
            currentExp -= needed;
            currentLvl++;
            this.entityData.set(LEVEL, currentLvl);
            needed = getXpForNextLevel(currentLvl);
            leveledUp = true;
        }

        setWarriorExperience(currentLvl >= 20 ? 0 : currentExp);

        if (leveledUp) {
            recalculateAttributes();
            this.heal(this.getMaxHealth() * 0.5F); // Cura 50% de bônus ao subir de nível
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.2D, this.getZ(), 25, 0.4D, 0.6D, 0.4D, 0.2D);
                serverLevel.sendParticles(ParticleTypes.FIREWORK, this.getX(), this.getY() + 1.0D, this.getZ(), 15, 0.3D, 0.5D, 0.3D, 0.1D);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.2F, 1.0F);
            }
            Player owner = getOwner();
            if (owner != null) {
                owner.sendSystemMessage(Component.translatable("epicvanguard.text.warrior_companion_entity.1", getWarriorName(), currentLvl));
            }
        }
    }

    public float getCalculatedAttackDamage() {
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float multiplier = switch (getSpecialization()) {
            case SPEC_BERSERKER -> 0.90F;
            case SPEC_STANDARD_BEARER -> 0.65F; // luta pouco: o valor dele é a aura
            default -> 0.75F;
        };
        return Math.max(1.0F, damage * multiplier);
    }

    public int getSpecialization() {
        return this.entityData.get(SPECIALIZATION);
    }

    public void setSpecialization(int spec) {
        this.entityData.set(SPECIALIZATION, spec);
        recalculateAttributes();
    }

    public int getWarcryCooldown() {
        return this.entityData.get(WARCRY_COOLDOWN);
    }

    public void setWarcryCooldown(int ticks) {
        this.entityData.set(WARCRY_COOLDOWN, Math.max(0, ticks));
    }

    public static int getXpForNextLevel(int lvl) {
        if (lvl >= 20) return 0; // Nível máximo
        return 50 + (lvl - 1) * 30 + (int) (Math.pow(lvl - 1, 1.6) * 8.0);
    }

    private static String specializationKey(int spec) {
        return switch (spec) {
            case SPEC_BERSERKER -> "berserker";
            case SPEC_GUARDIAN  -> "guardian";
            case SPEC_DUELIST   -> "duelist";
            case SPEC_ARCHER    -> "archer";
            case SPEC_STANDARD_BEARER -> "standard_bearer";
            default             -> "warrior";
        };
    }

    /** Nome traduzível da especialização (lang: epicvanguard.spec.<spec>.name). */
    public Component getSpecializationName() {
        return Component.translatable("epicvanguard.spec." + specializationKey(getSpecialization()) + ".name");
    }

    /** Título colorido traduzível (lang: epicvanguard.spec.<spec>.title). No cliente use {@code .getString()}. */
    public static Component specializationTitle(int spec) {
        return Component.translatable("epicvanguard.spec." + specializationKey(spec) + ".title");
    }

    public Component getFormattedSpecializationTitle() {
        return specializationTitle(getSpecialization());
    }

    public void triggerWarcry() {
        WarriorWarcry.trigger(this);
    }

    /** Recarga total do grito: config, -30% (talento de especialização II do Guerreiro/Guardião), -50% (Lenda). */
    public int getWarcryCooldownTotal() {
        double cd = EpicVanguardConfig.warcryCooldownTicks();
        int spec = getSpecialization();
        if (hasPerk(WarriorPerk.SPEC_T2) && (spec == SPEC_NONE || spec == SPEC_GUARDIAN)) cd *= 0.70D;
        if (hasPerk(WarriorPerk.LEGEND)) cd *= 0.50D;
        if (getWarriorLevel() >= 20) cd *= 0.80D; // marco do nível 20: grito 20% mais rápido (50 s -> 40 s)
        return (int) Math.max(100, cd);
    }

    /** O próprio guerreiro, o dono e os companheiros do mesmo dono dentro do raio. */
    public List<LivingEntity> getAuraAllies(double radius) {
        List<LivingEntity> allies = new java.util.ArrayList<>();
        allies.add(this);
        if (!isRecruited()) return allies;
        double r2 = radius * radius;
        Player owner = getOwner();
        if (owner != null && owner.level() == this.level() && owner.distanceToSqr(this) <= r2 && !owner.isSpectator()) {
            allies.add(owner);
        }
        for (WarriorCompanionEntity other : this.level().getEntitiesOfClass(WarriorCompanionEntity.class,
                this.getBoundingBox().inflate(radius), w -> w != this && w.isAlive() && this.isAlliedTo(w))) {
            if (other.distanceToSqr(this) <= r2) allies.add(other);
        }
        return allies;
    }

    /** Porta-estandarte com estandarte na mão secundária: Resistência I (e Velocidade I com Marcha) para os aliados. */
    public boolean hasBannerRaised() {
        return getSpecialization() == SPEC_STANDARD_BEARER
                && this.getWarriorInventory().getItem(WarriorInventory.SLOT_WEAPON_OFF).getItem() instanceof net.minecraft.world.item.BannerItem;
    }

    public double getBannerAuraRadius() {
        double r = EpicVanguardConfig.bannerAuraRadius();
        if (hasPerk(WarriorPerk.SPEC_T1)) r += 4.0D; // Estandarte Alto
        return r;
    }

    /** Aplica a aura uma vez (chamado a cada 40 ticks no servidor). */
    public void applyBannerAura() {
        if (!hasBannerRaised() || this.isPrisoner()) return;
        boolean march = hasPerk(WarriorPerk.SPEC_T2);
        for (LivingEntity ally : getAuraAllies(getBannerAuraRadius())) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, true));
            if (march) {
                ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, true));
            }
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY() + 2.3D, this.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D);
        }
    }

    /** Equipamento inicial específico da especialização (depois do conjunto sorteado pelo tier). */
    public void applySpecializationLoadout(int tier) {
        switch (getSpecialization()) {
            case SPEC_ARCHER -> {
                ItemStack bow = new ItemStack(tier >= 2 && this.random.nextInt(3) == 0 ? Items.CROSSBOW : Items.BOW);
                if (tier >= 2 && bow.is(Items.BOW)) {
                    bow.enchant(net.minecraft.world.item.enchantment.Enchantments.POWER_ARROWS, 1);
                }
                warriorInventory.setItem(WarriorInventory.SLOT_WEAPON_MAIN, bow);
                warriorInventory.setItem(WarriorInventory.SLOT_WEAPON_OFF, ItemStack.EMPTY);
                warriorInventory.addItemToBackpack(new ItemStack(Items.ARROW, 16 + tier * 8));
                // Faca de reserva para quando acabarem as flechas
                warriorInventory.addItemToBackpack(new ItemStack(tier >= 1 ? Items.STONE_SWORD : Items.WOODEN_SWORD));
            }
            case SPEC_STANDARD_BEARER -> {
                net.minecraft.world.item.Item[] banners = {Items.WHITE_BANNER, Items.RED_BANNER, Items.BLUE_BANNER,
                        Items.YELLOW_BANNER, Items.GREEN_BANNER, Items.BLACK_BANNER};
                warriorInventory.setItem(WarriorInventory.SLOT_WEAPON_OFF, new ItemStack(banners[this.random.nextInt(banners.length)]));
            }
            default -> {}
        }
    }






    /**
     * Recalcula dinamicamente todos os atributos escalonáveis com base no Nível e Especialização.
     */
    public void recalculateAttributes() {
        int lvl = getWarriorLevel();
        int spec = getSpecialization();

        // 1. Vida Máxima: 20.0 HP base + 1.5 HP por nível (+30% se Guardião)
        double baseMaxHealth = 20.0D + (lvl - 1) * 1.5D;
        if (spec == SPEC_GUARDIAN) {
            baseMaxHealth *= 1.30D;
        }
        if (spec == SPEC_ARCHER) {
            baseMaxHealth *= 0.90D;
        } else if (spec == SPEC_STANDARD_BEARER) {
            baseMaxHealth *= 1.15D;
        }
        if (hasPerk(WarriorPerk.VITALITY)) {
            baseMaxHealth += 4.0D;
        }
        var attrHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (attrHealth != null) {
            double oldMax = attrHealth.getBaseValue();
            attrHealth.setBaseValue(baseMaxHealth);
            if (baseMaxHealth > oldMax && this.isAlive()) {
                this.heal((float)(baseMaxHealth - oldMax));
            }
            // Vida máxima caiu (ex.: atributo recalculado ao carregar): a vida atual não pode passar do máximo
            if (this.getHealth() > this.getMaxHealth()) {
                this.setHealth(this.getMaxHealth());
            }
        }

        // 2. Dano de Ataque Físico Base: 1.0D + 0.25D por nível
        double baseAttack = 1.0D + (lvl - 1) * 0.25D;
        var attrAttack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attrAttack != null) {
            attrAttack.setBaseValue(baseAttack);
        }

        // 3. Armadura Natural: 0.0 + 0.5 por nível (+6 se Guardião)
        double naturalArmor = (lvl - 1) * 0.5D;
        if (spec == SPEC_GUARDIAN) {
            naturalArmor += 6.0D;
        }
        if (spec == SPEC_STANDARD_BEARER) {
            naturalArmor += 2.0D;
        }
        if (hasPerk(WarriorPerk.IRON_SKIN)) {
            naturalArmor += 2.0D;
        }
        var attrArmor = this.getAttribute(Attributes.ARMOR);
        if (attrArmor != null) {
            attrArmor.setBaseValue(naturalArmor);
        }

        // 4. Dureza de Armadura: 0.0 + 0.15 por nível (+2 se Guardião)
        double toughness = (lvl - 1) * 0.15D;
        if (spec == SPEC_GUARDIAN) {
            toughness += 2.0D;
        }
        var attrToughness = this.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (attrToughness != null) {
            attrToughness.setBaseValue(toughness);
        }

        // 5. Velocidade de Movimento: 0.28D + 0.002D por nível (+15% se Duelista)
        double speed = 0.28D + (lvl - 1) * 0.002D;
        if (spec == SPEC_DUELIST) {
            speed *= 1.15D;
        } else if (spec == SPEC_ARCHER) {
            speed *= 1.05D;
        }
        if (hasPerk(WarriorPerk.SWIFT_FEET)) {
            speed *= 1.08D;
        }
        var attrSpeed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attrSpeed != null) {
            attrSpeed.setBaseValue(speed);
        }

        // 6. Resistência a Repulsão: 0.0 (0.40D se Guardião)
        double knockbackRes = (spec == SPEC_GUARDIAN) ? 0.40D : 0.0D;
        if (spec == SPEC_GUARDIAN && hasPerk(WarriorPerk.SPEC_T2)) {
            knockbackRes += 0.30D; // Bastião
        }
        var attrKnockback = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attrKnockback != null) {
            attrKnockback.setBaseValue(knockbackRes);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HeroicInterveneGoal(this));
        this.goalSelector.addGoal(1, new WarriorWarcryGoal(this));
        this.goalSelector.addGoal(1, new FleeIllagersGoal(this, 12.0F, 1.2D, 1.35D));
        this.goalSelector.addGoal(1, new EmergencyRetreatAndEatGoal(this));
        this.goalSelector.addGoal(2, new ActiveShieldDefenseGoal(this));
        this.goalSelector.addGoal(2, new MountUpGoal(this));
        this.goalSelector.addGoal(3, new WarriorRangedCombatGoal(this, 1.1D));
        this.goalSelector.addGoal(3, new WarriorTacticalCombatGoal(this, 1.25D));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.15D, 8.0F, 3.0F));
        this.goalSelector.addGoal(4, new GuardRadiusGoal(this, 1.1D));
        this.goalSelector.addGoal(5, new AutoFeedGoal(this));
        this.goalSelector.addGoal(6, new CampfireRelaxGoal(this));
        this.goalSelector.addGoal(6, new VanguardBasePatrolGoal(this));
        this.goalSelector.addGoal(6, new TavernRelaxGoal(this, 0.9D));
        this.goalSelector.addGoal(7, new WarriorStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new DefendOwnerGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new AggressiveTargetGoal(this));
        this.targetSelector.addGoal(4, new HuntAnimalGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        SpawnGroupData data = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
        if (isDefaultName(this.entityData.get(WARRIOR_NAME))) {
            String name = WARRIOR_NAMES[this.random.nextInt(WARRIOR_NAMES.length)];
            this.setWarriorName(name);
            this.setSkinId(this.random.nextInt(SKIN_COUNT));
            this.setPersonalityId(this.random.nextInt(PersonalityArchetype.values().length));
            this.setRecruitCost(scaledCost(35 + this.random.nextInt(11)));
        }
        if (!this.isPrisoner() && this.getEquipmentTier() == 0 && this.getWarriorInventory().getItem(WarriorInventory.SLOT_WEAPON_MAIN).isEmpty()) {
            int rolledTier = rollRandomTier(pLevel.getRandom());
            this.applyEquipmentTier(rolledTier);
        }
        return data;
    }

    public void applyEquipmentTier(int tier) {
        this.setEquipmentTier(tier);
        if (tier == -1) {
            this.setPrisoner(true);
            this.setWarriorLevel(1);
            this.setHealth(3.0F);
            this.setRecruitCost(0);
            this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 999999, 1, false, false));
            for (int i = 0; i < warriorInventory.getContainerSize(); i++) {
                warriorInventory.setItem(i, ItemStack.EMPTY);
            }
            this.syncEquipmentWithInventory();
            this.recalculateAttributes();
            return;
        }

        this.setPrisoner(false);
        switch (tier) {
            case 0: // 60% Couro / Madeira -> Nível 1
                this.setWarriorLevel(1);
                warriorInventory.setItem(WarriorInventory.SLOT_HELMET, new ItemStack(Items.LEATHER_HELMET));
                warriorInventory.setItem(WarriorInventory.SLOT_CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                warriorInventory.setItem(WarriorInventory.SLOT_LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
                warriorInventory.setItem(WarriorInventory.SLOT_BOOTS, new ItemStack(Items.LEATHER_BOOTS));
                warriorInventory.setItem(WarriorInventory.SLOT_WEAPON_MAIN, new ItemStack(this.random.nextBoolean() ? Items.WOODEN_SWORD : Items.WOODEN_AXE));
                this.setRecruitCost(scaledCost(20 + this.random.nextInt(11))); // 20 a 30 moedas
                break;
            case 1: // 30% Cota de Malha / Pedra -> Nível 2
                this.setWarriorLevel(2);
                warriorInventory.setItem(WarriorInventory.SLOT_HELMET, new ItemStack(Items.CHAINMAIL_HELMET));
                warriorInventory.setItem(WarriorInventory.SLOT_CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                warriorInventory.setItem(WarriorInventory.SLOT_LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
                warriorInventory.setItem(WarriorInventory.SLOT_BOOTS, new ItemStack(Items.CHAINMAIL_BOOTS));
                warriorInventory.setItem(WarriorInventory.SLOT_WEAPON_MAIN, new ItemStack(this.random.nextBoolean() ? Items.STONE_SWORD : Items.STONE_AXE));
                warriorInventory.setItem(WarriorInventory.SLOT_WEAPON_OFF, new ItemStack(Items.SHIELD));
                this.setRecruitCost(scaledCost(40 + this.random.nextInt(11))); // 40 a 50 moedas
                break;
            case 2: // 10% Ferro -> Nível 3
            default:
                this.setWarriorLevel(3);
                warriorInventory.setItem(WarriorInventory.SLOT_HELMET, new ItemStack(Items.IRON_HELMET));
                warriorInventory.setItem(WarriorInventory.SLOT_CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                warriorInventory.setItem(WarriorInventory.SLOT_LEGS, new ItemStack(Items.IRON_LEGGINGS));
                warriorInventory.setItem(WarriorInventory.SLOT_BOOTS, new ItemStack(Items.IRON_BOOTS));
                warriorInventory.setItem(WarriorInventory.SLOT_WEAPON_MAIN, new ItemStack(this.random.nextBoolean() ? Items.IRON_SWORD : Items.IRON_AXE));
                warriorInventory.setItem(WarriorInventory.SLOT_WEAPON_OFF, new ItemStack(Items.SHIELD));
                this.setRecruitCost(scaledCost(60 + this.random.nextInt(21))); // 60 a 80 moedas
                break;
        }
        this.applySpecializationLoadout(tier);
        this.syncEquipmentWithInventory();
        this.recalculateAttributes();
    }

    public static int rollRandomTier(net.minecraft.util.RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 60) return 0; // 60%
        if (roll < 90) return 1; // 30%
        return 2; // 10%
    }

    @Nullable
    private net.minecraft.world.entity.animal.horse.AbstractHorse pendingMount;

    public void setPendingMount(@Nullable net.minecraft.world.entity.animal.horse.AbstractHorse horse) {
        this.pendingMount = horse;
    }

    // ── Tick & Equipment Sync ─────────────────────────────────────────────────
    @Override
    public void tick() {
        // Não existem mais guerreiros sem classe: o Guerreiro base vira uma classe sorteada
        if (!this.level().isClientSide && io.github.raffasanttos.epicvanguard.entity.util.WarriorProgression.needsClass(this)) {
            io.github.raffasanttos.epicvanguard.entity.util.WarriorProgression.assignRandomClass(this);
            if (this.isRemoved()) return;
        }
        // Montada pedida pelo MountUpGoal: feita aqui, fora da iteração dos goals (ver WarriorMountHelper#mount)
        if (this.pendingMount != null) {
            net.minecraft.world.entity.animal.horse.AbstractHorse horse = this.pendingMount;
            this.pendingMount = null;
            if (!this.level().isClientSide) {
                WarriorMountHelper.applyPendingMount(this, horse);
            }
        }
        super.tick();

        if (!this.level().isClientSide) {
            int wcCd = getWarcryCooldown();
            if (wcCd > 0) {
                setWarcryCooldown(wcCd - 1);
            }

            // Inventory Open & Talking & Looking at Player Logic
            if (isInventoryOpen()) {
                this.getNavigation().stop();
                this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
                lookAtPlayerDirectly(this.inventoryOpenPlayer);
            } else {
                if (this.inventoryOpenPlayer != null) {
                    this.inventoryOpenPlayer = null;
                }
                if (talkingTicks > 0) {
                    talkingTicks--;
                    if (talkingPlayer != null && talkingPlayer.isAlive() && this.distanceToSqr(talkingPlayer) < 64.0D) {
                        this.getNavigation().stop();
                        this.getLookControl().setLookAt(talkingPlayer, 30.0F, 30.0F);
                    } else {
                        talkingPlayer = null;
                    }
                }
            }

            // Target jump assistance when target is on elevated blocks/terrain or colliding with blocks
            LivingEntity currentTarget = this.getTarget();
            if (currentTarget != null && currentTarget.isAlive()) {
                double dy = currentTarget.getY() - this.getY();
                double dx = currentTarget.getX() - this.getX();
                double dz = currentTarget.getZ() - this.getZ();
                double horizontalDistSq = dx * dx + dz * dz;

                if (this.onGround()) {
                    if (this.horizontalCollision) {
                        this.getJumpControl().jump();
                    } else if (dy > 0.3D && dy < 3.5D && horizontalDistSq < 9.0D) {
                        this.getJumpControl().jump();
                    }
                }
            }

            // Stamina Cooldown
            if (staminaRegenCooldown > 0) {
                staminaRegenCooldown--;
                if (staminaRegenCooldown == 0) {
                    inStaminaRegen = false;
                }
            }

            // Dodge & Emergency Retreat Cooldowns
            if (dodgeCooldown > 0) {
                dodgeCooldown--;
            }
            if (emergencyRetreatCooldown > 0) {
                emergencyRetreatCooldown--;
                if (this.getTarget() != null) {
                    this.setTarget(null);
                }
                if (this.getLastHurtByMob() != null) {
                    this.setLastHurtByMob(null);
                }
            }


            // Speech Cooldowns
            if (globalSpeechCooldown > 0) {
                globalSpeechCooldown--;
            }
            for (int i = 0; i < triggerSpeechCooldowns.length; i++) {
                if (triggerSpeechCooldowns[i] > 0) {
                    triggerSpeechCooldowns[i]--;
                }
            }

            if (secondWindCooldown > 0) {
                secondWindCooldown--;
            }

            // Regeneração natural lenta fora de combate (desligada por padrão na config; 1 HP a cada 3 s, 5 s depois de qualquer dano)
            int regenInterval = EpicVanguardConfig.regenIntervalTicks();
            if (hasPerk(WarriorPerk.FIELD_MEDIC)) regenInterval = Math.max(5, regenInterval / 2);
            if (EpicVanguardConfig.naturalRegen() && this.isAlive() && !this.isPrisoner() && this.getHealth() < this.getMaxHealth()
                    && this.tickCount % regenInterval == 0 && !this.isFighting()
                    && (this.tickCount - this.lastDamageTick) > 100
                    && this.hurtTime == 0) {
                this.heal(1.0F);
            }

            // Montaria: guia o cavalo atrás do dono ou desmonta para lutar
            if (this.isPassenger()) {
                WarriorMountHelper.tickMounted(this);
            }

            // Porta-estandarte: aura a cada 2 s
            if (this.tickCount % 40 == 0) {
                applyBannerAura();
            }

            // Sem flechas em combate: saca a arma corpo a corpo (e o Arqueiro volta ao arco depois)
            if (this.tickCount % 20 == 5 && isRecruited()) {
                WarriorRangedHelper.manageWeaponSwap(this);
            }

            // Reparo em descanso: sentado (fogueira/taverna), fora de combate, a cada 5 s
            if (this.tickCount % 100 == 0 && this.getPose() == Pose.SITTING && !this.isFighting()) {
                WarriorEquipmentWear.tryFieldRepair(this);
            }

            // Sync Warrior Inventory equipment to entity equipment slots for rendering
            syncEquipmentWithInventory();

            // Modo Seguir: teleporta até o dono quando fica para trás, sem largar uma luta perto dele
            WarriorTeleport.tickFollowTeleport(this);

            // Modo Parado (2): Garante imobilidade quando sem ameaças, permitindo relaxar na fogueira ou patrulhar no Ponto de Vanguarda
            if (isRecruited() && getCombatMode() == 2) {
                if (this.getTarget() == null && this.getLastHurtByMob() == null && !isEmergencyRetreating()) {
                    boolean isRelaxingOrPatrolling = this.getPose() == Pose.SITTING || this.goalSelector.getAvailableGoals().stream().anyMatch(
                            g -> g.isRunning() && (g.getGoal() instanceof CampfireRelaxGoal || g.getGoal() instanceof VanguardBasePatrolGoal)
                    );
                    if (!isRelaxingOrPatrolling) {
                        if (this.getNavigation().isInProgress()) {
                            this.getNavigation().stop();
                        }
                        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
                        this.setSpeed(0.0F);
                    }
                }
            }

            // Coleta automática de itens do chão ao passar por cima
            pickUpGroundItems();

            // Veste sozinho armadura/arma melhor que estiver na mochila
            io.github.raffasanttos.epicvanguard.entity.util.WarriorEquipment.tickAutoEquip(this);

            // Update CompanionSavedData periodically (every 20 ticks = 1 second)
            // Snapshot completo a cada 10s; saída de chunk/mundo já salva na hora (CompanionTeleportHandler)
            if (isRecruited() && this.tickCount % 200 == 0) {
                updateSavedData();
            }
        }
    }

    public void updateSavedData() {
        if (!this.level().isClientSide && isRecruited() && this.isAlive() && this.getServer() != null && getOwnerUUID().isPresent()) {
            // Uma cópia antiga nunca pode sobrescrever o snapshot da cópia oficial
            if (isStaleCopy()) return;
            CompoundTag fullNbt = new CompoundTag();
            this.saveWithoutId(fullNbt);
            fullNbt.putInt("Specialization", this.getSpecialization());
            CompanionSavedData.get(this.getServer()).registerOrUpdate(
                    this.getUUID(),
                    getOwnerUUID().get(),
                    this.getWarriorName(),
                    this.level().dimension().location().toString(),
                    this.blockPosition(),
                    this.getCombatMode(),
                    this.getHealth(),
                    this.getMaxHealth(),
                    this.getWarriorLevel(),
                    this.getSpecialization(),
                    fullNbt
            );
        }
    }

    private void pickUpGroundItems() {
        WarriorInteraction.pickUpGroundItems(this);
    }

    /**
     * Copia o inventário do guerreiro para os slots de equipamento (render e atributos).
     * Roda todo tick, mas só chama {@code setItemSlot} quando o slot aponta para outro ItemStack:
     * o slot guarda a mesma instância do inventário, então durabilidade e encantamentos já ficam iguais.
     */
    public void syncEquipmentWithInventory() {
        syncSlot(EquipmentSlot.HEAD, WarriorInventory.SLOT_HELMET);
        syncSlot(EquipmentSlot.CHEST, WarriorInventory.SLOT_CHEST);
        syncSlot(EquipmentSlot.LEGS, WarriorInventory.SLOT_LEGS);
        syncSlot(EquipmentSlot.FEET, WarriorInventory.SLOT_BOOTS);
        if (!this.isUsingItem()) {
            syncSlot(EquipmentSlot.MAINHAND, WarriorInventory.SLOT_WEAPON_MAIN);
            syncSlot(EquipmentSlot.OFFHAND, WarriorInventory.SLOT_WEAPON_OFF);
        }
    }

    private void syncSlot(EquipmentSlot slot, int inventorySlot) {
        ItemStack stack = warriorInventory.getItem(inventorySlot);
        if (this.getItemBySlot(slot) != stack) {
            this.setItemSlot(slot, stack);
        }
    }

    @Override
    public boolean isPushable() {
        if (this.isInventoryOpen() || (this.isRecruited() && this.getCombatMode() == 2)) {
            return false;
        }
        return super.isPushable();
    }

    @Override
    public void push(Entity pEntity) {
        if (this.isInventoryOpen() || (this.isRecruited() && this.getCombatMode() == 2)) {
            return;
        }
        super.push(pEntity);
    }

    @Override
    public void aiStep() {
        if (this.isInventoryOpen() || (this.isRecruited() && this.getCombatMode() == 2)) {
            if (this.isInventoryOpen() || (this.getTarget() == null && this.getLastHurtByMob() == null && !this.isEmergencyRetreating())) {
                this.getNavigation().stop();
                this.xxa = 0.0F;
                this.zza = 0.0F;
                this.setSpeed(0.0F);
            }
        }
        super.aiStep();
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        if (this.isInventoryOpen() || (this.isRecruited() && this.getCombatMode() == 2)) {
            if (this.isInventoryOpen() || (this.getTarget() == null && this.getLastHurtByMob() == null && !this.isEmergencyRetreating())) {
                if (this.isEffectiveAi()) {
                    this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
                    super.travel(new net.minecraft.world.phys.Vec3(0.0D, travelVector.y, 0.0D));
                    return;
                }
            }
        }
        super.travel(travelVector);
    }

    /** Dano de magia/explosão/raio (para o fogo amigo "só magias" / "só físico"). */
    public static boolean isSpellOrMagicDamage(DamageSource source) {
        return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)
                || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)
                || source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                || source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)
                || source.is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION)
                || source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_EXPLOSION)
                || source.is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (!WarriorCombatHelper.checkFriendlyFire(this, pSource)) {
            return false;
        }

        if (WarriorCombatHelper.checkDuelistDodge(this, pSource)) {
            return false;
        }

        pAmount = WarriorCombatHelper.applyShieldBlock(this, pSource, pAmount);

        // Duel Mode mechanic: If damage would drop HP to <= 0 in duel
        // Duelo: só o adversário do duelo pode vencer e recrutar
        if (isDuelMode() && this.duelTarget instanceof Player duelist && pSource.getEntity() == duelist) {
            if (this.getHealth() - pAmount <= 1.0F) {
                this.setHealth(1.0F);
                this.setDuelMode(false);
                this.duelTarget = null;
                this.setRecruited(true);
                this.applyRecruitDefaults();

                this.setOwnerUUID(duelist.getUUID());
                duelist.displayClientMessage(
                        Component.translatable("epicvanguard.text.warrior_companion_entity.6", this.getWarriorName()),
                        false);

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.0D, this.getZ(),
                            20, 0.5D, 0.5D, 0.5D, 0.1D);
                }
                return false;
            }
        }

        boolean hurtResult = super.hurt(pSource, pAmount);
        if (hurtResult && pAmount > 0.0F) {
            this.lastDamageTick = this.tickCount; // regeneração natural espera 5 s depois de qualquer dano
        }

        // Se após o dano a vida cair para <= 30% e estiver vivo, executa rolamento de emergência
        if (hurtResult && this.isAlive() && !isDuelMode() && this.getHealth() <= (this.getMaxHealth() * 0.30F)) {
            LivingEntity attacker = pSource.getEntity() instanceof LivingEntity living ? living : this.getTarget();
            performEmergencyDodgeRoll(attacker != null ? attacker.position() : null);
        }

        return hurtResult;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float multiplier = (getSpecialization() == SPEC_BERSERKER) ? 0.90F : 0.75F;
        damage = Math.max(1.0F, damage * multiplier);

        boolean isCrit = false;
        // Berserker Passives:
        if (getSpecialization() == SPEC_BERSERKER) {
            // 1. Fúria Sangrenta com vida < 40%: +20% dano extra (config)
            if (this.getHealth() < (this.getMaxHealth() * EpicVanguardConfig.berserkerRageThreshold())) {
                damage *= 1.0F + EpicVanguardConfig.berserkerRageBonus();
            }
            // 2. Acertos Críticos: 20% de chance de causar 1.5x de dano (config)
            if (this.random.nextFloat() < EpicVanguardConfig.berserkerCritChance()) {
                damage *= EpicVanguardConfig.berserkerCritMultiplier();
                isCrit = true;
            }
        }

        // Encantamentos da arma, como no Mob#doHurtTarget vanilla: Afiação/Julgamento/Ruína, Repulsão e Aspecto Flamejante
        ItemStack weapon = this.getMainHandItem();
        float knockback = (float) this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        if (target instanceof LivingEntity livingTarget) {
            damage += net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(weapon, livingTarget.getMobType());
            knockback += (float) net.minecraft.world.item.enchantment.EnchantmentHelper.getKnockbackBonus(this);
        }
        int fireAspect = net.minecraft.world.item.enchantment.EnchantmentHelper.getFireAspect(this);
        if (fireAspect > 0) {
            target.setSecondsOnFire(fireAspect * 4);
        }

        int spec = getSpecialization();
        if (hasPerk(WarriorPerk.SPEC_T1)) {
            if (spec == SPEC_NONE) {
                damage *= 1.10F; // Veterano
            } else if (spec == SPEC_DUELIST && this.counterReady) {
                damage *= 1.50F; // Contra-ataque
                isCrit = true;
            }
        }
        this.counterReady = false;

        DamageSource source = this.damageSources().mobAttack(this);
        boolean success = target.hurt(source, damage);
        if (success && spec == SPEC_BERSERKER && hasPerk(WarriorPerk.SPEC_T1)) {
            this.heal(damage * 0.15F); // Sede de Sangue: cura 15% do dano causado
        }
        if (success) {
            if (knockback > 0.0F && target instanceof LivingEntity livingTarget) {
                float yawRad = this.getYRot() * ((float) Math.PI / 180F);
                livingTarget.knockback(knockback * 0.5F, Mth.sin(yawRad), -Mth.cos(yawRad));
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
            }
            this.doEnchantDamageEffects(this, target);
            this.setLastHurtMob(target);
            // Desgasta a arma como no jogador (espada -1, outras ferramentas -2), com config, talento,
            // aviso de durabilidade baixa e troca automática pela reserva da mochila
            if (!weapon.isEmpty() && weapon.isDamageableItem()) {
                WarriorEquipmentWear.damageMainWeapon(this, weapon.getItem() instanceof net.minecraft.world.item.SwordItem ? 1 : 2);
            }
        }
        if (success && isCrit && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.8D, target.getZ(),
                    12, 0.3D, 0.4D, 0.3D, 0.15D);
            this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2F, 1.1F);
        }
        return success;
    }

    @Override
    public void die(DamageSource pDamageSource) {
        super.die(pDamageSource);
        // Morte cancelada (talento Segundo Fôlego ou outro mod via LivingDeathEvent): continua vivo
        if (this.getHealth() > 0.0F) return;
        if (!this.level().isClientSide) {
            if (this.getServer() != null) {
                CompanionSavedData.get(this.getServer()).retire(this.getUUID());
                notifyOwnerOfDeath();
            }
            dropAllInventory(); // reserva: os itens já saíram em dropEquipment; aqui só se algo sobrou
        }
    }

    /** Avisa o dono (em qualquer dimensão) que o companheiro morreu, como e onde ficaram os itens. */
    private void notifyOwnerOfDeath() {
        if (!this.isRecruited() || this.getServer() == null) return;
        UUID ownerId = this.getOwnerUUID().orElse(null);
        if (ownerId == null) return;
        net.minecraft.server.level.ServerPlayer owner = this.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return;
        Component cause = this.getCombatTracker().getDeathMessage();
        String dim = this.level().dimension().location().toString();
        owner.sendSystemMessage(Component.translatable("epicvanguard.text.companion_died",
                this.getWarriorName(), cause,
                this.getBlockX(), this.getBlockY(), this.getBlockZ(), dim));
        owner.playNotifySound(SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.4F, 1.4F);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource pSource, int pLooting, boolean pRecentlyHit) {
        super.dropCustomDeathLoot(pSource, pLooting, pRecentlyHit);
    }

    /**
     * Itens da mochila e do equipamento na morte. Roda dentro da captura de drops do Forge (dropAllDeathLoot), então
     * passam pelo LivingDropsEvent: mods de lápide (Gravestone, Corail Tombstone) e de keep-inventory enxergam tudo.
     * Diferente do dropCustomDeathLoot, roda mesmo com a gamerule doMobLoot desligada.
     */
    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (this.level().isClientSide) return;
        for (int i = 0; i < warriorInventory.getContainerSize(); i++) {
            ItemStack stack = warriorInventory.getItem(i);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack.copy(), 0.5F);
                warriorInventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * Os slots de equipamento só espelham o WarriorInventory, que é dropado inteiro na morte. O zero do construtor
     * não basta: o Mob vanilla lê ArmorDropChances/HandDropChances do NBT, e saves antigos têm a chance padrão (8,5%),
     * o que duplicava a peça. Aqui a chance é sempre zero, venha de onde vier.
     */
    @Override
    protected float getEquipmentDropChance(EquipmentSlot pSlot) {
        return 0.0F;
    }

    /** Desgaste da armadura ao levar dano, como o do jogador ({@code Inventory#hurtArmor}). O Mob vanilla não desgasta. */
    @Override
    protected void hurtArmor(DamageSource pDamageSource, float pDamageAmount) {
        WarriorEquipmentWear.damageArmor(this, pDamageSource, pDamageAmount);
    }

    /** Bigorna ou estalactite caindo na cabeça: só o capacete. */
    @Override
    protected void hurtHelmet(DamageSource pDamageSource, float pDamageAmount) {
        WarriorEquipmentWear.damageHelmet(this, pDamageAmount);
    }

    /** Escudo ao aparar golpes (ActiveShieldDefenseGoal / bloqueio vanilla). */
    @Override
    protected void hurtCurrentlyUsedShield(float amount) {
        WarriorEquipmentWear.damageShield(this, amount);
    }

    public void dropAllInventory() {
        if (this.level().isClientSide) return;

        // Dropar todos os 22 slots do inventário (Armaduras, Armas, Slot Arcano e Mochila)
        for (int i = 0; i < warriorInventory.getContainerSize(); i++) {
            ItemStack stack = warriorInventory.getItem(i);
            if (!stack.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(
                        this.level(),
                        this.getX(),
                        this.getY() + 0.5D,
                        this.getZ(),
                        stack.copy()
                );
                itemEntity.setDefaultPickUpDelay();
                itemEntity.setDeltaMovement(
                        (this.random.nextDouble() - 0.5D) * 0.25D,
                        0.25D + this.random.nextDouble() * 0.1D,
                        (this.random.nextDouble() - 0.5D) * 0.25D
                );
                this.level().addFreshEntity(itemEntity);
                warriorInventory.setItem(i, ItemStack.EMPTY);
            }
        }

        // Limpar os slots de exibição visual
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean isAlliedTo(Entity pEntity) {
        if (isRecruited() && !isDuelMode() && getOwnerUUID().isPresent()) {
            if (pEntity.getUUID().equals(getOwnerUUID().get())) {
                return true;
            }
            if (pEntity instanceof WarriorCompanionEntity otherWarrior && otherWarrior.isRecruited() && otherWarrior.getOwnerUUID().isPresent()) {
                if (otherWarrior.getOwnerUUID().get().equals(getOwnerUUID().get())) {
                    return true;
                }
            }
        }
        return super.isAlliedTo(pEntity);
    }

    // ── Player Interaction ────────────────────────────────────────────────────
    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        InteractionResult result = WarriorInteraction.interact(this, pPlayer, pHand);
        return result != null ? result : super.mobInteract(pPlayer, pHand);
    }

    // ── Save & Load NBT ───────────────────────────────────────────────────────
    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        getOwnerUUID().ifPresent(uuid -> pCompound.putUUID("OwnerUUID", uuid));
        pCompound.putInt("SkinId", getSkinId());
        pCompound.putBoolean("TargetHostiles", isTargetHostiles());
        pCompound.putBoolean("TargetPassives", isTargetPassives());
        pCompound.putInt("PickupMode", getPickupMode());
        pCompound.putInt("CombatMode", getCombatMode());
        pCompound.putBoolean("Recruited", isRecruited());
        pCompound.putString("WarriorName", getWarriorName());
        pCompound.putBoolean("DuelMode", isDuelMode());
        pCompound.putBoolean("QuestStarted", isQuestStarted());
        pCompound.putInt("QuestType", getQuestType());
        pCompound.putInt("RecruitCost", getRecruitCost());
        pCompound.putInt("EquipmentTier", getEquipmentTier());
        pCompound.putBoolean("Prisoner", isPrisoner());
        pCompound.putInt("Level", getWarriorLevel());
        pCompound.putInt("Experience", getWarriorExperience());
        pCompound.putInt("Specialization", getSpecialization());
        pCompound.putInt("WarcryCooldown", getWarcryCooldown());
        pCompound.putInt("Personality", getPersonalityId());
        pCompound.putInt("Perks", getPerks());
        pCompound.putInt("SecondWindCooldown", secondWindCooldown);
        if (mountUUID != null) pCompound.putUUID("MountUUID", mountUUID);

        if (guardPos != null) {
            pCompound.putInt("GuardX", guardPos.getX());
            pCompound.putInt("GuardY", guardPos.getY());
            pCompound.putInt("GuardZ", guardPos.getZ());
        }

        CompoundTag invTag = new CompoundTag();
        warriorInventory.saveToNBT(invTag);
        pCompound.put("WarriorInventory", invTag);
        pCompound.putInt("RestoreGen", restoreGeneration);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.restoreGeneration = pCompound.getInt("RestoreGen");
        if (pCompound.hasUUID("OwnerUUID")) {
            setOwnerUUID(pCompound.getUUID("OwnerUUID"));
        }
        if (pCompound.contains("SkinId")) {
            setSkinId(pCompound.getInt("SkinId"));
        }
        if (pCompound.contains("TargetHostiles")) {
            setTargetHostiles(pCompound.getBoolean("TargetHostiles"));
        }
        if (pCompound.contains("TargetPassives")) {
            setTargetPassives(pCompound.getBoolean("TargetPassives"));
        }
        if (pCompound.contains("PickupMode")) {
            setPickupMode(pCompound.getInt("PickupMode"));
        } else if (pCompound.contains("PickupItems")) {
            // 1.0.18 só tinha liga/desliga: "ligado" era pegar tudo
            setPickupMode(pCompound.getBoolean("PickupItems") ? PICKUP_ALL : PICKUP_OFF);
        }
        if (pCompound.contains("CombatMode")) {
            setCombatMode(pCompound.getInt("CombatMode"));
        }
        if (pCompound.contains("WarriorName")) {
            setWarriorName(pCompound.getString("WarriorName"));
        } else if (this.hasCustomName()) {
            String raw = this.getCustomName() != null ? this.getCustomName().getString() : "";
            String clean = raw.replaceAll("§[0-9a-fk-or]", "").trim();
            if (!clean.isEmpty()) {
                setWarriorName(clean);
            }
        }
        if (pCompound.contains("Recruited")) {
            setRecruited(pCompound.getBoolean("Recruited"));
        } else {
            setRecruited(false);
        }
        if (!this.isRecruited() && isDefaultName(this.entityData.get(WARRIOR_NAME))) {
            String name = WARRIOR_NAMES[this.random.nextInt(WARRIOR_NAMES.length)];
            this.setWarriorName(name);
        }
        if (pCompound.contains("DuelMode")) {
            setDuelMode(pCompound.getBoolean("DuelMode"));
        }
        if (pCompound.contains("RecruitCost")) {
            setRecruitCost(pCompound.getInt("RecruitCost"));
        }
        if (pCompound.contains("EquipmentTier")) {
            setEquipmentTier(pCompound.getInt("EquipmentTier"));
        }
        if (pCompound.contains("Prisoner")) {
            setPrisoner(pCompound.getBoolean("Prisoner"));
        }
        if (pCompound.contains("Level")) {
            setWarriorLevel(pCompound.getInt("Level"));
        }
        if (pCompound.contains("Experience")) {
            setWarriorExperience(pCompound.getInt("Experience"));
        }
        if (pCompound.contains("Specialization")) {
            setSpecialization(pCompound.getInt("Specialization"));
        }
        if (pCompound.contains("WarcryCooldown")) {
            setWarcryCooldown(pCompound.getInt("WarcryCooldown"));
        }
        if (pCompound.contains("Personality")) {
            setPersonalityId(pCompound.getInt("Personality"));
        }
        this.entityData.set(PERKS, WarriorPerk.sanitize(pCompound.getInt("Perks")));
        this.secondWindCooldown = pCompound.getInt("SecondWindCooldown");
        this.mountUUID = pCompound.hasUUID("MountUUID") ? pCompound.getUUID("MountUUID") : null;
        recalculateAttributes();
        if (pCompound.contains("QuestStarted")) {
            setQuestStarted(pCompound.getBoolean("QuestStarted"));
        }
        if (pCompound.contains("QuestType")) {
            setQuestType(pCompound.getInt("QuestType"));
        }
        if (pCompound.contains("GuardX")) {
            guardPos = new BlockPos(pCompound.getInt("GuardX"), pCompound.getInt("GuardY"), pCompound.getInt("GuardZ"));
        }
        if (pCompound.contains("WarriorInventory")) {
            warriorInventory.loadFromNBT(pCompound.getCompound("WarriorInventory"));
        }
    }

    // ── Persistence & Despawn Prevention ─────────────────────────────────────
    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        // Companheiros NUNCA despawnam por distância do jogador
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // ── Intelligent Safe Teleport ─────────────────────────────────────────────
    /**
     * Move o guerreiro para outra dimensão, como o {@code Entity#teleportTo(ServerLevel, ...)} do vanilla,
     * mas devolve a entidade nova. Quando muda de dimensão, {@code this} é removido e não deve mais ser usado.
     */
    public WarriorCompanionEntity moveToLevel(ServerLevel targetLevel, double x, double y, double z) {
        if (this.level() == targetLevel) {
            this.teleportTo(x, y, z);
            return this;
        }
        if (this.isRemoved()) return this;
        this.unRide();
        Entity created = this.getType().create(targetLevel);
        if (!(created instanceof WarriorCompanionEntity copy)) return this;
        copy.restoreFrom(this);
        copy.moveTo(x, y, z, this.getYRot(), this.getXRot());
        copy.setYHeadRot(this.getYRot());
        copy.setDeltaMovement(Vec3.ZERO);
        this.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
        targetLevel.addDuringTeleport(copy);
        return copy;
    }

    /** Teleporta com segurança para perto do alvo (inclusive entre dimensões); devolve a entidade que ficou ao lado dele. */
    public WarriorCompanionEntity safeTeleportTo(Entity target) {
        return WarriorTeleport.safeTeleportTo(this, target);
    }

    public void safeTeleportTo(BlockPos targetPos) {
        WarriorTeleport.safeTeleportTo(this, targetPos);
    }




    // ── Sobrevivência Autônoma & Consumíveis ───────────────────────────────────
    public static boolean isHealingPotion(ItemStack stack) {
        return WarriorHealingHelper.isHealingPotion(stack);
    }

    public int findBestConsumableSlot(boolean inEmergency) {
        return WarriorHealingHelper.findBestConsumableSlot(this, inEmergency);
    }

    public int getConsumableHealingScore(ItemStack stack, boolean inEmergency) {
        return WarriorHealingHelper.getConsumableHealingScore(this, stack, inEmergency);
    }

    @Override
    protected void completeUsingItem() {
        this.stopUsingItem();
        this.syncEquipmentWithInventory();
    }

    public void consumeHealingItem(int slotIndex) {
        WarriorHealingHelper.consumeHealingItem(this, slotIndex);
    }
}
