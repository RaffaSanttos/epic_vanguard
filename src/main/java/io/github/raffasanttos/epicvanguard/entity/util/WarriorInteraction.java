package io.github.raffasanttos.epicvanguard.entity.util;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.inventory.WarriorInventory;
import io.github.raffasanttos.epicvanguard.screen.HonorContractMenu;
import io.github.raffasanttos.epicvanguard.screen.WarriorCompanionMenu;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

/** Interação do jogador com o guerreiro (resgate, contrato, name tag, abrir a tela) e a coleta de itens do chão. */
public final class WarriorInteraction {

    private WarriorInteraction() {}

    /** Clique com o botão direito no guerreiro. Devolve null para cair no comportamento padrão do Mob. */
    public static InteractionResult interact(WarriorCompanionEntity w, Player pPlayer, InteractionHand pHand) {
        if (pHand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (w.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ServerPlayer serverPlayer = (ServerPlayer) pPlayer;
        w.startTalkingWith(serverPlayer, 160);

        if (w.isPrisoner()) {
            ItemStack heldItem = pPlayer.getItemInHand(pHand);
            boolean isFood = heldItem.isEdible();
            boolean isPotion = heldItem.getItem() instanceof PotionItem;
            boolean isWeapon = heldItem.getItem() instanceof SwordItem || heldItem.getItem() instanceof AxeItem;

            if (isFood || isPotion || isWeapon) {
                if (!pPlayer.isCreative()) {
                    if (isWeapon) {
                        w.getWarriorInventory().setItem(WarriorInventory.SLOT_WEAPON_MAIN, heldItem.copy());
                        heldItem.shrink(1);
                    } else {
                        heldItem.shrink(1);
                    }
                } else if (isWeapon) {
                    w.getWarriorInventory().setItem(WarriorInventory.SLOT_WEAPON_MAIN, heldItem.copy());
                }

                // Resgate com sucesso: cura vida, remove fraqueza, assina contrato de honra de graça!
                w.setPrisoner(false);
                w.removeEffect(MobEffects.WEAKNESS);
                w.setHealth(w.getMaxHealth());
                w.setOwnerUUID(pPlayer.getUUID());
                w.setRecruited(true);
                w.applyRecruitDefaults(); // Seguir + pacífico
                w.setEquipmentTier(0);
                w.syncEquipmentWithInventory();

                if (w.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            w.getX(), w.getY() + 1.0D, w.getZ(),
                            25, 0.5D, 0.5D, 0.5D, 0.05D);
                    serverLevel.sendParticles(ParticleTypes.HEART,
                            w.getX(), w.getY() + 1.2D, w.getZ(),
                            10, 0.4D, 0.4D, 0.4D, 0.05D);
                    serverLevel.playSound(null, w.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                }

                pPlayer.sendSystemMessage(Component.translatable("epicvanguard.text.warrior_companion_entity.7", w.getWarriorName()));
                return InteractionResult.SUCCESS;
            } else {
                pPlayer.sendSystemMessage(Component.translatable("epicvanguard.text.warrior_companion_entity.8", w.getWarriorName()));
                return InteractionResult.SUCCESS;
            }
        }

        if (!w.isRecruited()) {
            // Abre a tela do Contrato de Honra para contratação manual pelo botão
            w.setInventoryOpenPlayer(serverPlayer);
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inv, p) -> new HonorContractMenu(id, inv, w.getId()),
                    Component.translatable("epicvanguard.text.warrior_companion_entity.9")
            ), buf -> buf.writeInt(w.getId()));
            return InteractionResult.SUCCESS;
        } else {
            // Check if player is the owner
            if (w.isOwner(pPlayer)) {
                ItemStack heldItem = pPlayer.getItemInHand(pHand);

                // Ordem rápida: agachado e de mão vazia alterna Seguir <-> Parado sem abrir a tela
                if (pPlayer.isShiftKeyDown() && heldItem.isEmpty()) {
                    boolean toStay = w.getCombatMode() != WarriorCompanionEntity.MODE_STAY;
                    w.setCombatMode(toStay ? WarriorCompanionEntity.MODE_STAY : WarriorCompanionEntity.MODE_FOLLOW);
                    if (toStay) w.clearCombatTarget();
                    w.updateSavedData();
                    pPlayer.displayClientMessage(Component.translatable(toStay
                            ? "epicvanguard.text.quick_order.stay" : "epicvanguard.text.quick_order.follow", w.getWarriorName()), true);
                    if (w.level() instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, w.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.NEUTRAL, 0.7F, toStay ? 0.8F : 1.2F);
                    }
                    return InteractionResult.SUCCESS;
                }
                if (heldItem.getItem() instanceof net.minecraft.world.item.NameTagItem && heldItem.hasCustomHoverName()) {
                    String raw = heldItem.getHoverName().getString();
                    String clean = raw.replaceAll("§[0-9a-fk-or]", "").trim();
                    if (!clean.isEmpty()) {
                        w.setWarriorName(clean);
                        if (!pPlayer.isCreative()) {
                            heldItem.shrink(1);
                        }
                        if (w.level() instanceof ServerLevel serverLevel) {
                            serverLevel.playSound(null, w.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
                        }
                        pPlayer.sendSystemMessage(Component.translatable("epicvanguard.text.warrior_companion_entity.10", clean));
                        return InteractionResult.SUCCESS;
                    }
                }

                // Open Warrior Companion Menu directly on click
                w.setInventoryOpenPlayer(serverPlayer);
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (id, inv, p) -> new WarriorCompanionMenu(id, inv, w.getId()),
                        Component.translatable("epicvanguard.text.warrior_companion_entity.11", w.getWarriorName())
                ), buf -> buf.writeInt(w.getId()));
                return InteractionResult.SUCCESS;
            }
        }

        return null;
    }

    /**
     * Coleta automática: guarda na mochila os itens do chão por onde o guerreiro passa.
     * Botão da tela: desligada, só úteis (comida, poções de cura, moedas, flechas, equipamento melhor) ou tudo.
     * Nunca pega o que o próprio dono jogou no chão
     * (o dono pode largar itens perto dele sem perdê-los para a mochila).
     */
    public static void pickUpGroundItems(WarriorCompanionEntity w) {
        if (!w.isAlive() || w.level().isClientSide() || !w.isRecruited() || w.isPrisoner() || !w.isPickupItems()) {
            return;
        }
        // Não precisa checar todo tick: a cada 5 ticks, espalhado entre guerreiros pelo id
        if ((w.tickCount + w.getId()) % 5 != 0) return;

        Entity owner = w.getOwner();
        java.util.UUID ownerId = w.getOwnerUUID().orElse(null);
        AABB pickupBox = w.getBoundingBox().inflate(1.0D, 0.5D, 1.0D);
        boolean usefulOnly = w.getPickupMode() == WarriorCompanionEntity.PICKUP_USEFUL;
        List<ItemEntity> items = w.level().getEntitiesOfClass(ItemEntity.class, pickupBox, e -> !e.isRemoved() && !e.hasPickUpDelay()
                && !isThrownBy(e, owner, ownerId)
                && (!usefulOnly || WarriorEquipment.isUseful(w, e.getItem())));

        for (ItemEntity itemEntity : items) {
            ItemStack groundStack = itemEntity.getItem();
            if (groundStack.isEmpty()) continue;

            int originalCount = groundStack.getCount();
            ItemStack remaining = w.getWarriorInventory().addItemToBackpack(groundStack);
            int pickedUpCount = originalCount - remaining.getCount();

            if (pickedUpCount > 0) {
                w.take(itemEntity, pickedUpCount);
                w.level().playSound(null, w.getX(), w.getY(), w.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL,
                        0.2F, (w.getRandom().nextFloat() - w.getRandom().nextFloat()) * 0.2F + 1.0F);

                if (remaining.isEmpty()) {
                    itemEntity.discard();
                } else {
                    itemEntity.setItem(remaining);
                }
            }
        }
    }

    /** O item foi jogado pelo dono? ItemEntity#getOwner devolve quem jogou (Player#drop marca o thrower). */
    private static boolean isThrownBy(ItemEntity item, Entity owner, java.util.UUID ownerId) {
        Entity thrower = item.getOwner();
        if (thrower == null) return false;
        return thrower == owner || (ownerId != null && ownerId.equals(thrower.getUUID()));
    }
}
