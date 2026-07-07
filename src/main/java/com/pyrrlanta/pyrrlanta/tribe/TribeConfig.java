package com.pyrrlanta.pyrrlanta.tribe;

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
            .comment("Maximum number of chunks a single tribe may claim.")
            .defineInRange("maxClaimsPerTribe", 64, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue IRON_VALUE = BUILDER
            .comment("Ore value of one iron ingot. Kept low by default since iron is trivial to automate (e.g. with Create).")
            .defineInRange("ironValue", 1, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue GOLD_VALUE = BUILDER
            .comment("Ore value of one gold ingot. No easy automated ore-doubling loop for gold, so it's worth more than iron.")
            .defineInRange("goldValue", 8, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue DIAMOND_VALUE = BUILDER
            .comment("Ore value of one diamond. The hardest of the three to automate, so it's worth the most.")
            .defineInRange("diamondValue", 25, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private TribeConfig() {
    }
}
