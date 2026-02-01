package net.kayn.unified_combat.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CancelRollPacket {

    public CancelRollPacket() {}

    public static void write(CancelRollPacket pkt, FriendlyByteBuf buf) {}

    public static CancelRollPacket read(FriendlyByteBuf buf) {
        return new CancelRollPacket();
    }

    public static void handle(CancelRollPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;

            KeyMapping rollKey = net.combatroll.client.Keybindings.roll;
            if (rollKey != null) {
                rollKey.setDown(false);
                while (rollKey.consumeClick()) {}
            }

            mc.player.displayClientMessage(Component.literal("§cCannot roll while casting!"), true);

            mc.player.playSound(
                    SoundEvents.NOTE_BLOCK_BASEDRUM.value(),
                    1.0f,
                    1.0f
            );
        });
        ctx.setPacketHandled(true);
    }
}