package net.kayn.unified_combat.event;

import net.kayn.unified_combat.config.ModConfig;
import net.combatroll.api.event.ServerSideRollEvents;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.kayn.unified_combat.network.NetworkHandler;
import net.kayn.unified_combat.network.CancelRollPacket;

public class RollLockServer {

    public static void registerRollStartListener() {
        ServerSideRollEvents.PLAYER_START_ROLLING.register(RollLockServer::onPlayerStartRoll);
    }

    private static boolean onPlayerStartRoll(ServerPlayer player, net.minecraft.world.phys.Vec3 velocity) {
        if (!ModConfig.ENABLE_ROLL_LOCK.get()) return true;

        MagicData magic = MagicData.getPlayerMagicData(player);
        if (magic == null || !magic.getSyncedData().isCasting()) return true;

        String spellId = magic.getCastingSpellId();
        if (spellId == null) spellId = "";

        if (ModConfig.ROLL_LOCK_WHITELIST.get() != null &&
                ModConfig.ROLL_LOCK_WHITELIST.get().contains(spellId)) {
            return true;
        }

        if (NetworkHandler.INSTANCE != null) {
            NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new CancelRollPacket()
            );
        }

        return false;
    }
}