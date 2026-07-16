package com.pyrrlanta.pyrrlantatribes.tribe;

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

// Per-observer entity glow, achieved without touching the target's real state and without
// requiring anything on the client (this mod is server-side, and players may be on vanilla).
//
// Every entity syncs a "shared flags" byte (data field 0) whose bit 6 is the glowing flag,
// which clients render as an outline drawn through terrain. We send a hand-built
// ClientboundSetEntityDataPacket carrying just that byte -- with the glow bit forced on -- to
// only the observers who should see the target glowing. The target's server-side
// SynchedEntityData is never modified, so nobody else sees any change; the flip side is that
// a genuine data update from the entity will overwrite our override on the observer's client,
// so callers re-send periodically and send an explicit "off" packet to revert cleanly.
public final class TribeGlow {
    private static final int GLOWING_FLAG_BIT = 6; // shared-flags bit 6 == glowing
    // Shared flags is always data field id 0 with the BYTE serializer. Rebuilding the accessor
    // (rather than reaching for Entity's protected DATA_SHARED_FLAGS_ID) is fine: values are
    // looked up and serialized by accessor id.
    private static final EntityDataAccessor<Byte> SHARED_FLAGS =
            new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);

    private TribeGlow() {
    }

    // Show `target` glowing to `observer` only, preserving the target's other shared flags
    // (sneaking/sprinting/etc.) so the observer's view stays otherwise accurate.
    public static void showGlow(ServerPlayer observer, ServerPlayer target) {
        byte flags = target.getEntityData().get(SHARED_FLAGS);
        sendSharedFlags(observer, target.getId(), (byte) (flags | (1 << GLOWING_FLAG_BIT)));
    }

    // Revert `observer`'s view of `target` to the target's real shared flags (i.e. no forced
    // glow). Call this when the target should no longer appear glowing to this observer.
    public static void clearGlow(ServerPlayer observer, ServerPlayer target) {
        sendSharedFlags(observer, target.getId(), target.getEntityData().get(SHARED_FLAGS));
    }

    private static void sendSharedFlags(ServerPlayer observer, int targetId, byte flags) {
        List<SynchedEntityData.DataValue<?>> values =
                List.of(SynchedEntityData.DataValue.create(SHARED_FLAGS, flags));
        observer.connection.send(new ClientboundSetEntityDataPacket(targetId, values));
    }
}
