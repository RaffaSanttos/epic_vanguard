package com.example.epicvanguard.entity;

import com.example.epicvanguard.init.ModItems;
import com.example.epicvanguard.inventory.WarriorInventory;
import com.example.epicvanguard.networking.Messages;
import com.example.epicvanguard.networking.packet.PacketOpenWarriorGUI;
import com.example.epicvanguard.networking.packet.PacketRecruitWarrior;
import com.example.epicvanguard.screen.HonorContractMenu;
import com.example.epicvanguard.screen.WarriorCompanionMenu;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
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

    private int secondRollTimer = 0;
    private Vec3 secondRollDirection = null;

    public boolean isLowHealth() {
        return this.getHealth() <= (this.getMaxHealth() * 0.30F);
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
        super.setTarget(target);
    }

    public void performEmergencyDodgeRoll(@Nullable Vec3 threatPos) {
        if (this.dodgeCooldown > 0) return;
        this.dodgeCooldown = 60; // Cooldown entre sequências de esquiva
        this.emergencyRetreatCooldown = 180; // 9s de recuo prioritário para cura

        Vec3 awayDir;
        if (threatPos != null) {
            awayDir = this.position().subtract(threatPos).multiply(1.0D, 0.0D, 1.0D);
            if (awayDir.lengthSqr() < 1.0E-4D) {
                awayDir = new Vec3(-Math.sin(Math.toRadians(this.getYRot())), 0.0D, Math.cos(Math.toRadians(this.getYRot())));
            } else {
                awayDir = awayDir.normalize();
            }
        } else {
            awayDir = new Vec3(-Math.sin(Math.toRadians(this.getYRot())), 0.0D, Math.cos(Math.toRadians(this.getYRot())));
        }

        // 1º Rolamento Evasivo Imediato
        applyRollImpulse(awayDir);

        // Agenda o 2º Rolamento Evasivo para 10 ticks depois (Duplo Rolamento)
        this.secondRollTimer = 10;
        this.secondRollDirection = awayDir;
    }

    public void applyRollImpulse(Vec3 dir) {
        this.setDeltaMovement(dir.x * 0.42D, 0.16D, dir.z * 0.42D);
        this.hasImpulse = true;

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1.0F, 1.4F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WOOL_FALL, SoundSource.NEUTRAL, 1.0F, 0.9F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.2D, this.getZ(),
                    10, 0.3D, 0.1D, 0.3D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 0.4D, this.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        // Executa animação do Epic Fight
        com.example.epicvanguard.compat.epicfight.EpicFightCompat.playDodgeRollAnimation(this);
    }

    public void startTalkingWith(Player player, int durationTicks) {
        this.talkingPlayer = player;
        this.talkingTicks = durationTicks;
        this.getNavigation().stop();
        this.getLookControl().setLookAt(player, 100.0F, 100.0F);
    }

    public boolean isTalking() {
        return this.talkingTicks > 0 && this.talkingPlayer != null && this.talkingPlayer.isAlive();
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
            this.setRecruitCost(35 + this.random.nextInt(11)); // 35 a 45 Peças de Ouro (Média ~40)
            this.setCustomName(Component.literal("§7" + name));
            this.setCustomNameVisible(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D) // 10 corações (padrão humanoide / jogador)
                .add(Attributes.ARMOR, 0.0D) // Sem armadura base embutida (depende de armaduras equipadas)
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
        this.entityData.define(COMBAT_MODE, 1); // 1=Defensivo (padrão inicial), 0=Agressivo, 2=Guarda, 3=Base
        this.entityData.define(RECRUITED, false);
        this.entityData.define(WARRIOR_NAME, "");
        this.entityData.define(DUEL_MODE, false);
        this.entityData.define(QUEST_STARTED, false);
        this.entityData.define(QUEST_TYPE, 0);
        this.entityData.define(RECRUIT_COST, 40);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
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
        if (mode == 2) {
            this.setGuardPos(this.blockPosition());
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
        if (recruited) {
            this.setCustomName(Component.literal("§a" + this.getWarriorName()));
            this.setCustomNameVisible(true);
        }
    }

    public String getWarriorName() {
        return this.entityData.get(WARRIOR_NAME);
    }

    public void setWarriorName(String name) {
        this.entityData.set(WARRIOR_NAME, name);
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

    // ── AI Goals ──────────────────────────────────────────────────────────────
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new EmergencyRetreatAndEatGoal(this));
        this.goalSelector.addGoal(2, new DefendOwnerGoal(this));
        this.goalSelector.addGoal(3, new WarriorCombatGoal(this, 1.2D, false));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.15D, 5.0F, 2.0F));
        this.goalSelector.addGoal(4, new GuardRadiusGoal(this, 1.1D));
        this.goalSelector.addGoal(5, new AutoFeedGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new AggressiveTargetGoal(this));
    }

    // ── Tick & Equipment Sync ─────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // Talking & Looking at Player Logic
            if (talkingTicks > 0) {
                talkingTicks--;
                if (talkingPlayer != null && talkingPlayer.isAlive() && this.distanceToSqr(talkingPlayer) < 64.0D) {
                    this.getNavigation().stop();
                    this.getLookControl().setLookAt(talkingPlayer, 30.0F, 30.0F);
                } else {
                    talkingPlayer = null;
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

            // Duplo Rolamento Evasivo
            if (secondRollTimer > 0) {
                secondRollTimer--;
                if (secondRollTimer == 0 && secondRollDirection != null) {
                    applyRollImpulse(secondRollDirection);
                    secondRollDirection = null;
                }
            }

            // Sync Warrior Inventory equipment to entity equipment slots for rendering
            syncEquipmentWithInventory();

            // Follow Owner distance safeguard (modes 0 and 1: teleporta ao atingir 15 blocos de distância)
            if (isRecruited() && getCombatMode() != 2 && getCombatMode() != 3 && !isEmergencyRetreating()) {
                Player owner = getOwner();
                if (owner != null && !owner.isSpectator() && this.distanceToSqr(owner) >= 225.0D) { // >= 15 blocks
                    this.setTarget(null);
                    this.safeTeleportTo(owner);
                }
            }

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
                        this.getMaxHealth()
                );
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

    public boolean hurt(DamageSource pSource, float pAmount) {
        // Friendly Fire Check & Immunity
        if (isRecruited() && !isDuelMode() && getOwnerUUID().isPresent()) {
            UUID ownerId = getOwnerUUID().get();
            Entity attacker = pSource.getEntity();
            Entity direct = pSource.getDirectEntity();

            // 1. Companheiras do mesmo dono NUNCA se acertam entre si
            if (attacker instanceof WarriorCompanionEntity otherWarrior && otherWarrior.isRecruited() && otherWarrior.getOwnerUUID().isPresent()) {
                if (otherWarrior.getOwnerUUID().get().equals(ownerId)) {
                    return false;
                }
            }
            if (direct instanceof WarriorCompanionEntity otherWarriorDirect && otherWarriorDirect.isRecruited() && otherWarriorDirect.getOwnerUUID().isPresent()) {
                if (otherWarriorDirect.getOwnerUUID().get().equals(ownerId)) {
                    return false;
                }
            }

            // 2. Ataques originados do Dono
            boolean isFromOwner = (attacker != null && attacker.getUUID().equals(ownerId)) ||
                                  (direct != null && direct.getUUID().equals(ownerId));

            if (isFromOwner && this.getServer() != null) {
                CompanionSavedData.FriendlyFireMode ffMode = CompanionSavedData.get(this.getServer()).getGlobalFriendlyFireMode();
                boolean isMagic = isSpellOrMagicDamage(pSource);

                switch (ffMode) {
                    case DISABLED:
                        // Totalmente livre de fogo amigo: Dono NUNCA causa dano
                        return false;
                    case SPELLS_ONLY:
                        // Apenas magias do dono causam dano (golpes corporais/Epic Fight bloqueados)
                        if (!isMagic) {
                            return false;
                        }
                        break;
                    case MELEE_ONLY:
                        // Apenas golpes fisicos / Epic Fight causam dano (magias bloqueadas)
                        if (isMagic) {
                            return false;
                        }
                        break;
                    case ALL:
                        // Totalmente habilitado: tanto magias quanto golpes acertam
                        break;
                }
            }
        }

        // Shield Block mechanic: 70% chance to block if carrying a shield
        ItemStack offhand = this.getItemBySlot(EquipmentSlot.OFFHAND);
        ItemStack mainhand = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (offhand.getItem() instanceof ShieldItem || mainhand.getItem() instanceof ShieldItem) {
            if (this.random.nextFloat() < 0.70F) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.0D, this.getZ(),
                            8, 0.3D, 0.3D, 0.3D, 0.1D);
                }
                pAmount *= 0.2F; // 80% damage reduction
            }
        }

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
        damage = Math.max(1.0F, damage * 0.65F);
        DamageSource source = this.damageSources().mobAttack(this);
        return target.hurt(source, damage);
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

        if (!isRecruited()) {
            // Sempre abre a tela do Contrato de Honra para contratação manual pelo botão
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inv, p) -> new HonorContractMenu(id, inv, this.getId()),
                    Component.literal("Contrato de Honra")
            ), buf -> buf.writeInt(this.getId()));
            return InteractionResult.SUCCESS;
        } else {
            // Check if player is the owner
            if (this.isOwner(pPlayer)) {
                // Open Warrior Companion Menu directly on click
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
        pCompound.putInt("CombatMode", getCombatMode());
        pCompound.putBoolean("Recruited", isRecruited());
        pCompound.putString("WarriorName", getWarriorName());
        pCompound.putBoolean("DuelMode", isDuelMode());
        pCompound.putBoolean("QuestStarted", isQuestStarted());
        pCompound.putInt("QuestType", getQuestType());
        pCompound.putInt("RecruitCost", getRecruitCost());

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
        if (pCompound.contains("CombatMode")) {
            setCombatMode(pCompound.getInt("CombatMode"));
        }
        if (pCompound.contains("Recruited")) {
            setRecruited(pCompound.getBoolean("Recruited"));
        }
        if (pCompound.contains("WarriorName")) {
            setWarriorName(pCompound.getString("WarriorName"));
        }
        if (pCompound.contains("DuelMode")) {
            setDuelMode(pCompound.getBoolean("DuelMode"));
        }
        if (pCompound.contains("RecruitCost")) {
            setRecruitCost(pCompound.getInt("RecruitCost"));
        }
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

    // ══════════════════════════════════════════════════════════════════════════
    // INNER AI GOALS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Follow Owner Goal: Follows owner when distance > 4 blocks.
     * Intelligently teleports if distance > 16 blocks away or if navigation path is blocked.
     * Respects Guard (2) and Base/Stay (3) modes: NEVER follows or auto-teleports in those modes.
     */
    public static class FollowOwnerGoal extends Goal {
        private final WarriorCompanionEntity warrior;
        private final double speedModifier;
        private final float stopDist;
        private final float startDist;
        private int timeToRecalcPath;

        public FollowOwnerGoal(WarriorCompanionEntity warrior, double speedModifier, float startDist, float stopDist) {
            this.warrior = warrior;
            this.speedModifier = speedModifier;
            this.startDist = startDist;
            this.stopDist = stopDist;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (warrior.isTalking()) return false;
            Player owner = warrior.getOwner();
            if (owner == null || owner.isSpectator() || !warrior.isRecruited()) return false;
            if (warrior.getCombatMode() == 2 || warrior.getCombatMode() == 3) return false; // Guarda ou Ficar
            if (warrior.getTarget() != null && warrior.getTarget().isAlive()) return false; // Não interrompe combate a não ser por teleport de 15 blocos
            return warrior.distanceToSqr(owner) > (double) (startDist * startDist);
        }

        @Override
        public boolean canContinueToUse() {
            if (warrior.isTalking()) return false;
            Player owner = warrior.getOwner();
            if (owner == null || !warrior.isRecruited()) return false;
            if (warrior.getCombatMode() == 2 || warrior.getCombatMode() == 3) return false;
            if (warrior.getTarget() != null && warrior.getTarget().isAlive()) return false;
            return warrior.distanceToSqr(owner) > (double) (stopDist * stopDist);
        }

        @Override
        public void tick() {
            Player owner = warrior.getOwner();
            if (owner == null) return;
            if (warrior.getCombatMode() == 2 || warrior.getCombatMode() == 3) return;

            warrior.getLookControl().setLookAt(owner, 10.0F, (float) warrior.getMaxHeadXRot());

            // Se o dono mudou de dimensão (e estamos no modo Seguir 0 ou 1 e dono está no chão firme)
            if (owner.level() != warrior.level() && owner.level() instanceof ServerLevel targetLevel) {
                if (owner.onGround() && owner.getY() >= owner.level().getMinBuildHeight()) {
                    warrior.teleportTo(targetLevel, owner.getX(), owner.getY(), owner.getZ(), null, warrior.getYRot(), warrior.getXRot());
                    warrior.safeTeleportTo(owner);
                }
                return;
            }

            // Se o dono não estiver no chão firme (voando de Elytra, criativo ou caindo), NÃO teletransporta
            if (!owner.onGround() || owner.isFallFlying() || owner.getAbilities().flying) {
                return;
            }

            double distSq = warrior.distanceToSqr(owner);
            if (distSq >= 225.0D) { // >= 15 blocks
                warrior.safeTeleportTo(owner);
                return;
            }

            if (--timeToRecalcPath <= 0) {
                timeToRecalcPath = 10;
                if (!warrior.getNavigation().moveTo(owner, speedModifier)) {
                    if (distSq > 64.0D) { // > 8 blocks sem caminho terrestre
                        warrior.safeTeleportTo(owner);
                    }
                }
            }
        }
    }

    /**
     * Defend Owner Goal: Prioritizes attacking whoever attacked the owner first.
     */
    public static class DefendOwnerGoal extends TargetGoal {
        private final WarriorCompanionEntity warrior;
        private LivingEntity ownerLastHurtBy;
        private int timestamp;

        public DefendOwnerGoal(WarriorCompanionEntity warrior) {
            super(warrior, false);
            this.warrior = warrior;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!warrior.isRecruited() || warrior.getCombatMode() == 3 || warrior.isEmergencyRetreating() || warrior.isLowHealth()) return false;
            Player owner = warrior.getOwner();
            if (owner == null) return false;

            this.ownerLastHurtBy = owner.getLastHurtByMob();
            int i = owner.getLastHurtByMobTimestamp();
            return i != this.timestamp && this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT) &&
                    this.ownerLastHurtBy != warrior && this.ownerLastHurtBy != owner;
        }

        @Override
        public void start() {
            this.mob.setTarget(this.ownerLastHurtBy);
            Player owner = warrior.getOwner();
            if (owner != null) {
                this.timestamp = owner.getLastHurtByMobTimestamp();
            }
            super.start();
        }
    }

    /**
     * Aggressive Target Goal: Attacks hostile monsters nearby in Aggressive mode.
     */
    public static class AggressiveTargetGoal extends NearestAttackableTargetGoal<Mob> {
        private final WarriorCompanionEntity warrior;

        public AggressiveTargetGoal(WarriorCompanionEntity warrior) {
            super(warrior, Mob.class, 10, true, false,
                    entity -> entity instanceof Enemy && !(entity instanceof WarriorCompanionEntity));
            this.warrior = warrior;
        }

        @Override
        public boolean canUse() {
            if (!warrior.isRecruited() || warrior.getCombatMode() != 0 || warrior.isEmergencyRetreating() || warrior.isLowHealth()) return false;
            return super.canUse();
        }
    }

    /**
     * Warrior Combat Goal: 1-4 combo hits, then backs up for 2s (stamina recovery).
     */
    public static class WarriorCombatGoal extends MeleeAttackGoal {
        private final WarriorCompanionEntity warrior;
        private int comboCount = 0;
        private int maxCombo = 3;

        public WarriorCombatGoal(WarriorCompanionEntity pMob, double pSpeedModifier, boolean pFollowingTargetEvenIfNotSeen) {
            super(pMob, pSpeedModifier, pFollowingTargetEvenIfNotSeen);
            this.warrior = pMob;
        }

        @Override
        public boolean canUse() {
            if (warrior.inStaminaRegen || warrior.isDuelMode() || warrior.isEmergencyRetreating() || warrior.isLowHealth()) return false;
            if (warrior.isRecruited()) {
                if (warrior.getCombatMode() == 3) { // Base / Ficar
                    warrior.clearCombatTarget();
                    return false;
                }
                if (warrior.getCombatMode() == 2 && warrior.getGuardPos() != null) {
                    if (warrior.getTarget() != null) {
                        double distToGuard = warrior.getTarget().distanceToSqr(
                                warrior.getGuardPos().getX() + 0.5D,
                                warrior.getGuardPos().getY(),
                                warrior.getGuardPos().getZ() + 0.5D
                        );
                        if (distToGuard > 64.0D) { // > 8 blocos do ponto de guarda
                            warrior.clearCombatTarget();
                            return false;
                        }
                    }
                }
                if (warrior.getTarget() != null && warrior.getTarget() == warrior.getOwner()) {
                    warrior.setTarget(null);
                    return false;
                }
            }
            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            if (warrior.isEmergencyRetreating() || warrior.isLowHealth()) return false;
            if (warrior.isRecruited()) {
                if (warrior.getCombatMode() == 3) {
                    warrior.clearCombatTarget();
                    return false;
                }
                if (warrior.getCombatMode() == 2 && warrior.getGuardPos() != null) {
                    if (warrior.getTarget() != null) {
                        double distToGuard = warrior.getTarget().distanceToSqr(
                                warrior.getGuardPos().getX() + 0.5D,
                                warrior.getGuardPos().getY(),
                                warrior.getGuardPos().getZ() + 0.5D
                        );
                        if (distToGuard > 64.0D) {
                            warrior.clearCombatTarget();
                            return false;
                        }
                    }
                }
            }
            return super.canContinueToUse();
        }

        @Override
        public void start() {
            super.start();
            comboCount = 0;
            maxCombo = 2 + warrior.random.nextInt(3); // 2 to 4 hits
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {
            if (warrior.isRecruited() && pEnemy == warrior.getOwner()) {
                warrior.setTarget(null);
                return;
            }
            double attackReach = this.getAttackReachSqr(pEnemy);
            if (pDistToEnemySqr <= attackReach && this.isTimeToAttack()) {
                this.resetAttackCooldown();
                this.mob.swing(InteractionHand.MAIN_HAND);
                this.mob.doHurtTarget(pEnemy);
                comboCount++;

                if (comboCount >= maxCombo) {
                    // Trigger stamina regen retreat
                    warrior.inStaminaRegen = true;
                    warrior.staminaRegenCooldown = 40; // 2 seconds

                    // Push back slightly (roll/evade simulation)
                    Vec3 dir = warrior.position().subtract(pEnemy.position()).normalize().scale(0.6D);
                    warrior.setDeltaMovement(dir.x, 0.25D, dir.z);
                    comboCount = 0;
                }
            }
        }
    }

    /**
     * Emergency Retreat & Eat Goal:
     * When health is <= 30% and food exists in the backpack:
     * 1. Performs an immediate evasion roll away from danger.
     * 2. Clears combat target and runs away at high speed (1.4x).
     * 3. Equips food in main hand and performs full eating animation with sound and particles.
     * 4. Consumes food, heals HP, and repeats until health >= 70% or food runs out.
     */
    public static class EmergencyRetreatAndEatGoal extends Goal {
        private final WarriorCompanionEntity warrior;
        private int currentSlot = -1;
        private int eatingTicks = 0;
        private int totalUseDuration = 32;
        private int recalPathTicks = 0;

        public EmergencyRetreatAndEatGoal(WarriorCompanionEntity warrior) {
            this.warrior = warrior;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private int findFoodSlot() {
            WarriorInventory inv = warrior.getWarriorInventory();
            for (int i = WarriorInventory.SLOT_BACKPACK_START; i <= WarriorInventory.SLOT_BACKPACK_END; i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && stack.isEdible()) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public boolean canUse() {
            if (warrior.isTalking() || warrior.isDuelMode()) return false;
            boolean lowHp = warrior.getHealth() <= (warrior.getMaxHealth() * 0.30F);
            return (lowHp || warrior.isEmergencyRetreating()) && findFoodSlot() != -1;
        }

        @Override
        public boolean canContinueToUse() {
            if (warrior.isTalking() || warrior.isDuelMode()) return false;
            if (warrior.getHealth() >= (warrior.getMaxHealth() * 0.70F)) return false;
            return findFoodSlot() != -1;
        }

        @Override
        public void start() {
            warrior.setTarget(null);
            warrior.setLastHurtByMob(null);
            warrior.clearCombatTarget();
            warrior.setEmergencyRetreatCooldown(180); // 9s de recuo prioritário

            // Executa o rolamento evasivo de emergência
            LivingEntity threat = warrior.getLastHurtByMob();
            if (threat == null || !threat.isAlive()) threat = warrior.getTarget();
            warrior.performEmergencyDodgeRoll(threat != null ? threat.position() : null);

            currentSlot = findFoodSlot();
            eatingTicks = 0;
            recalPathTicks = 0;

            if (currentSlot != -1) {
                ItemStack stack = warrior.getWarriorInventory().getItem(currentSlot);
                totalUseDuration = stack.getUseDuration() > 0 ? stack.getUseDuration() : 32;
                warrior.setItemSlot(EquipmentSlot.MAINHAND, stack);
                warrior.startUsingItem(InteractionHand.MAIN_HAND);
            }
        }

        @Override
        public void tick() {
            // Garante que o companheiro não trava a mira nem adquire alvos enquanto foge para comer
            if (warrior.getTarget() != null) {
                warrior.setTarget(null);
            }
            if (warrior.getLastHurtByMob() != null) {
                warrior.setLastHurtByMob(null);
            }

            // 1. Navegação de fuga / reposicionamento (distância reduzida pela metade: 5 blocos)
            if (recalPathTicks <= 0) {
                recalPathTicks = 12;
                LivingEntity threat = null;
                // Procura monstro mais próximo num raio de 8 blocos para calcular a direção oposta
                var nearbyEnemies = warrior.level().getEntitiesOfClass(
                        net.minecraft.world.entity.monster.Monster.class,
                        warrior.getBoundingBox().inflate(8.0D),
                        net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR
                );
                if (!nearbyEnemies.isEmpty()) {
                    threat = nearbyEnemies.get(0);
                }

                Vec3 targetPos;
                if (threat != null) {
                    Vec3 awayDir = warrior.position().subtract(threat.position()).multiply(1.0D, 0.0D, 1.0D).normalize().scale(5.0D);
                    targetPos = warrior.position().add(awayDir);
                } else {
                    Player owner = warrior.getOwner();
                    if (owner != null && warrior.distanceToSqr(owner) > 16.0D) {
                        targetPos = owner.position();
                    } else {
                        targetPos = warrior.position().add(
                                -Math.sin(Math.toRadians(warrior.getYRot())) * 4.0D,
                                0.0D,
                                Math.cos(Math.toRadians(warrior.getYRot())) * 4.0D
                        );
                    }
                }

                warrior.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.25D);
                // Direciona o olhar para a rota de fuga / mãos (nunca para os monstros)
                warrior.getLookControl().setLookAt(targetPos.x, warrior.getY() + 0.6D, targetPos.z, 50.0F, 50.0F);
            } else {
                recalPathTicks--;
            }

            // 2. Lógica de comer
            currentSlot = findFoodSlot();
            if (currentSlot == -1) {
                stop();
                return;
            }

            ItemStack foodStack = warrior.getWarriorInventory().getItem(currentSlot);
            if (foodStack.isEmpty() || !foodStack.isEdible()) {
                stop();
                return;
            }

            // Garante que o item de comida está na mão e sendo consumido visualmente
            if (!warrior.isUsingItem()) {
                warrior.setItemSlot(EquipmentSlot.MAINHAND, foodStack);
                warrior.startUsingItem(InteractionHand.MAIN_HAND);
            }

            eatingTicks++;

            // Partículas saindo da boca e som de mastigação a cada 4 ticks
            if (eatingTicks % 4 == 0) {
                warrior.level().playSound(null, warrior.getX(), warrior.getY(), warrior.getZ(),
                        SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.6F, 0.9F + (warrior.getRandom().nextFloat() * 0.2F));

                if (warrior.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, foodStack),
                            warrior.getX(), warrior.getY() + 1.35D, warrior.getZ(),
                            6, 0.2D, 0.2D, 0.2D, 0.05D);
                }
            }

            // Conclusão da refeição
            totalUseDuration = foodStack.getUseDuration() > 0 ? foodStack.getUseDuration() : 32;
            if (eatingTicks >= totalUseDuration) {
                eatingTicks = 0;
                var foodProps = foodStack.getItem().getFoodProperties(foodStack, warrior);
                int nutrition = foodProps != null ? foodProps.getNutrition() : 4;
                float saturation = foodProps != null ? foodProps.getSaturationModifier() : 0.6F;
                float healAmount = Math.max(4.0F, nutrition + (saturation * 2.0F));

                warrior.heal(healAmount);

                // Efeitos de comida especial
                if (foodProps != null) {
                    for (com.mojang.datafixers.util.Pair<net.minecraft.world.effect.MobEffectInstance, Float> effectPair : foodProps.getEffects()) {
                        if (effectPair.getFirst() != null && warrior.getRandom().nextFloat() < effectPair.getSecond()) {
                            warrior.addEffect(new net.minecraft.world.effect.MobEffectInstance(effectPair.getFirst()));
                        }
                    }
                }

                // Partículas de coração
                if (warrior.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART, warrior.getX(), warrior.getY() + 1.2D, warrior.getZ(),
                            6, 0.3D, 0.3D, 0.3D, 0.05D);
                }

                // Som de arroto/satisfação ao terminar
                warrior.level().playSound(null, warrior.getX(), warrior.getY(), warrior.getZ(),
                        SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 0.7F, 1.0F);

                foodStack.shrink(1);
                if (foodStack.isEmpty()) {
                    warrior.getWarriorInventory().setItem(currentSlot, ItemStack.EMPTY);
                }

                warrior.stopUsingItem();
                warrior.syncEquipmentWithInventory();
            }
        }

        @Override
        public void stop() {
            eatingTicks = 0;
            currentSlot = -1;
            warrior.stopUsingItem();
            warrior.syncEquipmentWithInventory();
            warrior.setEmergencyRetreatCooldown(0);
        }
    }

    /**
     * Guard Radius Goal: Patrolled area of 5 blocks around guardPos.
     */
    public static class GuardRadiusGoal extends Goal {
        private final WarriorCompanionEntity warrior;
        private final double speedModifier;

        public GuardRadiusGoal(WarriorCompanionEntity warrior, double speedModifier) {
            this.warrior = warrior;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (warrior.isTalking()) return false;
            return warrior.isRecruited() && warrior.getCombatMode() == 2 && warrior.getGuardPos() != null;
        }

        @Override
        public void tick() {
            BlockPos gPos = warrior.getGuardPos();
            if (gPos == null) return;

            double distSq = warrior.distanceToSqr(gPos.getX() + 0.5D, gPos.getY(), gPos.getZ() + 0.5D);
            if (distSq > 36.0D) { // > 6 blocks from guard point
                warrior.getNavigation().moveTo(gPos.getX() + 0.5D, gPos.getY(), gPos.getZ() + 0.5D, speedModifier);
            }
        }
    }

    /**
     * Auto Feed Goal: Scans backpack (slots 6-20) for food and eats taking the full standard Minecraft eating duration (32 ticks).
     */
    public static class AutoFeedGoal extends Goal {
        private final WarriorCompanionEntity warrior;
        private int currentSlot = -1;
        private int eatingTicks = 0;
        private int totalUseDuration = 32;
        private int postEatCooldown = 0;

        public AutoFeedGoal(WarriorCompanionEntity warrior) {
            this.warrior = warrior;
            this.setFlags(EnumSet.noneOf(Goal.Flag.class));
        }

        private int findFoodSlot() {
            WarriorInventory inv = warrior.getWarriorInventory();
            for (int i = WarriorInventory.SLOT_BACKPACK_START; i <= WarriorInventory.SLOT_BACKPACK_END; i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && stack.isEdible()) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public boolean canUse() {
            if (postEatCooldown > 0) {
                postEatCooldown--;
                return false;
            }
            if (warrior.getHealth() >= warrior.getMaxHealth()) return false;
            if (warrior.getTarget() != null && warrior.getTarget().isAlive()) return false;
            return findFoodSlot() != -1;
        }

        @Override
        public boolean canContinueToUse() {
            if (warrior.getHealth() >= warrior.getMaxHealth()) return false;
            if (warrior.getTarget() != null && warrior.getTarget().isAlive()) return false;
            if (currentSlot == -1) return false;
            ItemStack stack = warrior.getWarriorInventory().getItem(currentSlot);
            return !stack.isEmpty() && stack.isEdible() && eatingTicks < totalUseDuration;
        }

        @Override
        public void start() {
            currentSlot = findFoodSlot();
            if (currentSlot != -1) {
                ItemStack stack = warrior.getWarriorInventory().getItem(currentSlot);
                totalUseDuration = stack.getUseDuration() > 0 ? stack.getUseDuration() : 32;
                eatingTicks = 0;
                warrior.setItemSlot(EquipmentSlot.MAINHAND, stack);
                warrior.startUsingItem(InteractionHand.MAIN_HAND);
            }
        }

        @Override
        public void stop() {
            currentSlot = -1;
            eatingTicks = 0;
            warrior.stopUsingItem();
            warrior.syncEquipmentWithInventory();
        }

        @Override
        public void tick() {
            if (currentSlot == -1) return;

            WarriorInventory inv = warrior.getWarriorInventory();
            ItemStack stack = inv.getItem(currentSlot);
            if (stack.isEmpty() || !stack.isEdible()) {
                stop();
                return;
            }

            if (!warrior.isUsingItem()) {
                warrior.setItemSlot(EquipmentSlot.MAINHAND, stack);
                warrior.startUsingItem(InteractionHand.MAIN_HAND);
            }

            eatingTicks++;

            // Som de mastigação e partículas a cada 4 ticks (padrão vanilla)
            if (eatingTicks % 4 == 0) {
                warrior.level().playSound(null, warrior.getX(), warrior.getY(), warrior.getZ(),
                        SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.6F, 0.9F + warrior.getRandom().nextFloat() * 0.2F);

                if (warrior.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, stack),
                            warrior.getX(), warrior.getY() + 1.35D, warrior.getZ(),
                            5, 0.18D, 0.18D, 0.18D, 0.05D);
                }
            }

            // Quando conclui os 32 ticks de mastigação -> consome o alimento e cura
            if (eatingTicks >= totalUseDuration) {
                var foodProps = stack.getItem().getFoodProperties(stack, warrior);
                int nutrition = foodProps != null ? foodProps.getNutrition() : 4;
                float saturation = foodProps != null ? foodProps.getSaturationModifier() : 0.6F;
                float healAmount = Math.max(4.0F, nutrition + (saturation * 2.0F));

                warrior.heal(healAmount);

                // Efeitos de comida especial (ex: maçã dourada)
                if (foodProps != null) {
                    for (com.mojang.datafixers.util.Pair<net.minecraft.world.effect.MobEffectInstance, Float> effectPair : foodProps.getEffects()) {
                        if (effectPair.getFirst() != null && warrior.getRandom().nextFloat() < effectPair.getSecond()) {
                            warrior.addEffect(new net.minecraft.world.effect.MobEffectInstance(effectPair.getFirst()));
                        }
                    }
                }

                // Partículas de cura
                if (warrior.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART, warrior.getX(), warrior.getY() + 1.2D, warrior.getZ(),
                            5, 0.3D, 0.3D, 0.3D, 0.05D);
                }

                // Som de arroto/satisfação ao terminar
                warrior.level().playSound(null, warrior.getX(), warrior.getY(), warrior.getZ(),
                        SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 0.7F, 1.0F);

                // Consumir 1 unidade do alimento
                stack.shrink(1);
                if (stack.isEmpty()) {
                    warrior.getWarriorInventory().setItem(currentSlot, ItemStack.EMPTY);
                }

                // Intervalo natural antes de começar o próximo item
                postEatCooldown = 15;
                stop();
            }
        }
    }

    /**
     * Duel Goal: Non-lethal duel goal during recruitment Quest Type 2.
     */
    public static class DuelGoal extends MeleeAttackGoal {
        private final WarriorCompanionEntity warrior;

        public DuelGoal(WarriorCompanionEntity warrior, double speed) {
            super(warrior, speed, true);
            this.warrior = warrior;
        }

        @Override
        public boolean canUse() {
            return warrior.isDuelMode() && warrior.getDuelTarget() != null && warrior.getDuelTarget().isAlive();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {
            double attackReach = this.getAttackReachSqr(pEnemy);
            if (pDistToEnemySqr <= attackReach && this.isTimeToAttack()) {
                this.resetAttackCooldown();
                this.mob.swing(InteractionHand.MAIN_HAND);

                // Non-lethal hit: caps damage if player would die
                if (pEnemy.getHealth() <= 4.0F) {
                    pEnemy.hurt(warrior.damageSources().mobAttack(warrior), 1.0F);
                } else {
                    this.mob.doHurtTarget(pEnemy);
                }
            }
        }
    }
}
