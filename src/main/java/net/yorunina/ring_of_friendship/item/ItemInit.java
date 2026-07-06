package net.yorunina.ring_of_friendship.item;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.yorunina.ring_of_friendship.RingOfFriendship;

@Mod.EventBusSubscriber(modid = RingOfFriendship.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RingOfFriendship.MODID);

    public static final RegistryObject<Item> RING_OF_FRIENDSHIP = ITEMS.register("ring_of_friendship",
            () -> new RingOfFriendshipItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MAGIC_MIRROR = ITEMS.register("magic_mirror",
            () -> new MagicMirrorItem(new Item.Properties().stacksTo(1)));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(RING_OF_FRIENDSHIP);
            event.accept(MAGIC_MIRROR);
        }
    }
}
