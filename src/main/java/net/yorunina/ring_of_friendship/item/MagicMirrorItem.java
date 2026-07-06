package net.yorunina.ring_of_friendship.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.yorunina.ring_of_friendship.RingOfFriendship;

import javax.annotation.Nullable;
import java.util.List;

public class MagicMirrorItem extends Item {

    private static final int USE_DURATION_TICKS = 30; // 1.5 seconds
    private static final int TELEPORT_COOLDOWN_TICKS = 200; // 10 seconds

    public MagicMirrorItem(Properties properties) {
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
        MinecraftServer server = player.server;
        List<ServerPlayer> targetPlayerList = server.getPlayerList().getPlayers().stream()
                .filter(pPlayer -> !pPlayer.equals(player) && (pPlayer.getOffhandItem().is(this) || pPlayer.getMainHandItem().is(this)) && !pPlayer.hasEffect(MobEffects.INVISIBILITY)).toList();
        RandomSource random = pLevel.getRandom();
        if (targetPlayerList.isEmpty()) {
            pLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, player.getSoundSource(), 1.0F, 1.0F);
            return ItemStack.EMPTY;
        }
        int targetPlayerIndex = random.nextInt(targetPlayerList.size());
        ServerPlayer targetPlayer = targetPlayerList.get(targetPlayerIndex);
        pLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, player.getSoundSource(), 1.0F, 1.0F);
        player.teleportTo(targetPlayer.serverLevel(), targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(), targetPlayer.getYRot(), targetPlayer.getXRot());
        pLevel.playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(), SoundEvents.ENDERMAN_TELEPORT, targetPlayer.getSoundSource(), 1.0F, 1.0F);
        player.getCooldowns().addCooldown(this, TELEPORT_COOLDOWN_TICKS);
        return pStack;
    }

    @Override
    public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity, int pUseRemaining) {
        // No action on release, only on finish
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("item.magic_mirror.tooltip").withStyle(ChatFormatting.GRAY));
    }
}