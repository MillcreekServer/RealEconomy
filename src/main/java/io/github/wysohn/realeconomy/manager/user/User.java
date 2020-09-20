package io.github.wysohn.realeconomy.manager.user;

import io.github.wysohn.rapidframework3.bukkit.data.BukkitPlayer;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;

import java.util.UUID;

public class User extends BukkitPlayer implements IBankUser {

    private User() {
        super(null);
    }

    public User(UUID key) {
        super(key);
    }

    @Override
    public UUID getUuid() {
        return getKey();
    }
}
