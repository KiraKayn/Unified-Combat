package net.kayn.unified_combat.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class RollBufferPacket {
    private final boolean execute;

    public RollBufferPacket(boolean execute) {
        this.execute = execute;
    }

    public static void write(RollBufferPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.execute);
    }

    public static RollBufferPacket read(FriendlyByteBuf buf) {
        boolean exec = buf.readBoolean();
        return new RollBufferPacket(exec);
    }

    public static void handle(RollBufferPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (pkt.execute) {
            ctx.enqueueWork(() -> {
                try {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc == null || mc.player == null) return;
                    KeyMapping rollKey = net.combatroll.client.Keybindings.roll;
                    if (rollKey == null) return;
                    rollKey.setDown(true);
                    mc.execute(() -> {
                        try {
                            rollKey.setDown(false);
                        } catch (Throwable ignored) {}
                    });
                } catch (Throwable ignored) {}
            });
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            try {
                ServerPlayer sender = ctx.getSender();
                if (sender == null) return;
                sender.getPersistentData().putBoolean("wantsToRoll", true);
            } catch (Throwable ignored) {}
        });
        ctx.setPacketHandled(true);
    }
}
