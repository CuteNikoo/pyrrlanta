package com.pyrrlanta.pyrrlanta.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

// Extends SwordItem so it's recognized as sword-like by vanilla systems (sweep attack,
// "best weapon" checks, etc.) even though Better Combat's own combo system will usually
// take over the actual attack behavior. The Tier argument barely matters here -- damage,
// speed, and range all come from the custom ItemAttributeModifiers set in ModItems, and the
// item is Unbreakable, so Tiers.NETHERITE is just a reasonable base for anything else that
// reads off the tier (e.g. block-breaking speed on cobwebs).
public class TwilightEgoWeapon extends SwordItem {
    public TwilightEgoWeapon(Item.Properties properties) {
        super(Tiers.NETHERITE, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ModItems.EGO_NAME_STYLE);
    }
}
