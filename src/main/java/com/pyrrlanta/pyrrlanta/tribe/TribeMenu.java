package com.pyrrlanta.pyrrlanta.tribe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

// A read-mostly "GUI" for a tribe. Reuses vanilla's generic 3-row chest menu/screen
// (MenuType.GENERIC_9x3) so no custom client-side rendering code is needed at all -- the
// client already knows how to draw any ChestMenu. This isn't real item storage: clicks on
// the top 27 slots are intercepted server-side and turned into tribe actions instead of
// moving items, and quickMoveStack is disabled entirely so nothing can be shift-clicked out.
public class TribeMenu extends ChestMenu {
    private static final int SIZE = 27;

    private final Tribe tribe;
    private final TribeSavedData data;
    private final ServerPlayer viewer;

    private TribeMenu(int containerId, Inventory playerInventory, ServerPlayer viewer, Tribe tribe, TribeSavedData data) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, new SimpleContainer(SIZE), 3);
        this.tribe = tribe;
        this.data = data;
        this.viewer = viewer;
        populate();
    }

    public static void openFor(ServerPlayer player, Tribe tribe, TribeSavedData data) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new TribeMenu(containerId, inventory, player, tribe, data),
                Component.literal(tribe.getName())));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < SIZE) {
            handleClick(slotId);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private void handleClick(int slotId) {
        switch (slotId) {
            case 19 -> toggle("Protection", tribe.isProtectionEnabled(), tribe::setProtectionEnabled);
            case 20 -> toggle("PvP", tribe.isPvpEnabled(), tribe::setPvpEnabled);
            case 21 -> toggle("Mob spawning blocked", tribe.isMobSpawningBlocked(), tribe::setMobSpawningBlocked);
            case 22 -> toggle("Fire spread blocked", tribe.isFireSpreadBlocked(), tribe::setFireSpreadBlocked);
            case 23 -> toggle("Keep inventory", tribe.isKeepInventory(), tribe::setKeepInventory);
            case 24 -> toggle("Open membership", tribe.isOpen(), tribe::setOpen);
            default -> {
            }
        }
    }

    private void toggle(String label, boolean current, Consumer<Boolean> setter) {
        if (!tribe.hasPermission(viewer.getUUID(), TribeRole.OFFICER)) {
            viewer.sendSystemMessage(Component.literal("Only officers and the leader can change tribe settings."));
            return;
        }
        setter.accept(!current);
        data.setDirty();
        populate();
        broadcastChanges();
    }

    private void populate() {
        setSlot(4, named(Items.BOOK, tribe.getName() + " - Leader: " + leaderName()));
        setSlot(10, named(Items.PLAYER_HEAD, "Members: " + tribe.getMembers().size()));
        setSlot(12, named(Items.GRASS_BLOCK, "Claims: " + tribe.getClaims().size() + " / " + TribeConfig.MAX_CLAIMS_PER_TRIBE.get()));
        setSlot(14, named(Items.DIAMOND, "Treasury: " + tribe.getTreasury() + " ore"));

        setSlot(19, toggleIcon("Protection", tribe.isProtectionEnabled()));
        setSlot(20, toggleIcon("PvP", tribe.isPvpEnabled()));
        setSlot(21, toggleIcon("Mob spawning blocked", tribe.isMobSpawningBlocked()));
        setSlot(22, toggleIcon("Fire spread blocked", tribe.isFireSpreadBlocked()));
        setSlot(23, toggleIcon("Keep inventory", tribe.isKeepInventory()));
        setSlot(24, toggleIcon("Open membership", tribe.isOpen()));
    }

    private void setSlot(int index, ItemStack stack) {
        getSlot(index).set(stack);
    }

    private String leaderName() {
        ServerPlayer online = viewer.serverLevel().getServer().getPlayerList().getPlayer(tribe.getLeader());
        return online != null ? online.getName().getString() : tribe.getLeader().toString();
    }

    private static ItemStack toggleIcon(String label, boolean on) {
        ItemStack stack = new ItemStack(on ? Items.LIME_DYE : Items.GRAY_DYE);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(label + ": " + (on ? "ON" : "OFF") + " (click to toggle)"));
        return stack;
    }

    private static ItemStack named(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }
}
