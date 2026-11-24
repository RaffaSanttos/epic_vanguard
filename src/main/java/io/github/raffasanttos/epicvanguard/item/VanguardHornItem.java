package io.github.raffasanttos.epicvanguard.item;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Corneta da Vanguarda: ordem rápida para todos os seus companheiros por perto, sem abrir a tela.
 * <ul>
 *   <li>Mirando numa criatura: todos atacam esse alvo (mesmo se estiverem pacíficos). Quem está de Guarda
 *       persegue até o fim, mesmo longe do posto, e depois volta para o posto.</li>
 *   <li>Mirando no nada: todos param de lutar e voltam para você.</li>
 * </ul>
 * Não serve para mandar atacar jogadores, companheiros, animais domesticados (de qualquer jogador),
 * aldeões, golens nem suportes de armadura. A mira passa por eles e pega a próxima criatura.
 */
public class VanguardHornItem extends Item {

    public static final double ORDER_RADIUS = 32.0D;
    private static final double AIM_RANGE = 32.0D;
    private static final int COOLDOWN_TICKS = 40;

    public VanguardHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        // Agachado: troca a formação do modo Seguir (Livre -> Coluna -> Linha -> Círculo)
        if (player.isShiftKeyDown()) {
            return cycleFormation(level, player, stack);
        }

        List<WarriorCompanionEntity> companions = level.getEntitiesOfClass(WarriorCompanionEntity.class,
                player.getBoundingBox().inflate(ORDER_RADIUS),
                w -> w.isAlive() && w.isRecruited() && w.isOwner(player) && !w.isPrisoner());

        level.playSound(null, player.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(),
                SoundSource.PLAYERS, 1.2F, 1.1F);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        if (companions.isEmpty()) {
            player.displayClientMessage(Component.translatable("epicvanguard.text.horn.no_companions"), true);
            return InteractionResultHolder.consume(stack);
        }

        LivingEntity target = findAimedTarget(player);
        if (target != null) {
            int ordered = 0;
            for (WarriorCompanionEntity w : companions) {
                if (w.getCombatMode() == WarriorCompanionEntity.MODE_STAY) continue; // Parado segura a posição
                w.setTarget(target);
                if (w.getTarget() == target) {
                    w.setHornOrderTarget(target); // Guarda: pode sair do posto até matar o alvo
                    ordered++;
                }
            }
            player.displayClientMessage(Component.translatable("epicvanguard.text.horn.attack", ordered, target.getDisplayName()), true);
        } else {
            for (WarriorCompanionEntity w : companions) {
                w.clearCombatTarget();
            }
            player.displayClientMessage(Component.translatable("epicvanguard.text.horn.regroup", companions.size()), true);
        }
        if (level instanceof ServerLevel serverLevel) {
            for (WarriorCompanionEntity w : companions) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE,
                        w.getX(), w.getY() + w.getBbHeight() + 0.3D, w.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    private static InteractionResultHolder<ItemStack> cycleFormation(Level level, Player player, ItemStack stack) {
        if (level.getServer() == null) return InteractionResultHolder.pass(stack);
        io.github.raffasanttos.epicvanguard.entity.CompanionSavedData data =
                io.github.raffasanttos.epicvanguard.entity.CompanionSavedData.get(level.getServer());
        int next = io.github.raffasanttos.epicvanguard.entity.util.WarriorFormation.next(data.getFormation(player.getUUID()));
        data.setFormation(player.getUUID(), next);

        level.playSound(null, player.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2).value(),
                SoundSource.PLAYERS, 0.9F, 1.4F);
        player.getCooldowns().addCooldown(stack.getItem(), 10);
        player.displayClientMessage(Component.translatable("epicvanguard.text.horn.formation",
                io.github.raffasanttos.epicvanguard.entity.util.WarriorFormation.name(next)), true);
        if (level instanceof ServerLevel serverLevel) {
            for (WarriorCompanionEntity w : io.github.raffasanttos.epicvanguard.entity.util.WarriorFormation.members(player)) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE,
                        w.getX(), w.getY() + w.getBbHeight() + 0.3D, w.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    @Nullable
    private static LivingEntity findAimedTarget(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(AIM_RANGE));
        // Para no primeiro bloco no caminho
        var blockHit = player.level().clip(new net.minecraft.world.level.ClipContext(eye, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        AABB box = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && isValidOrderTarget(e),
                eye.distanceToSqr(end));
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    /** Alvos que a corneta aceita. Os outros a mira atravessa (um companheiro na frente não estraga a ordem). */
    public static boolean isValidOrderTarget(Entity target) {
        if (target instanceof Player) return false;
        if (target instanceof WarriorCompanionEntity) return false;
        if (target instanceof ArmorStand) return false;
        if (target instanceof OwnableEntity pet && pet.getOwnerUUID() != null) return false; // pet de qualquer jogador
        if (target instanceof AbstractHorse horse && horse.isTamed()) return false;
        if (target instanceof AbstractVillager || target instanceof IronGolem || target instanceof SnowGolem) return false; // não AbstractGolem: o shulker também é
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.epicvanguard.vanguard_horn.tooltip.1").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.epicvanguard.vanguard_horn.tooltip.2").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.epicvanguard.vanguard_horn.tooltip.3").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
