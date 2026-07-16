package com.pyrrlanta.pyrrlantatribes;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.pyrrlanta.pyrrlantatribes.tribe.TribeCommand;
import com.pyrrlanta.pyrrlantatribes.tribe.TribeConfig;
import com.pyrrlanta.pyrrlantatribes.tribe.TribeFireGuard;
import com.pyrrlanta.pyrrlantatribes.tribe.TribeMapIntegration;
import com.pyrrlanta.pyrrlantatribes.tribe.TribeMessageEvents;
import com.pyrrlanta.pyrrlantatribes.tribe.TribeProtectionEvents;
import com.pyrrlanta.pyrrlantatribes.tribe.TribeTaxCollector;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

// Server-side half of Pyrrlanta: the Towny-style tribe/land-claiming system.
//
// This mod registers nothing into any game registry and sends no custom network payloads, so
// it is safe to install on a dedicated server alone -- vanilla (and otherwise unmodded)
// clients can still connect, since NeoForge decides client/server compatibility by registry
// and payload negotiation rather than a mod-list match. All of the tribe UI is built from
// vanilla screens (chest/anvil menus) driven server-side, which is what keeps it client-free.
//
// The companion "pyrrlanta" mod (the Twilight E.G.O. item set) adds real registry content and
// therefore must be installed on both sides; the two mods are independent and neither
// requires the other.
@Mod(PyrrlantaTribes.MODID)
public class PyrrlantaTribes {
    public static final String MODID = "pyrrlanta_tribes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PyrrlantaTribes(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(TribeCommand.class);
        NeoForge.EVENT_BUS.register(TribeProtectionEvents.class);
        NeoForge.EVENT_BUS.register(TribeMessageEvents.class);
        TribeFireGuard.init();
        TribeTaxCollector.init();

        // Pinned to the pre-split filename rather than the mod id's default
        // ("pyrrlanta_tribes-common.toml") so servers upgrading from the combined mod keep
        // their existing tuned values (ore prices, claim costs) instead of silently
        // reverting to defaults under a new file.
        modContainer.registerConfig(ModConfig.Type.COMMON, TribeConfig.SPEC, "pyrrlanta-common.toml");

        // Soft dependency: only touch BlueMap's classes if the mod is actually present.
        if (ModList.get().isLoaded("bluemap")) {
            TribeMapIntegration.init();
        }
    }
}
