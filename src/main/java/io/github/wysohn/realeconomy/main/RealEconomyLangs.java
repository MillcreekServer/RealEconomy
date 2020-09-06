package io.github.wysohn.realeconomy.main;

import io.github.wysohn.rapidframework2.core.manager.lang.Lang;

public enum RealEconomyLangs implements Lang {

    ;

    private final String[] def;

    RealEconomyLangs(String... def) {
        this.def = def;
    }

    @Override
    public String[] getEngDefault() {
        return def;
    }
}
