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
import net.minecraft.world.entity.TamableAnimal;
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
 *   <li>Mirando numa criatura: todos atacam esse alvo (mesmo se estiverem pacíficos).</li>
 *   <li>Mirando no nada: todos param de lutar e voltam para você.</li>
 * </ul>
 * Não serve para mandar atacar jogadores, companheiros, nem os seus animais domesticados.
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
        if (target != null && isValidOrderTarget(player, target)) {
            int ordered = 0;
            for (WarriorCompanionEntity w : companions) {
                if (w.getCombatMode() == WarriorCompanionEntity.MODE_STAY) continue; // Parado segura a posição
                w.setTarget(target);
                if (w.getTarget() == target) ordered++;
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
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator(), eye.distanceToSqr(end));
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static boolean isValidOrderTarget(Player player, Entity target) {
        if (target instanceof Player) return false;
        if (target instanceof WarriorCompanionEntity) return false;
        if (target instanceof TamableAnimal pet && pet.isOwnedBy(player)) return false;
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.epicvanguard.vanguard_horn.tooltip.1").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.epicvanguard.vanguard_horn.tooltip.2").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
