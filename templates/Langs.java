package io.github.wysohn.%pluginname%.main;

import io.github.wysohn.rapidframework2.core.manager.lang.Lang;

public enum $pluginname$Langs implements Lang {

    ;

    private final String[] def;

    $pluginname$Langs(String... def) {
        this.def = def;
    }

    @Override
    public String[] getEngDefault() {
        return def;
    }
}
