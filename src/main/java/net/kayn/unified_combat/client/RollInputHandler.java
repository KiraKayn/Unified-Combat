package net.kayn.unified_combat.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.combatroll.client.Keybindings;
import net.kayn.unified_combat.UnifiedCombat;
import net.kayn.unified_combat.config.ModConfig;
import net.kayn.unified_combat.network.NetworkHandler;
import net.kayn.unified_combat.network.RollBufferPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(
        modid = UnifiedCombat.MOD_ID,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT,
        bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE
)
public class RollInputHandler {


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == 0) return;
        KeyMapping rollKey = Keybindings.roll;
        if (rollKey == null) return;
        if (event.getKey() != rollKey.getKey().getValue()) return;

        handleRollInput(event, event.getAction());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == 0) return;
        KeyMapping rollKey = Keybindings.roll;
        if (rollKey == null) return;
        if (event.getButton() != rollKey.getKey().getValue()) return;

        handleRollInput(event, event.getAction());
    }

    private static void handleRollInput(InputEvent event, int action) {
        if (!ModConfig.ENABLE_ROLL_LOCK.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        boolean casting = false;
        String spellId = "";
        try {
            casting = ClientMagicData.isCasting();
            if (casting) {
                String id = ClientMagicData.getCastingSpellId();
                if (id != null) spellId = id;
            }
        } catch (Throwable ignored) {}

        boolean whitelisted = ModConfig.ROLL_LOCK_WHITELIST.get().contains(spellId);

        if (!casting || whitelisted) {
            return;
        }

        KeyMapping rollKey = Keybindings.roll;
        rollKey.setDown(false);
        while (rollKey.consumeClick()) { /* drain */ }
        event.setCanceled(true);

        if (action != 1) return;

        int bufferWindow = ModConfig.ROLL_LOCK_BUFFER_WINDOW.get();
        if (bufferWindow > 0) {
            int remaining = -1;
            try {
                remaining = ClientMagicData.getCastDurationRemaining();
            } catch (Throwable ignored) {}

            if (remaining >= 0 && remaining <= bufferWindow) {
                if (NetworkHandler.INSTANCE != null) {
                    NetworkHandler.INSTANCE.sendToServer(new RollBufferPacket(false));
                    UnifiedCombat.LOGGER.debug("[UC] Roll buffered — {} ticks left on cast", remaining);
                }
                return;
            }
        }
        if (ModConfig.ENABLE_ROLL_LOCK_FEEDBACK.get()) {
            mc.player.displayClientMessage(
                    Component.literal("§cCannot roll while casting!"), true);
            mc.player.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.0f, 1.0f);
        }
    }


    public static void cancelClientAttack() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        try {
            mc.player.resetAttackStrengthTicker();
            mc.player.stopUsingItem();
            mc.player.swinging = false;
            mc.player.swingTime = 0;
            mc.player.attackAnim = 0;

            try {
                Class<?> bcHelper = Class.forName("net.combatroll.compatibility.BetterCombatHelper");
                if ((Boolean) bcHelper.getMethod("isDoingUpswing").invoke(null)) {
                    bcHelper.getMethod("cancelUpswing").invoke(null);
                }
            } catch (ClassNotFoundException ignored) { /* Better Combat not loaded */ }

        } catch (Throwable t) {
            UnifiedCombat.LOGGER.warn("[UC] cancelClientAttack: {}", t.getMessage());
        }
    }
}