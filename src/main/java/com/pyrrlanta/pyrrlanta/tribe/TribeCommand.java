package com.pyrrlanta.pyrrlanta.tribe;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.UUID;
import java.util.stream.Collectors;

public final class TribeCommand {
    private static final Component NOT_IN_TRIBE = Component.literal("You are not in a tribe.");

    private TribeCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tribe")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("disband")
                        .executes(ctx -> disband(ctx.getSource())))
                .then(Commands.literal("claim")
                        .executes(ctx -> claim(ctx.getSource())))
                .then(Commands.literal("unclaim")
                        .executes(ctx -> unclaim(ctx.getSource())))
                .then(Commands.literal("deposit")
                        .executes(ctx -> deposit(ctx.getSource(), -1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> deposit(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")))))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> invite(ctx.getSource(), resolveProfile(ctx, "player")))))
                .then(Commands.literal("accept")
                        .executes(ctx -> accept(ctx.getSource())))
                .then(Commands.literal("deny")
                        .executes(ctx -> deny(ctx.getSource())))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> kick(ctx.getSource(), resolveProfile(ctx, "player")))))
                .then(Commands.literal("leave")
                        .executes(ctx -> leave(ctx.getSource())))
                .then(Commands.literal("promote")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> setRole(ctx.getSource(), resolveProfile(ctx, "player"), true))))
                .then(Commands.literal("demote")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> setRole(ctx.getSource(), resolveProfile(ctx, "player"), false))))
                .then(Commands.literal("transfer")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> transfer(ctx.getSource(), resolveProfile(ctx, "player")))))
                .then(Commands.literal("trust")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> trust(ctx.getSource(), resolveProfile(ctx, "player")))))
                .then(Commands.literal("untrust")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> untrust(ctx.getSource(), resolveProfile(ctx, "player")))))
                .then(Commands.literal("sethome")
                        .executes(ctx -> setHome(ctx.getSource())))
                .then(Commands.literal("home")
                        .executes(ctx -> home(ctx.getSource())))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource(), null))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> info(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("map")
                        .executes(ctx -> map(ctx.getSource())))
                .then(Commands.literal("set")
                        .then(Commands.literal("greeting")
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(ctx -> setGreeting(ctx.getSource(), StringArgumentType.getString(ctx, "message")))))
                        .then(Commands.literal("farewell")
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(ctx -> setFarewell(ctx.getSource(), StringArgumentType.getString(ctx, "message")))))
                        .then(Commands.literal("pvp")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> setPvp(ctx.getSource(), BoolArgumentType.getBool(ctx, "value")))))
                        .then(Commands.literal("protect")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> setProtect(ctx.getSource(), BoolArgumentType.getBool(ctx, "value"))))))
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> adminDelete(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))))
        );
    }

    private static TribeSavedData data(CommandSourceStack source) {
        return TribeSavedData.get(source.getServer());
    }

    private static GameProfile resolveProfile(CommandContext<CommandSourceStack> ctx, String argName) throws CommandSyntaxException {
        return GameProfileArgument.getGameProfiles(ctx, argName).iterator().next();
    }

    private static String playerName(CommandSourceStack source, UUID id) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(id);
        return online != null ? online.getName().getString() : id.toString();
    }

    private static void notifyIfOnline(CommandSourceStack source, UUID id, String message) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(id);
        if (online != null) {
            online.sendSystemMessage(Component.literal(message));
        }
    }

    private static int create(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        if (data.getTribeOf(player.getUUID()) != null) {
            source.sendFailure(Component.literal("You are already in a tribe."));
            return 0;
        }
        if (!name.matches("[A-Za-z0-9_]{3,16}")) {
            source.sendFailure(Component.literal("Tribe names must be 3-16 letters, numbers, or underscores."));
            return 0;
        }
        if (data.getTribeByName(name) != null) {
            source.sendFailure(Component.literal("A tribe named '" + name + "' already exists."));
            return 0;
        }
        data.createTribe(name, player.getUUID());
        source.sendSuccess(() -> Component.literal("Founded tribe '" + name + "'. Use /tribe claim to claim your first (free) chunk."), true);
        return 1;
    }

    private static int disband(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasPermission(player.getUUID(), TribeRole.LEADER)) {
            source.sendFailure(Component.literal("Only the leader can disband the tribe."));
            return 0;
        }
        String name = tribe.getName();
        data.deleteTribe(tribe);
        source.sendSuccess(() -> Component.literal("Disbanded tribe '" + name + "'."), true);
        return 1;
    }

    private static int claim(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasPermission(player.getUUID(), TribeRole.OFFICER)) {
            source.sendFailure(Component.literal("Only officers and the leader can claim land."));
            return 0;
        }
        ClaimPos pos = ClaimPos.of(player.serverLevel(), player.blockPosition());
        Tribe existing = data.getTribeAt(pos);
        if (existing != null) {
            source.sendFailure(Component.literal(existing == tribe
                    ? "This chunk is already claimed by your tribe."
                    : "This chunk is already claimed by " + existing.getName() + "."));
            return 0;
        }
        if (tribe.getClaims().size() >= TribeSavedData.MAX_CLAIMS_PER_TRIBE) {
            source.sendFailure(Component.literal("Your tribe has reached the claim limit (" + TribeSavedData.MAX_CLAIMS_PER_TRIBE + ")."));
            return 0;
        }

        boolean foundingClaim = tribe.getClaims().isEmpty();
        if (!foundingClaim) {
            if (!data.isAdjacent(tribe, pos)) {
                source.sendFailure(Component.literal("New claims must be adjacent to your tribe's existing territory."));
                return 0;
            }
            if (tribe.getTreasury() < TribeEconomy.CLAIM_COST) {
                source.sendFailure(Component.literal("Not enough ore currency. Claiming costs " + TribeEconomy.CLAIM_COST
                        + ", your tribe has " + tribe.getTreasury() + ". Use /tribe deposit."));
                return 0;
            }
            tribe.setTreasury(tribe.getTreasury() - TribeEconomy.CLAIM_COST);
        }

        data.claim(tribe, pos);
        source.sendSuccess(() -> Component.literal("Claimed chunk " + pos.chunk().x + ", " + pos.chunk().z + " for " + tribe.getName()
                + (foundingClaim ? " (founding claim, free)." : " for " + TribeEconomy.CLAIM_COST + " ore currency.")), true);
        return 1;
    }

    private static int unclaim(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasPermission(player.getUUID(), TribeRole.OFFICER)) {
            source.sendFailure(Component.literal("Only officers and the leader can unclaim land."));
            return 0;
        }
        ClaimPos pos = ClaimPos.of(player.serverLevel(), player.blockPosition());
        if (!data.unclaim(tribe, pos)) {
            source.sendFailure(Component.literal("Your tribe does not own this chunk."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Unclaimed chunk " + pos.chunk().x + ", " + pos.chunk().z + "."), true);
        return 1;
    }

    private static int deposit(CommandSourceStack source, int amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !TribeEconomy.isAccepted(held.getItem())) {
            source.sendFailure(Component.literal("Hold iron ingots, gold ingots, or diamonds in your main hand to deposit them."));
            return 0;
        }
        int toDeposit = amount < 0 ? held.getCount() : Math.min(amount, held.getCount());
        if (toDeposit <= 0) {
            source.sendFailure(Component.literal("Nothing to deposit."));
            return 0;
        }
        String itemName = held.getItem().getDescription().getString();
        int value = TribeEconomy.valueOf(held.getItem()) * toDeposit;
        held.shrink(toDeposit);
        tribe.setTreasury(tribe.getTreasury() + value);
        data.setDirty();
        source.sendSuccess(() -> Component.literal("Deposited " + toDeposit + " " + itemName + " for " + value
                + " ore currency. Treasury: " + tribe.getTreasury() + "."), true);
        return 1;
    }

    private static int invite(CommandSourceStack source, GameProfile target) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasPermission(player.getUUID(), TribeRole.OFFICER)) {
            source.sendFailure(Component.literal("Only officers and the leader can invite players."));
            return 0;
        }
        if (tribe.isMember(target.getId())) {
            source.sendFailure(Component.literal(target.getName() + " is already in your tribe."));
            return 0;
        }
        if (data.getTribeOf(target.getId()) != null) {
            source.sendFailure(Component.literal(target.getName() + " is already in another tribe."));
            return 0;
        }
        tribe.getInvites().add(target.getId());
        data.setDirty();
        source.sendSuccess(() -> Component.literal("Invited " + target.getName() + " to " + tribe.getName() + "."), true);
        notifyIfOnline(source, target.getId(), player.getName().getString() + " invited you to join tribe '" + tribe.getName()
                + "'. Use /tribe accept or /tribe deny.");
        return 1;
    }

    private static int accept(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        if (data.getTribeOf(player.getUUID()) != null) {
            source.sendFailure(Component.literal("You are already in a tribe."));
            return 0;
        }
        Tribe invitingTribe = null;
        for (Tribe t : data.getAllTribes()) {
            if (t.getInvites().contains(player.getUUID())) {
                invitingTribe = t;
                break;
            }
        }
        if (invitingTribe == null) {
            source.sendFailure(Component.literal("You have no pending tribe invites."));
            return 0;
        }
        for (Tribe t : data.getAllTribes()) {
            t.getInvites().remove(player.getUUID());
        }
        data.addMember(invitingTribe, player.getUUID(), TribeRole.MEMBER);
        Tribe joined = invitingTribe;
        source.sendSuccess(() -> Component.literal("You joined tribe '" + joined.getName() + "'."), true);
        return 1;
    }

    private static int deny(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        boolean any = false;
        for (Tribe t : data.getAllTribes()) {
            if (t.getInvites().remove(player.getUUID())) {
                any = true;
            }
        }
        if (!any) {
            source.sendFailure(Component.literal("You have no pending tribe invites."));
            return 0;
        }
        data.setDirty();
        source.sendSuccess(() -> Component.literal("Declined all pending tribe invites."), false);
        return 1;
    }

    private static int kick(CommandSourceStack source, GameProfile target) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasPermission(player.getUUID(), TribeRole.LEADER)) {
            source.sendFailure(Component.literal("Only the leader can kick members."));
            return 0;
        }
        if (!tribe.isMember(target.getId())) {
            source.sendFailure(Component.literal(target.getName() + " is not in your tribe."));
            return 0;
        }
        if (target.getId().equals(tribe.getLeader())) {
            source.sendFailure(Component.literal("You cannot kick yourself. Use /tribe disband or /tribe transfer."));
            return 0;
        }
        data.removeMember(tribe, target.getId());
        source.sendSuccess(() -> Component.literal("Kicked " + target.getName() + " from " + tribe.getName() + "."), true);
        notifyIfOnline(source, target.getId(), "You were removed from tribe '" + tribe.getName() + "'.");
        return 1;
    }

    private static int leave(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (player.getUUID().equals(tribe.getLeader())) {
            source.sendFailure(Component.literal("The leader cannot leave. Use /tribe transfer <player> first, or /tribe disband."));
            return 0;
        }
        data.removeMember(tribe, player.getUUID());
        source.sendSuccess(() -> Component.literal("You left tribe '" + tribe.getName() + "'."), true);
        return 1;
    }

    private static int setRole(CommandSourceStack source, GameProfile target, boolean promote) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasPermission(player.getUUID(), TribeRole.LEADER)) {
            source.sendFailure(Component.literal("Only the leader can change member ranks."));
            return 0;
        }
        if (!tribe.isMember(target.getId()) || target.getId().equals(tribe.getLeader())) {
            source.sendFailure(Component.literal("Cannot change that player's rank."));
            return 0;
        }
        TribeRole current = tribe.roleOf(target.getId());
        TribeRole next = promote ? TribeRole.OFFICER : TribeRole.MEMBER;
        if (current == next) {
            source.sendFailure(Component.literal(target.getName() + " is already " + (promote ? "an officer." : "a member.")));
            return 0;
        }
        tribe.getMembers().put(target.getId(), next);
        data.setDirty();
        source.sendSuccess(() -> Component.literal(target.getName() + " is now " + (next == TribeRole.OFFICER ? "an officer." : "a member.")), true);
        return 1;
    }

    private static int transfer(CommandSourceStack source, GameProfile target) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasPermission(player.getUUID(), TribeRole.LEADER)) {
            source.sendFailure(Component.literal("Only the leader can transfer leadership."));
            return 0;
        }
        if (!tribe.isMember(target.getId())) {
            source.sendFailure(Component.literal(target.getName() + " is not in your tribe."));
            return 0;
        }
        tribe.getMembers().put(player.getUUID(), TribeRole.OFFICER);
        tribe.getMembers().put(target.getId(), TribeRole.LEADER);
        tribe.setLeader(target.getId());
        data.setDirty();
        source.sendSuccess(() -> Component.literal("Transferred leadership of '" + tribe.getName() + "' to " + target.getName() + "."), true);
        return 1;
    }

    private static int trust(CommandSourceStack source, GameProfile target) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasPermission(player.getUUID(), TribeRole.OFFICER)) {
            source.sendFailure(Component.literal("Only officers and the leader can manage trusted players."));
            return 0;
        }
        if (tribe.isMember(target.getId())) {
            source.sendFailure(Component.literal(target.getName() + " is already a member."));
            return 0;
        }
        if (!tribe.getTrusted().add(target.getId())) {
            source.sendFailure(Component.literal(target.getName() + " is already trusted."));
            return 0;
        }
        data.setDirty();
        source.sendSuccess(() -> Component.literal(target.getName() + " is now trusted in " + tribe.getName() + "'s territory."), true);
        return 1;
    }

    private static int untrust(CommandSourceStack source, GameProfile target) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasPermission(player.getUUID(), TribeRole.OFFICER)) {
            source.sendFailure(Component.literal("Only officers and the leader can manage trusted players."));
            return 0;
        }
        if (!tribe.getTrusted().remove(target.getId())) {
            source.sendFailure(Component.literal(target.getName() + " was not trusted."));
            return 0;
        }
        data.setDirty();
        source.sendSuccess(() -> Component.literal(target.getName() + " is no longer trusted."), true);
        return 1;
    }

    private static int setHome(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasPermission(player.getUUID(), TribeRole.OFFICER)) {
            source.sendFailure(Component.literal("Only officers and the leader can set the tribe home."));
            return 0;
        }
        ClaimPos here = ClaimPos.of(player.serverLevel(), player.blockPosition());
        Tribe owner = data.getTribeAt(here);
        if (owner != tribe) {
            source.sendFailure(Component.literal("You must be standing in your tribe's territory to set the home."));
            return 0;
        }
        tribe.setHome(player.serverLevel().dimension(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        data.setDirty();
        source.sendSuccess(() -> Component.literal("Tribe home set."), true);
        return 1;
    }

    private static int home(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasHome()) {
            source.sendFailure(Component.literal("Your tribe has not set a home yet. Use /tribe sethome."));
            return 0;
        }
        ServerLevel level = source.getServer().getLevel(tribe.getHomeDimension());
        if (level == null) {
            source.sendFailure(Component.literal("Tribe home dimension is not loaded."));
            return 0;
        }
        player.teleportTo(level, tribe.getHomeX(), tribe.getHomeY(), tribe.getHomeZ(), tribe.getHomeYaw(), tribe.getHomePitch());
        source.sendSuccess(() -> Component.literal("Teleported to tribe home."), false);
        return 1;
    }

    private static int info(CommandSourceStack source, String name) throws CommandSyntaxException {
        TribeSavedData data = data(source);
        Tribe tribe;
        if (name != null) {
            tribe = data.getTribeByName(name);
            if (tribe == null) {
                source.sendFailure(Component.literal("No tribe named '" + name + "'."));
                return 0;
            }
        } else {
            ServerPlayer player = source.getPlayerOrException();
            tribe = data.getTribeOf(player.getUUID());
            if (tribe == null) {
                source.sendFailure(Component.literal("You are not in a tribe. Use /tribe info <name> to look up another tribe."));
                return 0;
            }
        }
        Tribe t = tribe;
        source.sendSuccess(() -> Component.literal(
                "== " + t.getName() + " ==\n"
                        + "Leader: " + playerName(source, t.getLeader()) + "\n"
                        + "Members: " + t.getMembers().size() + "\n"
                        + "Claims: " + t.getClaims().size() + "\n"
                        + "Treasury: " + t.getTreasury() + " ore currency\n"
                        + "Protected: " + (t.isProtectionEnabled() ? "yes" : "no") + "\n"
                        + "Trusted outsiders: " + t.getTrusted().size() + "\n"
                        + "PvP: " + (t.isPvpEnabled() ? "enabled" : "disabled")
        ), false);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        TribeSavedData data = data(source);
        if (data.getAllTribes().isEmpty()) {
            source.sendSuccess(() -> Component.literal("There are no tribes yet."), false);
            return 1;
        }
        String names = data.getAllTribes().stream()
                .map(t -> t.getName() + " (" + t.getMembers().size() + ")")
                .collect(Collectors.joining(", "));
        source.sendSuccess(() -> Component.literal("Tribes: " + names), false);
        return 1;
    }

    private static int map(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        ChunkPos center = new ChunkPos(player.blockPosition());
        var dimension = player.serverLevel().dimension();
        int radius = 4;
        StringBuilder sb = new StringBuilder("Tribe map (you are [+], CAPS = protected):\n");
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx == 0 && dz == 0) {
                    sb.append("[+]");
                    continue;
                }
                ClaimPos pos = new ClaimPos(dimension, new ChunkPos(center.x + dx, center.z + dz));
                Tribe owner = data.getTribeAt(pos);
                if (owner == null) {
                    sb.append(" . ");
                } else {
                    char letter = owner.getName().charAt(0);
                    char shown = owner.isProtectionEnabled() ? Character.toUpperCase(letter) : Character.toLowerCase(letter);
                    sb.append(" ").append(shown).append(" ");
                }
            }
            sb.append("\n");
        }
        String result = sb.toString();
        source.sendSuccess(() -> Component.literal(result), false);
        return 1;
    }

    private static int setGreeting(CommandSourceStack source, String message) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasPermission(player.getUUID(), TribeRole.OFFICER)) {
            source.sendFailure(Component.literal("Only officers and the leader can change tribe settings."));
            return 0;
        }
        tribe.setGreeting(message);
        data.setDirty();
        source.sendSuccess(() -> Component.literal("Greeting message updated."), true);
        return 1;
    }

    private static int setFarewell(CommandSourceStack source, String message) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasPermission(player.getUUID(), TribeRole.OFFICER)) {
            source.sendFailure(Component.literal("Only officers and the leader can change tribe settings."));
            return 0;
        }
        tribe.setFarewell(message);
        data.setDirty();
        source.sendSuccess(() -> Component.literal("Farewell message updated."), true);
        return 1;
    }

    private static int setPvp(CommandSourceStack source, boolean value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasPermission(player.getUUID(), TribeRole.OFFICER)) {
            source.sendFailure(Component.literal("Only officers and the leader can change tribe settings."));
            return 0;
        }
        tribe.setPvpEnabled(value);
        data.setDirty();
        source.sendSuccess(() -> Component.literal("PvP is now " + (value ? "enabled" : "disabled") + " in " + tribe.getName() + "'s territory."), true);
        return 1;
    }

    private static int setProtect(CommandSourceStack source, boolean value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeOf(player.getUUID());
        if (tribe == null) {
            source.sendFailure(NOT_IN_TRIBE);
            return 0;
        }
        if (!tribe.hasPermission(player.getUUID(), TribeRole.OFFICER)) {
            source.sendFailure(Component.literal("Only officers and the leader can change tribe settings."));
            return 0;
        }
        tribe.setProtectionEnabled(value);
        data.setDirty();
        source.sendSuccess(() -> Component.literal(value
                ? tribe.getName() + "'s territory is now protected. Only members and trusted players may build or interact."
                : tribe.getName() + "'s territory is no longer protected."), true);
        return 1;
    }

    private static int adminDelete(CommandSourceStack source, String name) {
        TribeSavedData data = data(source);
        Tribe tribe = data.getTribeByName(name);
        if (tribe == null) {
            source.sendFailure(Component.literal("No tribe named '" + name + "'."));
            return 0;
        }
        data.deleteTribe(tribe);
        source.sendSuccess(() -> Component.literal("Deleted tribe '" + name + "'."), true);
        return 1;
    }
}
