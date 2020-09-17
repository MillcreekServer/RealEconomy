package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import javax.inject.Singleton;
import java.math.BigDecimal;

public class MaxCapitalModule extends AbstractModule {
    @Provides
    @Singleton
    BigDecimal max() {
        return BigDecimal.valueOf(1E100);
    }
}
