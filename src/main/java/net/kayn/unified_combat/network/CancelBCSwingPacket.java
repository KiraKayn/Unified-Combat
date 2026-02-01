package net.kayn.unified_combat.network;

import net.kayn.unified_combat.client.RollInputHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CancelBCSwingPacket {
    public CancelBCSwingPacket() {}
    public static void write(CancelBCSwingPacket pkt, FriendlyByteBuf buf) {}
    public static CancelBCSwingPacket read(FriendlyByteBuf buf) { return new CancelBCSwingPacket(); }

    public static void handle(CancelBCSwingPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(RollInputHandler::cancelClientAttack);
        ctx.setPacketHandled(true);
    }
}