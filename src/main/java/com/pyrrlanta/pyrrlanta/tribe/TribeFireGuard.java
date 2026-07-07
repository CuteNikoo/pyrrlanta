package com.pyrrlanta.pyrrlanta.tribe;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Fire spread has no clean cancelable event in NeoForge as of 1.21.1 (confirmed via an open,
// unresolved upstream RFC -- neoforged/NeoForge#1469), so "block fire spread" can't be true
// instant prevention the way break/place/pvp/explosion protection are. Instead, this
// periodically scans claimed chunks that have fireSpreadBlocked on and extinguishes any fire
// found. Fire can visibly exist for up to SCAN_INTERVAL_TICKS before being cleared -- a real
// but minor imperfection, not a silent failure.
public final class TribeFireGuard {
    private static final int SCAN_INTERVAL_TICKS = 40; // ~2 seconds

    private static int tickCounter = 0;

    private TribeFireGuard() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(TribeFireGuard::onServerTick);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < SCAN_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        scanAll(event.getServer());
    }

    private static void scanAll(MinecraftServer server) {
        TribeSavedData data = TribeSavedData.get(server);
        for (Tribe tribe : data.getAllTribes()) {
            if (!tribe.isFireSpreadBlocked()) {
                continue;
            }
            for (ClaimPos claim : tribe.getClaims()) {
                ServerLevel level = server.getLevel(claim.dimension());
                if (level != null) {
                    extinguish(level, claim.chunk());
                }
            }
        }
    }

    private static void extinguish(ServerLevel level, ChunkPos chunk) {
        if (!level.hasChunk(chunk.x, chunk.z)) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    pos.set(minX + x, y, minZ + z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
    }
}
