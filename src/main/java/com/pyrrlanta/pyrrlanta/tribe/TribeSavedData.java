package com.pyrrlanta.pyrrlanta.tribe;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TribeSavedData extends SavedData {
    public static final String ID = "pyrrlanta_tribes";
    public static final int MAX_CLAIMS_PER_TRIBE = 64;

    public static final SavedData.Factory<TribeSavedData> FACTORY =
            new SavedData.Factory<>(TribeSavedData::new, TribeSavedData::load);

    private final Map<UUID, Tribe> tribes = new LinkedHashMap<>();
    private final Map<ClaimPos, UUID> claimIndex = new HashMap<>();
    private final Map<UUID, UUID> memberIndex = new HashMap<>();

    public TribeSavedData() {
    }

    public static TribeSavedData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    public static TribeSavedData get(MinecraftServer server) {
        return get(server.overworld());
    }

    public Tribe createTribe(String name, UUID leader) {
        Tribe tribe = new Tribe(UUID.randomUUID(), name, leader);
        tribes.put(tribe.getId(), tribe);
        memberIndex.put(leader, tribe.getId());
        setDirty();
        return tribe;
    }

    public void deleteTribe(Tribe tribe) {
        tribes.remove(tribe.getId());
        for (ClaimPos claim : tribe.getClaims()) {
            claimIndex.remove(claim);
        }
        for (UUID member : tribe.getMembers().keySet()) {
            memberIndex.remove(member);
        }
        setDirty();
    }

    public Tribe getTribe(UUID id) {
        return tribes.get(id);
    }

    public Tribe getTribeByName(String name) {
        for (Tribe tribe : tribes.values()) {
            if (tribe.getName().equalsIgnoreCase(name)) {
                return tribe;
            }
        }
        return null;
    }

    public Tribe getTribeOf(UUID player) {
        UUID tribeId = memberIndex.get(player);
        return tribeId == null ? null : tribes.get(tribeId);
    }

    public Tribe getTribeAt(ClaimPos pos) {
        UUID id = claimIndex.get(pos);
        return id == null ? null : tribes.get(id);
    }

    public boolean claim(Tribe tribe, ClaimPos pos) {
        if (claimIndex.containsKey(pos)) {
            return false;
        }
        claimIndex.put(pos, tribe.getId());
        tribe.getClaims().add(pos);
        setDirty();
        return true;
    }

    public boolean unclaim(Tribe tribe, ClaimPos pos) {
        if (!tribe.getId().equals(claimIndex.get(pos))) {
            return false;
        }
        claimIndex.remove(pos);
        tribe.getClaims().remove(pos);
        setDirty();
        return true;
    }

    public void addMember(Tribe tribe, UUID player, TribeRole role) {
        tribe.getMembers().put(player, role);
        memberIndex.put(player, tribe.getId());
        setDirty();
    }

    public void removeMember(Tribe tribe, UUID player) {
        tribe.getMembers().remove(player);
        memberIndex.remove(player);
        setDirty();
    }

    public Collection<Tribe> getAllTribes() {
        return tribes.values();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Tribe tribe : tribes.values()) {
            list.add(tribe.save());
        }
        tag.put("tribes", list);
        return tag;
    }

    public static TribeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TribeSavedData data = new TribeSavedData();
        ListTag list = tag.getList("tribes", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            Tribe tribe = Tribe.load((CompoundTag) t);
            data.tribes.put(tribe.getId(), tribe);
            for (ClaimPos claim : tribe.getClaims()) {
                data.claimIndex.put(claim, tribe.getId());
            }
            for (UUID member : tribe.getMembers().keySet()) {
                data.memberIndex.put(member, tribe.getId());
            }
        }
        return data;
    }
}
