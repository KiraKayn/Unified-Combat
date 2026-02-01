package net.kayn.unified_combat;

import net.kayn.unified_combat.config.ModConfig;
import net.kayn.unified_combat.event.RollCancelSpellHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(UnifiedCombat.MOD_ID)
public class UnifiedCombat {

    public static final String MOD_ID = "unified_combat";
    public static final Logger LOGGER = LogManager.getLogger();

    public UnifiedCombat(FMLJavaModLoadingContext context) {
        LOGGER.info("Loading Unified Combat");

        context.registerConfig(Type.COMMON, ModConfig.SPEC);

        RollCancelSpellHandler.register();
    }
}
