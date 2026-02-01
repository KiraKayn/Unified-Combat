package net.kayn.unified_combat;

import net.kayn.unified_combat.config.ModConfig;
import net.kayn.unified_combat.event.RollCancelSpellHandler;
import net.kayn.unified_combat.event.RollLockServer;
import net.kayn.unified_combat.network.NetworkHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(UnifiedCombat.MOD_ID)
public class UnifiedCombat {

    public static final String MOD_ID = "unified_combat";
    public static final Logger LOGGER = LogManager.getLogger();

    public UnifiedCombat(FMLJavaModLoadingContext context) {
        LOGGER.info("Loading Unified Combat");

        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.SPEC);

        context.getModEventBus().addListener(this::setup);
        context.getModEventBus().addListener(this::clientSetup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Common setup for Unified Combat");

        NetworkHandler.register();

        RollCancelSpellHandler.register();

        RollLockServer.registerRollStartListener();
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Client setup for Unified Combat");
    }
}