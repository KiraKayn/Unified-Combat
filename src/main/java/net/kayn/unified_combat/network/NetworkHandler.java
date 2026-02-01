package net.kayn.unified_combat.network;

import net.kayn.unified_combat.UnifiedCombat;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;

public class NetworkHandler {

    private static final AtomicInteger ID = new AtomicInteger(0);
    public static SimpleChannel INSTANCE;

    public static void register() {
        INSTANCE = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(UnifiedCombat.MOD_ID, "cancel_cast"),
                () -> "1",
                "1"::equals,
                "1"::equals
        );

        INSTANCE.registerMessage(
                ID.getAndIncrement(),
                CancelCastPacket.class,
                CancelCastPacket::write,
                CancelCastPacket::read,
                CancelCastPacket::handle
        );
    }

    public static void registerClientHandlers() {
    }
}
