package com.pyrrlanta.pyrrlanta.tribe;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Shows an actionbar message when a player crosses into or out of tribe territory.
// The last-known-chunk/tribe maps below are runtime-only session state (not persisted);
// they are rebuilt naturally as players move and cleared on logout.
public final class TribeMessageEvents {
    private TribeMessageEvents() {
    }

    private static final Map<UUID, ChunkPos> LAST_CHUNK = new HashMap<>();
    private static final Map<UUID, UUID> LAST_TRIBE = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }

        ChunkPos current = new ChunkPos(player.blockPosition());
        ChunkPos last = LAST_CHUNK.get(player.getUUID());
        if (current.equals(last)) {
            return;
        }
        LAST_CHUNK.put(player.getUUID(), current);

        ServerLevel level = player.serverLevel();
        TribeSavedData data = TribeSavedData.get(level.getServer());
        ClaimPos pos = new ClaimPos(level.dimension(), current);
        Tribe newOwner = data.getTribeAt(pos);
        UUID newOwnerId = newOwner == null ? null : newOwner.getId();
        UUID oldOwnerId = LAST_TRIBE.get(player.getUUID());

        if (Objects.equals(newOwnerId, oldOwnerId)) {
            return;
        }
        LAST_TRIBE.put(player.getUUID(), newOwnerId);

        if (oldOwnerId != null) {
            Tribe oldOwner = data.getTribe(oldOwnerId);
            if (oldOwner != null) {
                String msg = oldOwner.getFarewell().isEmpty()
                        ? "Leaving " + oldOwner.getName() + " territory"
                        : oldOwner.getFarewell();
                player.displayClientMessage(Component.literal(msg), true);
            }
        }
        if (newOwner != null) {
            String msg = newOwner.getGreeting().isEmpty()
                    ? "Entering " + newOwner.getName() + " territory"
                    : newOwner.getGreeting();
            player.displayClientMessage(Component.literal(msg), true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        LAST_CHUNK.remove(id);
        LAST_TRIBE.remove(id);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        TribeSavedData data = TribeSavedData.get(player.serverLevel().getServer());
        if (data.getTribeOf(player.getUUID()) != null) {
            return;
        }
        for (Tribe tribe : data.getAllTribes()) {
            if (tribe.getInvites().contains(player.getUUID())) {
                player.sendSystemMessage(Component.literal("You have a pending invite to join tribe '" + tribe.getName()
                        + "'. Use /tribe accept or /tribe deny."));
            }
        }
    }
}
