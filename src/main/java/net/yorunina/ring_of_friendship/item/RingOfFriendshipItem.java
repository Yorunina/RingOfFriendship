package net.yorunina.ring_of_friendship.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yorunina.ring_of_friendship.RingOfFriendship;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotResult;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = RingOfFriendship.MODID)
public class RingOfFriendshipItem extends Item {

    private static final String TAG_FRIEND_ID = "friend_id";
    private static final String TAG_FRIEND_NAME = "friend_name";
    private static final int USE_DURATION_TICKS = 30; // 1.5 seconds
    private static final int TELEPORT_COOLDOWN_TICKS = 200; // 10 seconds
    private static final int DEATH_SAVE_COOLDOWN_TICKS = 1200; // 5 minutes

    public RingOfFriendshipItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return USE_DURATION_TICKS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        pPlayer.startUsingItem(pHand);
        return InteractionResultHolder.consume(pPlayer.getItemInHand(pHand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        if (pLevel.isClientSide()) {
            return pStack;
        }

        ServerPlayer player = (ServerPlayer) pLivingEntity;
        CompoundTag tag = pStack.getOrCreateTag();

        if (player.isShiftKeyDown() || !tag.contains(TAG_FRIEND_ID)) {
            tag.putUUID(TAG_FRIEND_ID, player.getGameProfile().getId());
            tag.putString(TAG_FRIEND_NAME, player.getName().getString());
            pStack.setTag(tag);
            player.sendSystemMessage(Component.translatable("item.ring_of_friendship.bound", player.getName()).withStyle(ChatFormatting.AQUA));
        } else {
            UUID friendId = tag.getUUID(TAG_FRIEND_ID);
            if (player.getGameProfile().getId().equals(friendId)) {
                // Single-player: Teleport to spawn
                teleportToSpawn(player);
                pLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, player.getSoundSource(), 1.0F, 1.0F);
                player.getCooldowns().addCooldown(this, TELEPORT_COOLDOWN_TICKS);
                player.sendSystemMessage(Component.translatable("item.ring_of_friendship.teleported_spawn").withStyle(ChatFormatting.GREEN));
            } else {
                // Multiplayer: Teleport to friend
                ServerPlayer friend = player.getServer().getPlayerList().getPlayer(friendId);
                if (friend != null) {
                    player.getCooldowns().addCooldown(this, TELEPORT_COOLDOWN_TICKS);
                    player.teleportTo(friend.serverLevel(), friend.getX(), friend.getY(), friend.getZ(), friend.getYRot(), friend.getXRot());
                    pLevel.playSound(null, friend.getX(), friend.getY(), friend.getZ(), SoundEvents.ENDERMAN_TELEPORT, friend.getSoundSource(), 1.0F, 1.0F);
                    player.sendSystemMessage(Component.translatable("item.ring_of_friendship.teleported", friend.getName()).withStyle(ChatFormatting.GREEN));
                } else {
                    player.sendSystemMessage(Component.translatable("item.ring_of_friendship.friend_offline").withStyle(ChatFormatting.RED));
                }
            }
        }
        return pStack;
    }

