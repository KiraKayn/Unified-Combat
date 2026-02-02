package net.kayn.unified_combat.client;

import net.combatroll.client.Keybindings;
import net.kayn.unified_combat.UnifiedCombat;
import net.kayn.unified_combat.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Method;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(
        modid = UnifiedCombat.MOD_ID,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT,
        bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE
)
public class AttackRollInputHandler {


    private static boolean rollBuffered = false;
    private static int bufferedRollTicksRemaining = 0;

    private static Method isDoingUpswingMethod = null;
    private static Method getUpswingTicksMethod = null;

    private static long lastAttackBlockedGameTime = -1;

    static {
        try {
            Class<?> betterCombatHelperClass = Class.forName("net.combatroll.compatibility.BetterCombatHelper");
            isDoingUpswingMethod = betterCombatHelperClass.getMethod("isDoingUpswing");
            getUpswingTicksMethod = betterCombatHelperClass.getMethod("getUpswingTicks");
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (shouldBlockRoll()) {
            KeyMapping rollKey = Minecraft.getInstance().options.keyShift;

            if (rollKey.isDown()) {
                rollKey.setDown(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == 0) return;
        KeyMapping rollKey = Keybindings.roll;
        if (rollKey == null) return;
        if (event.getButton() != rollKey.getKey().getValue()) return;

        if (handleAttackRollInput(event.getAction())) {
            event.setCanceled(true);
        }
    }


    private static boolean handleAttackRollInput(int action) {
        if (!ModConfig.ENABLE_ATTACK_ROLL_LOCK.get()) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return false;

        boolean inUpswing = false;
        int upswingTicksRemaining = -1;

        if (isDoingUpswingMethod != null) {
            try {
                Object swinging = isDoingUpswingMethod.invoke(null);
                if (swinging instanceof Boolean) inUpswing = (Boolean) swinging;
                if (inUpswing && getUpswingTicksMethod != null) {
                    Object ticks = getUpswingTicksMethod.invoke(null);
                    if (ticks instanceof Integer) upswingTicksRemaining = (Integer) ticks;
                }
            } catch (Throwable t) {
                inUpswing = false;
            }
        }

        if (!inUpswing) {
            try {
                if (mc.player.swinging || mc.player.swingTime > 0 || mc.player.attackAnim > 0) {
                    inUpswing = true;
                }
            } catch (Throwable ignored) {}
        }

        if (!inUpswing) return false;

        KeyMapping rollKey = Keybindings.roll;
        if (rollKey != null) {
            rollKey.setDown(false);
            while (rollKey.consumeClick()) {}
        }

        if (action != 1) return true;

        int bufferWindow = ModConfig.ATTACK_BUFFER_WINDOW_TICKS.get();
        if (bufferWindow > 0 && upswingTicksRemaining >= 0 && upswingTicksRemaining <= bufferWindow && !rollBuffered) {
            rollBuffered = true;
            bufferedRollTicksRemaining = upswingTicksRemaining;
            return true;
        }

        if (ModConfig.ENABLE_ATTACK_ROLL_FEEDBACK.get()) {
            mc.player.displayClientMessage(Component.literal("§cCannot roll while attacking!"), true);
            mc.player.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.0f, 1.0f);

            try {
                if (mc.level != null) lastAttackBlockedGameTime = mc.level.getGameTime();
            } catch (Throwable ignored) {}
        }

        return true;
    }

    private static boolean shouldBlockRoll() {
        if (!ModConfig.ENABLE_ATTACK_ROLL_LOCK.get()) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return false;

        boolean inUpswing = false;
        if (isDoingUpswingMethod != null) {
            try {
                Object swinging = isDoingUpswingMethod.invoke(null);
                if (swinging instanceof Boolean) inUpswing = (Boolean) swinging;
            } catch (Throwable ignored) {}
        }

        if (!inUpswing) {
            try {
                if (mc.player.swinging || mc.player.swingTime > 0 || mc.player.attackAnim > 0) {
                    inUpswing = true;
                }
            } catch (Throwable ignored) {}
        }

        return inUpswing;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!rollBuffered) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            rollBuffered = false;
            bufferedRollTicksRemaining = 0;
            return;
        }

        bufferedRollTicksRemaining = Math.max(0, bufferedRollTicksRemaining - 1);

        boolean stillSwinging = false;
        if (isDoingUpswingMethod != null) {
            try {
                Object res = isDoingUpswingMethod.invoke(null);
                if (res instanceof Boolean) stillSwinging = (Boolean) res;
            } catch (Throwable ignored) {
                stillSwinging = false;
            }
        }

        if (!stillSwinging) {
            try {
                if (mc.player.swinging || mc.player.swingTime > 0 || mc.player.attackAnim > 0) {
                    stillSwinging = true;
                }
            } catch (Throwable ignored) {
                stillSwinging = false;
            }
        }
        if (!stillSwinging || bufferedRollTicksRemaining <= 0) {
            boolean shouldFire = !stillSwinging;
            rollBuffered = false;
            bufferedRollTicksRemaining = 0;

            if (shouldFire) {
                KeyMapping rollKey = Keybindings.roll;
                if (rollKey != null) {
                    rollKey.setDown(true);
                    mc.execute(() -> {
                        try { rollKey.setDown(false); } catch (Throwable ignored) {}
                    });
                }
            }
        }
    }

    public static boolean wasAttackBlockedThisTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return false;
        try {
            return mc.level.getGameTime() == lastAttackBlockedGameTime;
        } catch (Throwable ignored) {
            return false;
        }
    }
}