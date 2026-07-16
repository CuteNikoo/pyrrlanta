package com.pyrrlanta.pyrrlantatribes.tribe;

import net.neoforged.neoforge.common.ModConfigSpec;

public class TribeConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue CLAIM_BASE_COST = BUILDER
            .comment("Ore cost of a tribe's 2nd claim (the 1st/founding claim is always free).")
            .defineInRange("claimBaseCost", 20, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue CLAIM_COST_INCREMENT = BUILDER
            .comment("Extra ore cost added per claim the tribe already owns, so expansion gets more expensive over time.")
            .defineInRange("claimCostIncrement", 5, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MAX_CLAIMS_PER_TRIBE = BUILDER
            .comment("Maximum number of chunks a single tribe may claim. 0 = unlimited.")
            .defineInRange("maxClaimsPerTribe", 0, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue IRON_VALUE = BUILDER
            .comment("Ore value of one iron ingot. Kept low by default since iron is trivial to automate (e.g. with Create).")
            .defineInRange("ironValue", 1, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue GOLD_VALUE = BUILDER
            .comment("Ore value of one gold ingot. No easy automated ore-doubling loop for gold, so it's worth more than iron.")
            .defineInRange("goldValue", 8, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue DIAMOND_VALUE = BUILDER
            .comment("Ore value of one diamond. The hardest of the three to automate, so it's worth the most.")
            .defineInRange("diamondValue", 25, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue TAXES_ENABLED = BUILDER
            .comment("Master switch for the taxes feature. Off by default. If false, taxes never run "
                    + "server-wide even for tribes that have turned on /tribe toggle taxes true.")
            .define("taxesEnabled", false);

    public static final ModConfigSpec.IntValue TAX_PER_CLAIM = BUILDER
            .comment("Ore charged per claim, per tax cycle, for tribes that have taxes enabled (off by default).")
            .defineInRange("taxPerClaim", 2, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue TAX_INTERVAL_TICKS = BUILDER
            .comment("How often taxes are collected, in ticks. Default is 24000 (one Minecraft day).")
            .defineInRange("taxIntervalTicks", 24000, 1200, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue TIER_SYSTEM_ENABLED = BUILDER
            .comment("Master switch for the tribe tier system (tier passives, force-loading, tier-up "
                    + "announcements). On by default. If false, no tier passives apply and force-loaded "
                    + "chunks are not maintained.")
            .define("tierSystemEnabled", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private TribeConfig() {
    }
}