    @Override
    public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity, int pUseRemaining) {
        // No action on release, only on finish
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        CompoundTag tag = pStack.getTag();
        Player player = Minecraft.getInstance().player;

        if (tag != null && tag.contains(TAG_FRIEND_NAME)) {
            pTooltipComponents.add(Component.translatable("item.ring_of_friendship.tooltip.friend", Component.literal(tag.getString(TAG_FRIEND_NAME)).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
            pTooltipComponents.add(Component.translatable("item.ring_of_friendship.tooltip.how_to_teleport").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            pTooltipComponents.add(Component.translatable("item.ring_of_friendship.tooltip.unbound").withStyle(ChatFormatting.YELLOW));
            pTooltipComponents.add(Component.translatable("item.ring_of_friendship.tooltip.how_to_bind").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        pTooltipComponents.add(Component.translatable("item.ring_of_friendship.tooltip.consumes_on_use").withStyle(ChatFormatting.DARK_RED));

        if (player != null && player.getCooldowns().isOnCooldown(this)) {
            pTooltipComponents.add(Component.translatable("item.ring_of_friendship.tooltip.status.on_cooldown").withStyle(ChatFormatting.RED));
        } else {
            pTooltipComponents.add(Component.translatable("item.ring_of_friendship.tooltip.status.ready").withStyle(ChatFormatting.GREEN));
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (event.getAmount() < player.getHealth()) {
            return; // Not a fatal blow
        }

        // Find a ring to use, first from Curios, then from hands
        ItemStack ringStack = findRing(player);
        if (ringStack != null && trySavePlayer(player, ringStack, event)) {
            ringStack.shrink(1); // Consume the ring
            player.getInventory().add(new ItemStack(Items.GOLD_INGOT));
            player.sendSystemMessage(Component.translatable("item.ring_of_friendship.shattered").withStyle(ChatFormatting.DARK_RED));
        }
    }

    @Nullable
    private static ItemStack findRing(ServerPlayer player) {
        Optional<SlotResult> slotResultOpt = CuriosCompat.findRingSlot(player);
        if (slotResultOpt.isPresent()) {
            return slotResultOpt.get().stack();
        }

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stackInHand = player.getItemInHand(hand);
            if (stackInHand.getItem() instanceof RingOfFriendshipItem) {
                return stackInHand;
            }
        }
        return null;
    }

    private static boolean trySavePlayer(ServerPlayer player, ItemStack stack, LivingHurtEvent event) {
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return false;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_FRIEND_ID)) {
            return false;
        }

        UUID friendId = tag.getUUID(TAG_FRIEND_ID);
        if (player.getUUID().equals(friendId)) {
            // Single-player: Save by teleporting to spawn
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth() * 0.5f);
            teleportToSpawn(player);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, player.getSoundSource(), 1.0F, 1.0F);
            player.getCooldowns().addCooldown(stack.getItem(), DEATH_SAVE_COOLDOWN_TICKS);
            player.sendSystemMessage(Component.translatable("item.ring_of_friendship.saved_self").withStyle(ChatFormatting.GOLD));
            return true;
        } else {
            // Multiplayer: Save by teleporting to friend
            ServerPlayer friend = player.getServer().getPlayerList().getPlayer(friendId);
            if (friend != null) {
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth() * 0.5f);
                player.teleportTo(friend.serverLevel(), friend.getX(), friend.getY(), friend.getZ(), friend.getYRot(), friend.getXRot());
                player.getCooldowns().addCooldown(stack.getItem(), DEATH_SAVE_COOLDOWN_TICKS);
                player.sendSystemMessage(Component.translatable("item.ring_of_friendship.saved").withStyle(ChatFormatting.GOLD));
                friend.sendSystemMessage(Component.translatable("item.ring_of_friendship.friend_saved", player.getName()).withStyle(ChatFormatting.GOLD));
                return true;
            }
        }
        return false;
    }

    private static void teleportToSpawn(ServerPlayer player) {
        BlockPos respawnPos = player.getRespawnPosition();
        ServerLevel respawnLevel = player.getServer().getLevel(player.getRespawnDimension());

        if (respawnPos == null || respawnLevel == null) {
            respawnLevel = player.getServer().overworld();
            respawnPos = respawnLevel.getSharedSpawnPos();
        }

        player.teleportTo(respawnLevel, respawnPos.getX() + 0.5, respawnPos.getY(), respawnPos.getZ() + 0.5, player.getRespawnAngle(), 0);
    }

    private static class CuriosCompat {
        private static Optional<SlotResult> findRingSlot(Player player) {
            return player.getCapability(CuriosCapability.INVENTORY)
                    .map(handler -> handler.findFirstCurio(ItemInit.RING_OF_FRIENDSHIP.get()))
                    .orElse(Optional.empty());
        }
    }
}
