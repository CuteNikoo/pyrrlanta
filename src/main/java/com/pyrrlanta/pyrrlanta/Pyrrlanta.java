package com.pyrrlanta.pyrrlanta;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.pyrrlanta.pyrrlanta.tribe.TribeCommand;
import com.pyrrlanta.pyrrlanta.tribe.TribeConfig;
import com.pyrrlanta.pyrrlanta.tribe.TribeFireGuard;
import com.pyrrlanta.pyrrlanta.tribe.TribeMapIntegration;
import com.pyrrlanta.pyrrlanta.tribe.TribeMessageEvents;
import com.pyrrlanta.pyrrlanta.tribe.TribeProtectionEvents;
import com.pyrrlanta.pyrrlanta.tribe.TribeTaxCollector;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
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

        NeoForge.EVENT_BUS.register(TribeCommand.class);
        NeoForge.EVENT_BUS.register(TribeProtectionEvents.class);
        NeoForge.EVENT_BUS.register(TribeMessageEvents.class);
        TribeFireGuard.init();
        TribeTaxCollector.init();

        modContainer.registerConfig(ModConfig.Type.COMMON, TribeConfig.SPEC);

        // Soft dependency: only touch BlueMap's classes if the mod is actually present.
        if (ModList.get().isLoaded("bluemap")) {
            TribeMapIntegration.init();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Pyrrlanta common setup");
    }
}
