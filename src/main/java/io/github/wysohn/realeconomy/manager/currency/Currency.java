package io.github.wysohn.realeconomy.manager.currency;

import io.github.wysohn.rapidframework2.core.manager.caching.CachedElement;

import java.util.UUID;

public class Currency extends CachedElement<UUID> {
    private String code;

    private Currency() {
        super(null);
    }

    public Currency(UUID key) {
        super(key);
    }

    public String getCode() {
        return code;
    }

    void setCode(String code) {
        this.code = code;

        notifyObservers();
    }

    @Override
    public String toString() {
        return getStringKey();
    }
}
