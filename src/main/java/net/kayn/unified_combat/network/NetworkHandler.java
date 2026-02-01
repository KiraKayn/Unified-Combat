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
                new ResourceLocation(UnifiedCombat.MOD_ID, "unified_combat"),
                () -> "1",
                "1"::equals,
                "1"::equals
        );

        // server -> client (and client -> server) CancelCastPacket (keeps original behavior)
        INSTANCE.registerMessage(
                ID.getAndIncrement(),
                CancelCastPacket.class,
                CancelCastPacket::write,
                CancelCastPacket::read,
                CancelCastPacket::handle
        );

        // single RollBufferPacket (both directions; handler checks execute flag)
        INSTANCE.registerMessage(
                ID.getAndIncrement(),
                RollBufferPacket.class,
                RollBufferPacket::write,
                RollBufferPacket::read,
                RollBufferPacket::handle
        );

        INSTANCE.registerMessage(
                ID.getAndIncrement(),
                CancelRollPacket.class,
                CancelRollPacket::write,
                CancelRollPacket::read,
                CancelRollPacket::handle
        );
    }

    public static void registerClientHandlers() {
    }
}
