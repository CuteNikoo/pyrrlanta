package com.pyrrlanta.pyrrlanta;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.pyrrlanta.pyrrlanta.item.ModItems;
import com.pyrrlanta.pyrrlanta.item.TwilightEgoSetEffects;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

// Content half of Pyrrlanta: the Twilight E.G.O. weapon/armor set.
//
// This mod adds real registry content (items, armor materials) plus a client-only mixin for
// the helmet's per-observer entity highlight, so it must be installed on BOTH sides -- an
// unmodded client cannot join a server running it.
//
// It deliberately keeps the original "pyrrlanta" mod id: the items are registered as
// pyrrlanta:twilight_ego_*, and any already sitting in players' inventories or world saves
// would be destroyed on load if that namespace changed.
//
// The companion "pyrrlanta_tribes" mod holds the server-only tribe/land-claiming system. The
// two are independent -- neither requires the other.
@Mod(Pyrrlanta.MODID)
public class Pyrrlanta {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "pyrrlanta";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Pyrrlanta(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModItems::addCreative);
        ModItems.ITEMS.register(modEventBus);
        ModItems.ARMOR_MATERIALS.register(modEventBus);

        TwilightEgoSetEffects.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Pyrrlanta common setup");
    }
}
