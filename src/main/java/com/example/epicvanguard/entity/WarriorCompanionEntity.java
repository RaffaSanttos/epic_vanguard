package com.example.epicvanguard.entity;

import com.example.epicvanguard.init.ModBlocks;
import com.example.epicvanguard.init.ModItems;
import com.example.epicvanguard.init.ModPoiTypes;
import com.example.epicvanguard.inventory.WarriorInventory;
import com.example.epicvanguard.networking.Messages;
import com.example.epicvanguard.networking.packet.PacketOpenWarriorGUI;
import com.example.epicvanguard.networking.packet.PacketRecruitWarrior;
import com.example.epicvanguard.dialogue.PersonalityArchetype;
import com.example.epicvanguard.dialogue.SpeechTrigger;
import com.example.epicvanguard.dialogue.WarriorSpeechSystem;
import com.example.epicvanguard.entity.ai.*;
import com.example.epicvanguard.entity.util.WarriorCombatHelper;
import com.example.epicvanguard.entity.util.WarriorHealingHelper;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ShieldItem;
import com.example.epicvanguard.screen.HonorContractMenu;
import com.example.epicvanguard.screen.WarriorCompanionMenu;
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
import net.minecraft.world.item.ShieldItem;
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
    public static final String[] WARRIOR_NAMES = {
            "Aldric", "Bram", "Caelan", "Dorian", "Eudes", "Faolan", "Gareth", "Hadwin",
            "Jorik", "Kael", "Lyam", "Maren", "Nolann", "Oryn", "Phelan", "Revan",
            "Sorin", "Theron", "Ulric", "Veron", "Wulf", "Zarek"
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

    // ── Constantes de Especialização ──────────────────────────────────────────
    public static final int SPEC_NONE = 0;
    public static final int SPEC_BERSERKER = 1;
    public static final int SPEC_GUARDIAN = 2;
    public static final int SPEC_DUELIST = 3;

    private final WarriorInventory warriorInventory = new WarriorInventory();
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
            default -> 0.30F;
        };
        return this.getHealth() <= (this.getMaxHealth() * threshold);
    }

    public boolean isEmergencyRetreating() {
        return this.emergencyRetreatCooldown > 0;
    }

    public void setEmergencyRetreatCooldown(int ticks) {
        this.emergencyRetreatCooldown = ticks;
    }

    @Override
    public void setTarget(@javax.annotation.Nullable LivingEntity target) {
        if (this.isEmergencyRetreating() || this.isLowHealth()) {
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
        List<Creeper> creepers = this.level().getEntitiesOfClass(
                Creeper.class,
                this.getBoundingBox().inflate(radius),
                c -> c.isAlive() && (c.isIgnited() || c.getSwellDir() > 0)
        );
        return creepers.isEmpty() ? null : creepers.get(0);
    }

    public boolean isThreatenedByProjectiles(double radius) {
        List<Projectile> projectiles = this.level().getEntitiesOfClass(
                Projectile.class,
                this.getBoundingBox().inflate(radius),
                p -> p.isAlive() && p.getDeltaMovement().lengthSqr() > 0.05D
        );
        for (Projectile p : projectiles) {
            Vec3 projMotion = p.getDeltaMovement().normalize();
            Vec3 toWarrior = this.position().subtract(p.position()).normalize();
            if (projMotion.dot(toWarrior) > 0.65D) {
                return true;
            }
            Player owner = getOwner();
            if (owner != null && this.distanceToSqr(owner) < 64.0D) {
                Vec3 toOwner = owner.position().subtract(p.position()).normalize();
                if (projMotion.dot(toOwner) > 0.65D) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public Entity getIncomingProjectileThreat(double radius) {
        List<Projectile> projectiles = this.level().getEntitiesOfClass(
                Projectile.class,
                this.getBoundingBox().inflate(radius),
                p -> p.isAlive() && p.getDeltaMovement().lengthSqr() > 0.05D
        );
        for (Projectile p : projectiles) {
            Vec3 projMotion = p.getDeltaMovement().normalize();
            Vec3 toWarrior = this.position().subtract(p.position()).normalize();
            if (projMotion.dot(toWarrior) > 0.65D) {
                return p;
            }
        }
        return null;
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

    private Player inventoryOpenPlayer = null;

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

        if (!pLevel.isClientSide && this.getWarriorName().isEmpty()) {
            String name = WARRIOR_NAMES[this.random.nextInt(WARRIOR_NAMES.length)];
            this.setWarriorName(name);
            this.setSkinId(this.random.nextInt(SKIN_COUNT));
            this.setPersonalityId(this.random.nextInt(PersonalityArchetype.values().length));
            this.setRecruitCost(35 + this.random.nextInt(11)); // 35 a 45 Peças de Ouro (Média ~40)
            this.setCustomName(Component.literal("§7" + name));
            this.setCustomNameVisible(true);
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
        return this.getOwnerUUID().map(this.level()::getPlayerByUUID).orElse(null);
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
    }

    public void clearCombatTarget() {
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
        String prefix = recruited ? "§a" : "§7";
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
        return "Guerreiro";
    }

    public void setWarriorName(String name) {
        this.entityData.set(WARRIOR_NAME, name);
        String prefix = this.isRecruited() ? "§a" : "§7";
        super.setCustomName(Component.literal(prefix + name));
        this.setCustomNameVisible(true);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        if (name != null) {
            String clean = name.getString().replaceAll("§[0-9a-fk-or]", "").trim();
            if (!clean.isEmpty() && !clean.equals(this.entityData.get(WARRIOR_NAME))) {
                this.entityData.set(WARRIOR_NAME, clean);
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
                owner.sendSystemMessage(Component.literal("§6✦ [Vanguarda] §e§l" + getWarriorName() + " §asubiu para o §e§lNível " + currentLvl + "§a! Seus atributos aumentaram!"));
            }
        }
    }

    public float getCalculatedAttackDamage() {
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float multiplier = (getSpecialization() == SPEC_BERSERKER) ? 0.90F : 0.75F;
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

    public String getSpecializationName() {
        return switch (getSpecialization()) {
            case SPEC_BERSERKER -> "Berserker";
            case SPEC_GUARDIAN  -> "Guardião";
            case SPEC_DUELIST   -> "Duelista";
            default             -> "Guerreiro";
        };
    }

    public String getFormattedSpecializationTitle() {
        return switch (getSpecialization()) {
            case SPEC_BERSERKER -> "§c⚔ Berserker";
            case SPEC_GUARDIAN  -> "§9🛡 Guardião";
            case SPEC_DUELIST   -> "§b🗡 Duelista";
            default             -> "§7Guerreiro";
        };
    }

    public void triggerWarcry() {
        if (this.level().isClientSide) return;
        this.setWarcryCooldown(1000); // 50 segundos de recarga

        switch (getSpecialization()) {
            case SPEC_BERSERKER -> executeBerserkerWarcry();
            case SPEC_GUARDIAN  -> executeGuardianWarcry();
            case SPEC_DUELIST   -> executeDuelistWarcry();
            default             -> executeBaseWarcry();
        }
    }

    private void spawnParticleRing(ServerLevel serverLevel, ParticleOptions particle, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            double px = this.getX() + Math.cos(angle) * radius;
            double pz = this.getZ() + Math.sin(angle) * radius;
            serverLevel.sendParticles(particle, px, this.getY() + 0.2D, pz, 1, 0.0D, 0.1D, 0.0D, 0.02D);
        }
    }

    private void executeBerserkerWarcry() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.5F, 1.2F);
        spawnParticleRing(serverLevel, ParticleTypes.FLAME, 3.0D, 24);
        spawnParticleRing(serverLevel, ParticleTypes.ANGRY_VILLAGER, 2.0D, 12);

        // 1. Monstros em 10 blocos ganham Fraqueza I por 10s (200 ticks)
        List<Monster> enemies = serverLevel.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(10.0D), LivingEntity::isAlive);
        for (Monster enemy : enemies) {
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, true));
        }

        // 2. Berserker ganha Força II por 10s
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, true));

        // 3. Jogador e equipe ganham Força I por 10s
        Player owner = getOwner();
        if (owner != null && owner.distanceToSqr(this) <= 256.0D) {
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, false, true));
            owner.displayClientMessage(
                    Component.literal("§c⚔ [Berserker] " + this.getWarriorName() + " rugiu em fúria! O Grito Feroz de Sangue ecoa! (Força I concedida)"),
                    false);
        }
    }

    private void executeGuardianWarcry() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2F, 0.8F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.3F, 0.7F);
        spawnParticleRing(serverLevel, ParticleTypes.FLASH, 4.0D, 16);
        spawnParticleRing(serverLevel, ParticleTypes.ENCHANTED_HIT, 3.0D, 32);

        // 1. Taunt absoluto (100%) em monstros num raio de 12 blocos
        List<Monster> enemies = serverLevel.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(12.0D), LivingEntity::isAlive);
        for (Monster enemy : enemies) {
            enemy.setTarget(this);
        }

        // 2. Guardião ganha Resistência II por 15s (300 ticks)
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1, false, true));

        // 3. Jogador e equipe ganham Força I e Resistência I por 15s
        Player owner = getOwner();
        if (owner != null && owner.distanceToSqr(this) <= 324.0D) {
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0, false, true));
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0, false, true));
            owner.displayClientMessage(
                    Component.literal("§9🛡 [Guardião] " + this.getWarriorName() + " bateu seu escudo! O Rugido do Bastião ecoa! (Monstros provocados & Resistência I)"),
                    false);
        }
    }

    private void executeDuelistWarcry() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5F, 0.5F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6F, 1.8F);
        spawnParticleRing(serverLevel, ParticleTypes.SWEEP_ATTACK, 2.5D, 16);
        spawnParticleRing(serverLevel, ParticleTypes.CLOUD, 3.5D, 24);

        // 1. Desestabiliza inimigos em 6 blocos com quebra de postura breve (Lentidão IV / atordoamento por 1.5s)
        List<Monster> enemies = serverLevel.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(6.0D), LivingEntity::isAlive);
        for (Monster enemy : enemies) {
            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3, false, true));
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, true));
        }

        // 2. Jogador e equipe ganham Força I e Velocidade II (+40%) por 12s (240 ticks)
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 1, false, true));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 240, 0, false, true));

        Player owner = getOwner();
        if (owner != null && owner.distanceToSqr(this) <= 256.0D) {
            owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 1, false, true));
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 240, 0, false, true));
            owner.displayClientMessage(
                    Component.literal("§b🗡 [Duelista] " + this.getWarriorName() + " disparou o Brado da Tempestade! (Velocidade II e Força I)"),
                    false);
        }
    }

    private void executeBaseWarcry() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 1.0F);
        spawnParticleRing(serverLevel, ParticleTypes.CRIT, 2.5D, 16);

        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0, false, true));
        Player owner = getOwner();
        if (owner != null && owner.distanceToSqr(this) <= 256.0D) {
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0, false, true));
            owner.displayClientMessage(
                    Component.literal("§7[Guerreiro] " + this.getWarriorName() + " rugiu em batalha! (Força I concedida)"),
                    false);
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
        var attrHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (attrHealth != null) {
            double oldMax = attrHealth.getBaseValue();
            attrHealth.setBaseValue(baseMaxHealth);
            if (baseMaxHealth > oldMax && this.isAlive()) {
                this.heal((float)(baseMaxHealth - oldMax));
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
        }
        var attrSpeed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attrSpeed != null) {
            attrSpeed.setBaseValue(speed);
        }

        // 6. Resistência a Repulsão: 0.0 (0.40D se Guardião)
        double knockbackRes = (spec == SPEC_GUARDIAN) ? 0.40D : 0.0D;
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
        this.goalSelector.addGoal(2, new DefendOwnerGoal(this));
        this.goalSelector.addGoal(3, new WarriorTacticalCombatGoal(this, 1.25D));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.15D, 5.0F, 2.0F));
        this.goalSelector.addGoal(4, new GuardRadiusGoal(this, 1.1D));
        this.goalSelector.addGoal(5, new AutoFeedGoal(this));
        this.goalSelector.addGoal(6, new CampfireRelaxGoal(this));
        this.goalSelector.addGoal(6, new VanguardBasePatrolGoal(this));
        this.goalSelector.addGoal(6, new TavernRelaxGoal(this, 0.9D));
        this.goalSelector.addGoal(7, new WarriorStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new AggressiveTargetGoal(this));
        this.targetSelector.addGoal(3, new HuntAnimalGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        SpawnGroupData data = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
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
                this.setRecruitCost(20 + this.random.nextInt(11)); // 20 a 30 moedas
                break;
            case 1: // 30% Cota de Malha / Pedra -> Nível 2
                this.setWarriorLevel(2);
                warriorInventory.setItem(WarriorInventory.SLOT_HELMET, new ItemStack(Items.CHAINMAIL_HELMET));
                warriorInventory.setItem(WarriorInventory.SLOT_CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                warriorInventory.setItem(WarriorInventory.SLOT_LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
                warriorInventory.setItem(WarriorInventory.SLOT_BOOTS, new ItemStack(Items.CHAINMAIL_BOOTS));
                warriorInventory.setItem(WarriorInventory.SLOT_WEAPON_MAIN, new ItemStack(this.random.nextBoolean() ? Items.STONE_SWORD : Items.STONE_AXE));
                warriorInventory.setItem(WarriorInventory.SLOT_WEAPON_OFF, new ItemStack(Items.SHIELD));
                this.setRecruitCost(40 + this.random.nextInt(11)); // 40 a 50 moedas
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
                this.setRecruitCost(60 + this.random.nextInt(21)); // 60 a 80 moedas
                break;
        }
        this.syncEquipmentWithInventory();
        this.recalculateAttributes();
    }

    public static int rollRandomTier(net.minecraft.util.RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 60) return 0; // 60%
        if (roll < 90) return 1; // 30%
        return 2; // 10%
    }

    // ── Tick & Equipment Sync ─────────────────────────────────────────────────
    @Override
    public void tick() {
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

            // Sync Warrior Inventory equipment to entity equipment slots for rendering
            syncEquipmentWithInventory();

            // Follow Owner distance safeguard (modo 0 = Seguir: teleporta ao atingir 15 blocos de distância)
            if (isRecruited() && getCombatMode() == 0 && !isEmergencyRetreating()) {
                Player owner = getOwner();
                if (owner != null && !owner.isSpectator() && this.distanceToSqr(owner) >= 225.0D) { // >= 15 blocks
                    this.setTarget(null);
                    this.safeTeleportTo(owner);
                }
            }

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

            // Update CompanionSavedData periodically
            if (isRecruited() && this.tickCount % 40 == 0 && this.getServer() != null && getOwnerUUID().isPresent()) {
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
                        this.getSpecialization()
                );
            }
        }
    }

    private void pickUpGroundItems() {
        if (!this.isAlive() || this.level().isClientSide() || !this.isRecruited() || this.isPrisoner()) {
            return;
        }

        AABB pickupBox = this.getBoundingBox().inflate(1.0D, 0.5D, 1.0D);
        List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class, pickupBox, e -> !e.isRemoved() && !e.hasPickUpDelay());

        for (ItemEntity itemEntity : items) {
            ItemStack groundStack = itemEntity.getItem();
            if (groundStack.isEmpty()) continue;

            int originalCount = groundStack.getCount();
            ItemStack remaining = this.warriorInventory.addItemToBackpack(groundStack);
            int pickedUpCount = originalCount - remaining.getCount();

            if (pickedUpCount > 0) {
                this.take(itemEntity, pickedUpCount);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL,
                        0.2F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);

                if (remaining.isEmpty()) {
                    itemEntity.discard();
                } else {
                    itemEntity.setItem(remaining);
                }
            }
        }
    }

    public void syncEquipmentWithInventory() {
        this.setItemSlot(EquipmentSlot.HEAD, warriorInventory.getItem(WarriorInventory.SLOT_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, warriorInventory.getItem(WarriorInventory.SLOT_CHEST));
        this.setItemSlot(EquipmentSlot.LEGS, warriorInventory.getItem(WarriorInventory.SLOT_LEGS));
        this.setItemSlot(EquipmentSlot.FEET, warriorInventory.getItem(WarriorInventory.SLOT_BOOTS));
        if (!this.isUsingItem()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, warriorInventory.getItem(WarriorInventory.SLOT_WEAPON_MAIN));
            this.setItemSlot(EquipmentSlot.OFFHAND, warriorInventory.getItem(WarriorInventory.SLOT_WEAPON_OFF));
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

        public static boolean isSpellOrMagicDamage(DamageSource source) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR) ||
            source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_EXPLOSION) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT)) {
            return true;
        }
        return false;
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
        if (isDuelMode()) {
            if (this.getHealth() - pAmount <= 1.0F) {
                this.setHealth(1.0F);
                this.setDuelMode(false);
                this.setTarget(null);
                this.duelTarget = null;
                this.setRecruited(true);

                if (pSource.getEntity() instanceof Player player) {
                    this.setOwnerUUID(player.getUUID());
                    player.displayClientMessage(
                            Component.literal("§6" + this.getWarriorName() + "§a se rende e jura lealdade a você!"),
                            false);
                }

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.0D, this.getZ(),
                            20, 0.5D, 0.5D, 0.5D, 0.1D);
                }
                return false;
            }
        }

        boolean hurtResult = super.hurt(pSource, pAmount);

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
            // 1. Fúria Sangrenta com vida < 40%: +20% dano extra
            if (this.getHealth() < (this.getMaxHealth() * 0.40F)) {
                damage *= 1.20F;
            }
            // 2. Acertos Críticos: 20% de chance de causar 1.5x de dano
            if (this.random.nextFloat() < 0.20F) {
                damage *= 1.50F;
                isCrit = true;
            }
        }

        DamageSource source = this.damageSources().mobAttack(this);
        boolean success = target.hurt(source, damage);
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
        if (!this.level().isClientSide) {
            if (this.getServer() != null) {
                CompanionSavedData.get(this.getServer()).unregister(this.getUUID());
            }
            dropAllInventory();
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource pSource, int pLooting, boolean pRecentlyHit) {
        super.dropCustomDeathLoot(pSource, pLooting, pRecentlyHit);
        dropAllInventory();
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
        if (pHand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ServerPlayer serverPlayer = (ServerPlayer) pPlayer;
        this.startTalkingWith(serverPlayer, 160);

        if (this.isPrisoner()) {
            ItemStack heldItem = pPlayer.getItemInHand(pHand);
            boolean isFood = heldItem.isEdible();
            boolean isPotion = heldItem.getItem() instanceof PotionItem;
            boolean isWeapon = heldItem.getItem() instanceof SwordItem || heldItem.getItem() instanceof AxeItem;

            if (isFood || isPotion || isWeapon) {
                if (!pPlayer.isCreative()) {
                    if (isWeapon) {
                        this.getWarriorInventory().setItem(WarriorInventory.SLOT_WEAPON_MAIN, heldItem.copy());
                        heldItem.shrink(1);
                    } else {
                        heldItem.shrink(1);
                    }
                } else if (isWeapon) {
                    this.getWarriorInventory().setItem(WarriorInventory.SLOT_WEAPON_MAIN, heldItem.copy());
                }

                // Resgate com sucesso: cura vida, remove fraqueza, assina contrato de honra de graça!
                this.setPrisoner(false);
                this.removeEffect(MobEffects.WEAKNESS);
                this.setHealth(this.getMaxHealth());
                this.setOwnerUUID(pPlayer.getUUID());
                this.setRecruited(true);
                this.setCombatMode(1); // Defensivo
                this.setEquipmentTier(0);
                this.syncEquipmentWithInventory();

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            this.getX(), this.getY() + 1.0D, this.getZ(),
                            25, 0.5D, 0.5D, 0.5D, 0.05D);
                    serverLevel.sendParticles(ParticleTypes.HEART,
                            this.getX(), this.getY() + 1.2D, this.getZ(),
                            10, 0.4D, 0.4D, 0.4D, 0.05D);
                    serverLevel.playSound(null, this.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                }

                pPlayer.sendSystemMessage(Component.literal("§6[" + this.getWarriorName() + "] §aVocê salvou a minha vida! Juro lealdade absoluta à sua espada!"));
                return InteractionResult.SUCCESS;
            } else {
                pPlayer.sendSystemMessage(Component.literal("§c" + this.getWarriorName() + " está fraco e ferido! Entregue comida/poção para curá-lo ou uma arma para resgatá-lo."));
                return InteractionResult.SUCCESS;
            }
        }

        if (!isRecruited()) {
            // Abre a tela do Contrato de Honra para contratação manual pelo botão
            this.setInventoryOpenPlayer(serverPlayer);
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inv, p) -> new HonorContractMenu(id, inv, this.getId()),
                    Component.literal("Contrato de Honra")
            ), buf -> buf.writeInt(this.getId()));
            return InteractionResult.SUCCESS;
        } else {
            // Check if player is the owner
            if (this.isOwner(pPlayer)) {
                // Open Warrior Companion Menu directly on click
                this.setInventoryOpenPlayer(serverPlayer);
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (id, inv, p) -> new WarriorCompanionMenu(id, inv, this.getId()),
                        Component.literal("Companheiro - " + this.getWarriorName())
                ), buf -> buf.writeInt(this.getId()));
                return InteractionResult.SUCCESS;
            }
        }

        return super.mobInteract(pPlayer, pHand);
    }

    // ── Save & Load NBT ───────────────────────────────────────────────────────
    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        getOwnerUUID().ifPresent(uuid -> pCompound.putUUID("OwnerUUID", uuid));
        pCompound.putInt("SkinId", getSkinId());
        pCompound.putBoolean("TargetHostiles", isTargetHostiles());
        pCompound.putBoolean("TargetPassives", isTargetPassives());
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

        if (guardPos != null) {
            pCompound.putInt("GuardX", guardPos.getX());
            pCompound.putInt("GuardY", guardPos.getY());
            pCompound.putInt("GuardZ", guardPos.getZ());
        }

        CompoundTag invTag = new CompoundTag();
        warriorInventory.saveToNBT(invTag);
        pCompound.put("WarriorInventory", invTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
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
    public void safeTeleportTo(Entity target) {
        if (target == null) return;
        if (this.level() != target.level()) return;

        // 1. Proteção contra o Void: se o alvo estiver no void ou abaixo da altura mínima, NÃO teletransporta
        if (target.getY() < target.level().getMinBuildHeight()) {
            return;
        }

        // 2. Proteção Absoluta contra Voo / Elytra / Queda Livre:
        // Se o jogador estiver voando, planando, caindo ou NÃO estiver pisando firmemente no chão, NÃO teletransporta!
        if (target instanceof Player player) {
            if (!player.onGround() || player.isFallFlying() || player.getAbilities().flying) {
                return; // Jogador está no ar: guerreiro aguarda com segurança no chão
            }
        } else if (!target.onGround()) {
            return;
        }

        BlockPos targetPos = target.blockPosition();

        // 3. Procura blocos sólidos seguros no mesmo nível do chão do alvo (raio horizontal de 3 blocos, dy entre -1 e +1)
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos candidate = targetPos.offset(dx, dy, dz);
                    if (isSafeTeleportBlock(candidate)) {
                        doTeleport(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
                        return;
                    }
                }
            }
        }

        // 4. Fallback: se o bloco onde o alvo está pisando for seguro
        if (isSafeTeleportBlock(targetPos)) {
            doTeleport(target.getX(), target.getY(), target.getZ());
        }
        // Caso contrário (sem chão seguro), o guerreiro permanece onde está
    }

    public void safeTeleportTo(BlockPos targetPos) {
        if (targetPos == null) return;
        if (targetPos.getY() < this.level().getMinBuildHeight()) return;

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos candidate = targetPos.offset(dx, dy, dz);
                    if (isSafeTeleportBlock(candidate)) {
                        doTeleport(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
                        return;
                    }
                }
            }
        }
        if (isSafeTeleportBlock(targetPos)) {
            doTeleport(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D);
        }
    }

    private boolean isSafeTeleportBlock(BlockPos pos) {
        if (pos.getY() < this.level().getMinBuildHeight()) return false;

        BlockState below = this.level().getBlockState(pos.below());
        BlockState at = this.level().getBlockState(pos);
        BlockState above = this.level().getBlockState(pos.above());

        // O chão abaixo deve ser sólido e não perigoso (não é void, ar, lava, fogo, cacto)
        if (!below.isSolid() && !below.isFaceSturdy(this.level(), pos.below(), net.minecraft.core.Direction.UP)) {
            return false;
        }

        if (below.is(Blocks.LAVA) || below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.CACTUS) || below.is(Blocks.FIRE) || below.isAir()) {
            return false;
        }

        // Os blocos onde o corpo/cabeça vão ficar não podem ser sufocantes nem perigosos
        if (at.isSuffocating(this.level(), pos) || above.isSuffocating(this.level(), pos.above())) {
            return false;
        }

        if (at.is(Blocks.LAVA) || at.is(Blocks.FIRE) || above.is(Blocks.LAVA) || above.is(Blocks.FIRE)) {
            return false;
        }

        return true;
    }

    private void doTeleport(double x, double y, double z) {
        this.teleportTo(x, y, z);
        this.resetFallDistance();
        this.fallDistance = 0.0F; // Reseta inércia e distância de queda acumulada antes do teleporte
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
        this.getNavigation().stop();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 0.5D, this.getZ(),
                    15, 0.3D, 0.5D, 0.3D, 0.1D);
        }
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

    public void consumeHealingItem(ItemStack stack, int slotIndex) {
        WarriorHealingHelper.consumeHealingItem(this, stack, slotIndex);
    }

}
