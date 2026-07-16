package com.pyrrlanta.pyrrlanta;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Pyrrlanta.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Pyrrlanta.MODID, value = Dist.CLIENT)
public class PyrrlantaClient {
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        Pyrrlanta.LOGGER.info("Pyrrlanta client setup");
    }
}
