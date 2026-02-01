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

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = UnifiedCombat.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE)
public class RollInputHandler {


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        KeyMapping rollKey = Keybindings.roll;
        if (rollKey == null) return;
        if (event.getKey() != rollKey.getKey().getValue()) return;

        handleRollInput(event.getAction(), rollKey, event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        KeyMapping rollKey = Keybindings.roll;
        if (rollKey == null) return;
        if (event.getButton() != rollKey.getKey().getValue()) return;

        handleRollInput(event.getAction(), rollKey, event);
    }

    private static void handleRollInput(int action, KeyMapping rollKey, InputEvent event) {
        if (!ModConfig.ENABLE_ROLL_LOCK.get()) return;
        if (!ClientMagicData.isCasting()) return;

        String spellId = ClientMagicData.getCastingSpellId();
        if (spellId == null) spellId = "";

        if (ModConfig.ROLL_LOCK_WHITELIST.get().contains(spellId)) return;

        Minecraft mc = Minecraft.getInstance();


        if (action == 1) {
            int remaining = ClientMagicData.getCastDurationRemaining();

            if (remaining <= ModConfig.ROLL_LOCK_BUFFER_WINDOW.get() && remaining >= 0) {
                if (NetworkHandler.INSTANCE != null) {
                    NetworkHandler.INSTANCE.sendToServer(new RollBufferPacket(false));
                }
            } else if (ModConfig.ENABLE_ROLL_LOCK_FEEDBACK.get()) {
                mc.player.displayClientMessage(Component.literal("§cCannot roll while casting!"), true);
                mc.player.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.0f, 1.0f);
            }
        }
        rollKey.setDown(false);
        while (rollKey.consumeClick()) {
        }

        event.setCanceled(true);
    }
}
