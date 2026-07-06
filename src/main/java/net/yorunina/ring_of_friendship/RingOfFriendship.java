package net.yorunina.ring_of_friendship;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.yorunina.ring_of_friendship.item.ItemInit;
import org.slf4j.Logger;


@Mod(RingOfFriendship.MODID)
public class RingOfFriendship {

    public static final String MODID = "ring_of_friendship";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RingOfFriendship(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ItemInit.register(modEventBus);
    }
}
