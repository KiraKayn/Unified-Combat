package net.kayn.unified_combat.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLE_ROLL_CANCEL;
    public static final ForgeConfigSpec.BooleanValue PANIC_ROLL_PENALTY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BACKLASH_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue ANTI_SPAM_CAST;

    public static final ForgeConfigSpec.DoubleValue MANA_DEDUCT_MIN_FLAT;
    public static final ForgeConfigSpec.DoubleValue MANA_DEDUCT_MAX_PERCENT;

    public static final ForgeConfigSpec.IntValue MIN_ROLL_CANCEL_COOLDOWN;
    public static final ForgeConfigSpec.IntValue MAX_ROLL_CANCEL_COOLDOWN;


    public static final ForgeConfigSpec.DoubleValue BACKLASH_EFFECT_CHANCE;
    public static final ForgeConfigSpec.IntValue ROLL_PARTICLE_COUNT;


    public static final ForgeConfigSpec.ConfigValue<List<String>> ALLOWED_ROLL_SPELLS;

    static {
        BUILDER.push("Unified Combat");

        ENABLE_ROLL_CANCEL = BUILDER
                .comment("Allows rolling to cancel spell casting")
                .define("enableRollCancel", true);

        PANIC_ROLL_PENALTY = BUILDER
                .comment("Deduct mana when cancelling a spell via roll")
                .define("panicRollPenalty", true);

        ENABLE_BACKLASH_EFFECTS = BUILDER
                .comment("Apply backlash effects when cancelling unstable magic")
                .define("enableBacklashEffects", true);

        ANTI_SPAM_CAST = BUILDER
                .comment("Apply cooldown to cancelled spells")
                .define("antiSpamCast", true);

        BUILDER.push("Mana");

        MANA_DEDUCT_MIN_FLAT = BUILDER
                .comment("Minimum flat mana deducted when panic rolling")
                .defineInRange("manaDeductMinFlat", 5.0, 0.0, 1000.0);

        MANA_DEDUCT_MAX_PERCENT = BUILDER
                .comment("Maximum percentage of spell mana cost deducted (scaled by cast progress)")
                .defineInRange("manaDeductMaxPercent", 0.35, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.push("Cooldowns");

        MIN_ROLL_CANCEL_COOLDOWN = BUILDER
                .comment("Minimum cooldown applied when cancelling near completion")
                .defineInRange("minRollCancelCooldown", 10, 0, 20 * 60);

        MAX_ROLL_CANCEL_COOLDOWN = BUILDER
                .comment("Maximum cooldown applied when cancelling instantly")
                .defineInRange("maxRollCancelCooldown", 60, 0, 20 * 60);

        BUILDER.pop();

        BUILDER.push("Visuals & Effects");

        BACKLASH_EFFECT_CHANCE = BUILDER
                .comment("Chance for backlash effects to occur")
                .defineInRange("backlashEffectChance", 0.75, 0.0, 1.0);

        ROLL_PARTICLE_COUNT = BUILDER
                .comment("Number of particles spawned when cancelling a cast")
                .defineInRange("rollParticleCount", 16, 0, 128);

        BUILDER.pop();

        ALLOWED_ROLL_SPELLS = BUILDER
                .comment("Spell IDs that are allowed to continue casting during a roll")
                .define("allowedRollSpells", new ArrayList<>(List.of(
                        "irons_spellbooks:magic_arrow",
                        "irons_spellbooks:root"
                )));

        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
