package com.pyrrlanta.pyrrlantatribes.tribe;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

// Periodic ore upkeep for tribes that have taxes enabled (off by default -- see
// Tribe.taxesEnabled / /tribe toggle taxes). Never lets a tribe's treasury go negative: if it
// can't fully cover the bill, whatever is available is taken and online members are
// notified, rather than the tribe going into debt or automatically losing claims.
public final class TribeTaxCollector {
    private static int tickCounter = 0;

    private TribeTaxCollector() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(TribeTaxCollector::onServerTick);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (!TribeConfig.TAXES_ENABLED.get()) {
            return;
        }
        tickCounter++;
        if (tickCounter < TribeConfig.TAX_INTERVAL_TICKS.get()) {
            return;
        }
        tickCounter = 0;
        collect(event.getServer());
    }

    private static void collect(MinecraftServer server) {
        TribeSavedData data = TribeSavedData.get(server);
        long perClaim = TribeConfig.TAX_PER_CLAIM.get();
        for (Tribe tribe : data.getAllTribes()) {
            if (!tribe.isTaxesEnabled()) {
                continue;
            }
            long bill = perClaim * tribe.getClaims().size();
            if (bill <= 0) {
                continue;
            }
            long available = tribe.getTreasury();
            long paid = Math.min(bill, available);
            tribe.setTreasury(available - paid);
            data.setDirty();
            if (paid < bill) {
                notifyMembers(server, tribe, "Taxes: only paid " + paid + "/" + bill
                        + " ore -- treasury is empty. Deposit more with /tribe deposit.");
            } else {
                notifyMembers(server, tribe, "Taxes: paid " + paid + " ore. Treasury: " + tribe.getTreasury() + ".");
            }
        }
    }

    private static void notifyMembers(MinecraftServer server, Tribe tribe, String message) {
        for (UUID memberId : tribe.getMembers().keySet()) {
            ServerPlayer online = server.getPlayerList().getPlayer(memberId);
            if (online != null) {
                online.sendSystemMessage(Component.literal("[" + tribe.getName() + "] " + message));
            }
        }
    }
}
