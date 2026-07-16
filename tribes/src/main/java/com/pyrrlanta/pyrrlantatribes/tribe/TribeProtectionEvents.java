package com.pyrrlanta.pyrrlantatribes.tribe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobSpawnType;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.UUID;

// Enforces tribe claim protection: only members (or anyone, if the tribe allows public
// access) may break/place blocks or interact with blocks inside a claimed chunk.
// PvP and explosions are blocked inside claims unless the tribe has PvP enabled.
public final class TribeProtectionEvents {
    private TribeProtectionEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!canModify(level, event.getPos(), player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!canModify(level, event.getPos(), player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        // Chests/barrels/furnaces/hoppers/shulker boxes/etc all have a BlockEntity that
        // implements Container -- use that as the general test for "is this lootable",
        // gated by its own toggle instead of the general build-protection one.
        boolean isContainer = level.getBlockEntity(pos) instanceof Container;
        boolean allowed = isContainer ? canOpenContainer(level, pos, player) : canModify(level, pos, player);
        if (!allowed) {
            event.setUseBlock(TriState.FALSE);
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof Player victim)) {
            return;
        }
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        TribeSavedData data = TribeSavedData.get(level.getServer());
        ClaimPos pos = ClaimPos.of(level, victim.blockPosition());
        Tribe owner = data.getTribeAt(pos);
        if (owner != null && !owner.isPvpEnabled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        TribeSavedData data = TribeSavedData.get(level.getServer());
        event.getAffectedBlocks().removeIf(pos -> {
            Tribe owner = data.getTribeAt(ClaimPos.of(level, pos));
            return owner != null && owner.isProtectionEnabled();
        });
    }

    @SubscribeEvent
    public static void onMobSpawn(FinalizeSpawnEvent event) {
        // Only block ambient/natural hostile spawns -- not spawn eggs, breeding, dispensers, etc.
        if (event.getSpawnType() != MobSpawnType.NATURAL || !(event.getEntity() instanceof Enemy)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        TribeSavedData data = TribeSavedData.get(level.getServer());
        Tribe owner = data.getTribeAt(ClaimPos.of(level, pos));
        if (owner != null && owner.isMobSpawningBlocked()) {
            event.setSpawnCancelled(true);
        }
    }

    // Keeps a dying player's items instead of dropping them on the ground, if their tribe's
    // claim has keepInventory on. Each drop is only removed from the drop list once it's been
    // successfully placed back in the player's inventory -- Inventory#add() only mutates on a
    // full, successful placement, so a full inventory just falls back to a normal ground drop
    // for the leftover instead of silently destroying it.
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        TribeSavedData data = TribeSavedData.get(level.getServer());
        Tribe owner = data.getTribeAt(ClaimPos.of(level, player.blockPosition()));
        if (owner == null || !owner.isKeepInventory()) {
            return;
        }
        event.getDrops().removeIf(itemEntity -> player.getInventory().add(itemEntity.getItem()));
    }

    // Companion to onLivingDrops: stops XP orbs from spawning too. Note this only suppresses
    // the dropped orbs, matching the "don't lose your stuff" spirit of the toggle -- it does
    // not attempt to replicate the vanilla keepInventory gamerule's XP-level preservation.
    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        TribeSavedData data = TribeSavedData.get(level.getServer());
        Tribe owner = data.getTribeAt(ClaimPos.of(level, player.blockPosition()));
        if (owner != null && owner.isKeepInventory()) {
            event.setCanceled(true);
        }
    }

    // Claims are open to everyone by default. A tribe only becomes protected once a
    // high-ranking member turns it on with /tribe toggle protect true, at which point only
    // members and explicitly trusted outsiders may build or interact.
    private static boolean canModify(ServerLevel level, BlockPos pos, Player player) {
        TribeSavedData data = TribeSavedData.get(level.getServer());
        Tribe owner = data.getTribeAt(ClaimPos.of(level, pos));
        if (owner == null || !owner.isProtectionEnabled()) {
            return true;
        }
        UUID playerId = player.getUUID();
        return owner.isMember(playerId) || owner.isTrusted(playerId);
    }

    // Separate toggle (/tribe toggle chests true) so a tribe can protect its loot without
    // necessarily locking down general building, or vice versa.
    private static boolean canOpenContainer(ServerLevel level, BlockPos pos, Player player) {
        TribeSavedData data = TribeSavedData.get(level.getServer());
        Tribe owner = data.getTribeAt(ClaimPos.of(level, pos));
        if (owner == null || !owner.isChestProtectionEnabled()) {
            return true;
        }
        UUID playerId = player.getUUID();
        return owner.isMember(playerId) || owner.isTrusted(playerId);
    }
}
