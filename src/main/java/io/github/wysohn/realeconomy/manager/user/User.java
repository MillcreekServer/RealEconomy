package io.github.wysohn.realeconomy.manager.user;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;

import java.util.UUID;

public class User extends CachedElement<UUID> {
    private User() {
        super(null);
    }

    public User(UUID key) {
        super(key);
    }


}
