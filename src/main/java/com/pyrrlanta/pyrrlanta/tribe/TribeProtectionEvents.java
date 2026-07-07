package com.pyrrlanta.pyrrlanta.tribe;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
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
        if (!canModify(level, event.getPos(), player)) {
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

    // Claims are open to everyone by default. A tribe only becomes protected once a
    // high-ranking member turns it on with /tribe set protect true, at which point only
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
}
