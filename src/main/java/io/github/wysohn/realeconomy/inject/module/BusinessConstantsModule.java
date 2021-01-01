package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import javax.inject.Named;

public class BusinessConstantsModule extends AbstractModule {
    @Provides
    @Named("oreRegenDelay")
    public long regenDelay() {
        return 60 * 1000L;
    }
}
