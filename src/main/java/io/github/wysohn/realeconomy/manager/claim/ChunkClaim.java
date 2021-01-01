package io.github.wysohn.realeconomy.manager.claim;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.data.SimpleChunkLocation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChunkClaim extends CachedElement<SimpleChunkLocation> {
    private final Set<UUID> memberList = new HashSet<>();

    private ChunkClaim() {
        super(null);
    }

    public ChunkClaim(SimpleChunkLocation key) {
        super(key);
    }

    public boolean addMember(UUID memberUuid) {
        boolean add = memberList.add(memberUuid);
        if (add)
            notifyObservers();
        return add;
    }

    public boolean hasMember(UUID memberUuid) {
        return memberList.contains(memberUuid);
    }

    public boolean removeMember(UUID memberUuid) {
        boolean remove = memberList.remove(memberUuid);
        if (remove)
            notifyObservers();
        return remove;
    }
}
