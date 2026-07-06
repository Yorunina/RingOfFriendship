package net.yorunina.ring_of_friendship.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "ring_of_friendship")
public class RingOfFriendshipItem extends Item {

    private static final String TAG_FRIEND_ID = "friend_id";
    private static final String TAG_FRIEND_NAME = "friend_name";

    public RingOfFriendshipItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();
            if (tag.contains(TAG_FRIEND_ID)) {
                UUID friendId = tag.getUUID(TAG_FRIEND_ID);
                ServerPlayer friend = player.getServer().getPlayerList().getPlayer(friendId);
                if (friend != null) {
                    player.teleportTo(friend.getX(), friend.getY(), friend.getZ());
                    return InteractionResultHolder.success(stack);
                }
            } else {
                tag.putUUID(TAG_FRIEND_ID, player.getUUID());
                tag.putString(TAG_FRIEND_NAME, player.getName().getString());
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_FRIEND_NAME)) {
            tooltip.add(Component.translatable("item.ring_of_friendship.tooltip.friend", tag.getString(TAG_FRIEND_NAME)));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof RingOfFriendshipItem) {
                    CompoundTag tag = stack.getTag();
                    if (tag != null && tag.contains(TAG_FRIEND_ID)) {
                        UUID friendId = tag.getUUID(TAG_FRIEND_ID);
                        ServerPlayer friend = player.getServer().getPlayerList().getPlayer(friendId);
                        if (friend != null) {
                            event.setCanceled(true);
                            player.setHealth(1.0f);
                            player.teleportTo(friend.getX(), friend.getY(), friend.getZ());
                        }
                    }
                }
            }
        }
    }
}