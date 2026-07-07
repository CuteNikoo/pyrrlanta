package com.pyrrlanta.pyrrlanta.tribe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Map;

// Ore currency: iron/gold/diamonds can be deposited into a tribe's treasury
// to fund claiming land beyond the tribe's free founding claim.
public final class TribeEconomy {
    private TribeEconomy() {
    }

    public static final int CLAIM_COST = 20;

    private static final Map<Item, Integer> ORE_VALUES = Map.of(
            Items.IRON_INGOT, 1,
            Items.GOLD_INGOT, 5,
            Items.DIAMOND, 15
    );

    public static boolean isAccepted(Item item) {
        return ORE_VALUES.containsKey(item);
    }

    public static int valueOf(Item item) {
        return ORE_VALUES.getOrDefault(item, 0);
    }
}
