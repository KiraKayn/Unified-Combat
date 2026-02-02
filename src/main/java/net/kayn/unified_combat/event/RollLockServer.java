package net.kayn.unified_combat.event;

import net.kayn.unified_combat.config.ModConfig;
import net.combatroll.api.event.ServerSideRollEvents;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.kayn.unified_combat.network.CancelBCSwingPacket;
import net.kayn.unified_combat.network.CancelRollPacket;
import net.kayn.unified_combat.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public class RollLockServer {

    public static void registerRollStartListener() {
        ServerSideRollEvents.PLAYER_START_ROLLING.register(RollLockServer::onPlayerStartRoll);
    }

    private static void onPlayerStartRoll(ServerPlayer player, Vec3 velocity) {

        if (ModConfig.ENABLE_ROLL_LOCK.get()) {
            MagicData magic = MagicData.getPlayerMagicData(player);
            boolean isCasting = magic != null && magic.getSyncedData().isCasting();

            String spellId = "";
            if (isCasting && magic != null) {
                try {
                    SpellData spellData = magic.getCastingSpell();
                    if (spellData != null) {
                        AbstractSpell spell = spellData.getSpell();
                        if (spell != null) {
                            spellId = spell.getSpellId();
                        }
                    }
                } catch (Throwable ignored) {}
            }

            boolean whitelisted = ModConfig.ROLL_LOCK_WHITELIST.get().contains(spellId);

            if (isCasting && !whitelisted) {
                if (NetworkHandler.INSTANCE != null) {
                    NetworkHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new CancelRollPacket()
                    );
                }
                return;
            }
        }
        sendAttackCancel(player);

        player.getPersistentData().putLong("lastRollCancelTime", player.level().getGameTime());
    }

    private static void sendAttackCancel(ServerPlayer player) {
        if (!ModConfig.ENABLE_ROLL_CANCEL_ATTACK.get()) return;
        if (NetworkHandler.INSTANCE != null) {
            NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new CancelBCSwingPacket()
            );
        }
    }
}