package net.kayn.unified_combat.event;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.combatroll.api.event.ServerSideRollEvents;
import net.kayn.unified_combat.UnifiedCombat;
import net.kayn.unified_combat.config.ModConfig;
import net.kayn.unified_combat.network.CancelCastPacket;
import net.kayn.unified_combat.network.NetworkHandler;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

public class RollCancelSpellHandler {

    private static final Random RANDOM = new Random();

    public static void register() {
        ServerSideRollEvents.PLAYER_START_ROLLING.register(RollCancelSpellHandler::onRoll);
    }

    private static void onRoll(ServerPlayer player, Vec3 rollVelocity) {
        if (!ModConfig.ENABLE_ROLL_CANCEL.get()) return;
        if (player == null) return;

        MagicData magic = MagicData.getPlayerMagicData(player);
        if (magic == null || !magic.getSyncedData().isCasting()) return;

        SpellData spellData = magic.getCastingSpell();
        if (spellData == null) return;

        try {
            AbstractSpell spell = spellData.getSpell();
            String spellId = spell.getSpellId();
            List<String> whitelist = ModConfig.ALLOWED_ROLL_SPELLS.get();

            if (whitelist != null && whitelist.contains(spellId)) return;

            int total = magic.getCastDuration();
            int remaining = magic.getCastDurationRemaining();
            float progress = total > 0 ? (float) (total - remaining) / (float) total : 0f;
            progress = Math.min(1f, Math.max(0f, progress));
            if (ModConfig.PANIC_ROLL_PENALTY.get()) {
                float manaCost = spell.getManaCost(spellData.getLevel());
                float minFlat = (float) ModConfig.MANA_DEDUCT_MIN_FLAT.get().doubleValue();
                float maxPercent = (float) ModConfig.MANA_DEDUCT_MAX_PERCENT.get().doubleValue();
                float penalty = Math.max(minFlat, manaCost * maxPercent * progress);
                magic.setMana(Math.max(0f, magic.getMana() - penalty));
            }

            String school = spell.getSchoolType().getId().toString();
            String sound = "entity.arrow.hit_player";
            String particle = "minecraft:campfire_cosy_smoke";

            boolean doBacklash = ModConfig.ENABLE_BACKLASH_EFFECTS.get()
                    && RANDOM.nextFloat() < (float) ModConfig.BACKLASH_EFFECT_CHANCE.get().doubleValue();

            if (doBacklash) {
                ServerLevel lvl = player.serverLevel();
                switch (school) {
                    case "irons_spellbooks:fire" -> {
                        sound = "entity.generic.extinguish_fire";
                        particle = "minecraft:flame";
                        player.setSecondsOnFire(3);
                    }
                    case "irons_spellbooks:ice" -> {
                        sound = "block.glass.break";
                        particle = "irons_spellbooks:snowflake";
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    }
                    case "irons_spellbooks:lightning" -> {
                        sound = "irons_spellbooks:entity.lightning_strike.strike";
                        particle = "minecraft:glow";
                        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(lvl);
                        if (bolt != null) {
                            bolt.setPos(player.getX(), player.getY(), player.getZ());
                            lvl.addFreshEntity(bolt);
                        }
                    }
                    case "irons_spellbooks:blood" -> {
                        sound = "irons_spellbooks:entity.blood_needle.impact";
                        particle = "irons_spellbooks:blood";
                        player.hurt(player.damageSources().magic(), 2f);
                    }
                    case "irons_spellbooks:holy" -> {
                        sound = "entity.allay.item_taken";
                        particle = "minecraft:end_rod";
                        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
                    }
                    case "irons_spellbooks:ender" -> {
                        sound = "entity.illusioner.mirror_move";
                        particle = "minecraft:reverse_portal";
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                    }
                    case "irons_spellbooks:nature" -> {
                        sound = "entity.allay.item_given";
                        particle = "irons_spellbooks:wisp";
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                    }
                    case "irons_spellbooks:evocation" -> {
                        sound = "block.respawn_anchor.deplete";
                        particle = "minecraft:entity_effect";
                        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                    }
                    default -> {
                    }
                }
            }

            if (ModConfig.ANTI_SPAM_CAST.get()) {
                int maxCd = ModConfig.MAX_ROLL_CANCEL_COOLDOWN.get();
                int minCd = ModConfig.MIN_ROLL_CANCEL_COOLDOWN.get();
                int cooldown = Math.round(maxCd - (maxCd - minCd) * progress);

                Object cooldowns = magic.getPlayerCooldowns();
                Method add = cooldowns.getClass().getMethod("addCooldown", String.class, int.class);
                Method sync = cooldowns.getClass().getMethod("syncToPlayer", ServerPlayer.class);
                add.invoke(cooldowns, spellId, cooldown);
                sync.invoke(cooldowns, player);
            }

            Utils.serverSideCancelCast(player);
            magic.resetCastingState();


            ServerLevel level = player.serverLevel();
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(sound)),
                    SoundSource.PLAYERS,
                    1.5f,
                    1.1f
            );

            ParticleOptions particleOptions = resolveParticle(particle);
            Vec3 dir = rollVelocity != null ? rollVelocity.normalize() : Vec3.ZERO;
            int particleCount = ModConfig.ROLL_PARTICLE_COUNT.get();

            for (int i = 0; i < particleCount; i++) {
                level.sendParticles(
                        particleOptions,
                        player.getX(),
                        player.getY() + 1.0,
                        player.getZ(),
                        1,
                        dir.x * RANDOM.nextDouble() * 0.3,
                        dir.y * RANDOM.nextDouble() * 0.3,
                        dir.z * RANDOM.nextDouble() * 0.3,
                        0.0
                );
            }

            NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new CancelCastPacket()
            );

        } catch (Throwable t) {
            UnifiedCombat.LOGGER.error("Roll cancel failed", t);
        }
    }

    private static ParticleOptions resolveParticle(String id) {
        try {
            ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(new ResourceLocation(id));
            if (type instanceof ParticleOptions options) return options;
        } catch (Exception ignored) {}
        return ParticleTypes.CAMPFIRE_COSY_SMOKE;
    }
}
