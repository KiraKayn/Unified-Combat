package net.kayn.unified_combat.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CancelCastPacket {

    public CancelCastPacket() {}

    public static void write(CancelCastPacket packet, FriendlyByteBuf buf) {
    }

    public static CancelCastPacket read(FriendlyByteBuf buf) {
        return new CancelCastPacket();
    }

    public static void handle(CancelCastPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            try {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc == null || mc.player == null) return;

                io.redspace.ironsspellbooks.player.ClientMagicData
                        .resetClientCastState(mc.player.getUUID());

                if (mc.options.keyUse.isDown()) {
                    mc.options.keyUse.setDown(false);
                }
            } catch (Throwable ignored) {
            }
        });

        ctx.setPacketHandled(true);
    }
}