package com.pyrrlanta.pyrrlantatribes.tribe;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Tracks which players currently have an autoclaim mode armed, and which one. This is a
// transient, per-session, per-player mode -- not persisted to NBT, similar in spirit to a
// movement toggle like sprinting -- cleared on logout so it can't leak across sessions.
// The actual claiming happens in TribeMessageEvents.onPlayerTick, piggybacking on the chunk-
// change detection that's already computed there for the greeting/farewell messages.
//
// A player has at most one mode armed: claiming a chunk for your tribe and protecting it as
// admin land are mutually exclusive outcomes, so storing a mode rather than two independent
// booleans makes the ambiguous "both on" state unrepresentable instead of something
// onPlayerTick would have to arbitrate.
public final class TribeAutoClaim {
    // TRIBE: claim for your own tribe, paying from the treasury (/tribe autoclaim).
    // ADMIN:  protect as server-owned "Protected Land" (/tribe admin autoclaim), OP only.
    public enum Mode {
        TRIBE,
        ADMIN
    }

    private static final Map<UUID, Mode> MODES = new HashMap<>();

    private TribeAutoClaim() {
    }

    // A null mode disarms autoclaim entirely.
    public static void setMode(UUID playerId, Mode mode) {
        if (mode == null) {
            MODES.remove(playerId);
        } else {
            MODES.put(playerId, mode);
        }
    }

    // Returns null when the player has no autoclaim armed.
    public static Mode getMode(UUID playerId) {
        return MODES.get(playerId);
    }

    public static void clear(UUID playerId) {
        MODES.remove(playerId);
    }
}
