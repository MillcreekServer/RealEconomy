package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;

import javax.inject.Singleton;
import java.math.BigDecimal;

public class CapitalLimitModule extends AbstractModule {
    @Provides
    @Singleton
    @MinCapital
    BigDecimal min() {
        return BigDecimal.valueOf(-1E100);
    }

    @Provides
    @Singleton
    @MaxCapital
    BigDecimal max() {
        return BigDecimal.valueOf(1E100);
    }
}
