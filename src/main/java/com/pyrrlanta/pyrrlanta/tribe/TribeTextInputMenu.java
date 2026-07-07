package com.pyrrlanta.pyrrlanta.tribe;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

// Reuses vanilla's anvil GUI purely as a free-text input box -- no custom client rendering
// needed, AnvilScreen already exists and knows how to drive any AnvilMenu. The input slot
// is pre-filled so the rename field is immediately active (an empty input slot leaves it
// disabled in vanilla). The normal repair/XP-cost mechanics are neutralized: mayPickup
// always allows taking the result, createResult always shows a static placeholder, and
// onTake skips cost deduction entirely and just reports whatever was typed to a callback.
public class TribeTextInputMenu extends AnvilMenu {
    private final Consumer<String> onConfirm;
    private String typedName = "";

    private TribeTextInputMenu(int containerId, Inventory playerInventory, Consumer<String> onConfirm) {
        super(containerId, playerInventory);
        this.onConfirm = onConfirm;
        getSlot(INPUT_SLOT).set(new ItemStack(Items.NAME_TAG));
        getSlot(RESULT_SLOT).set(new ItemStack(Items.NAME_TAG));
    }

    public static void open(ServerPlayer player, String title, Consumer<String> onConfirm) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new TribeTextInputMenu(containerId, inventory, onConfirm),
                Component.literal(title)));
    }

    @Override
    public boolean setItemName(String itemName) {
        this.typedName = itemName;
        return true;
    }

    @Override
    public void createResult() {
        getSlot(RESULT_SLOT).set(new ItemStack(Items.NAME_TAG));
    }

    @Override
    public boolean mayPickup(Player player, boolean hasStack) {
        return hasStack;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        String typed = typedName.trim();
        player.closeContainer();
        if (!typed.isEmpty()) {
            onConfirm.accept(typed);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
