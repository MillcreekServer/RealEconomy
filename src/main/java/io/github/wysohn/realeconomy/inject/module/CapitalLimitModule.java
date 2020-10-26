package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;

import javax.inject.Singleton;
import java.math.BigDecimal;

public class CapitalLimitModule extends AbstractModule {
    public static final BigDecimal MIN = BigDecimal.valueOf(-1E100);
    public static final BigDecimal MAX = BigDecimal.valueOf(1E100);

    @Provides
    @Singleton
    @MinCapital
    BigDecimal min() {
        return MIN;
    }

    @Provides
    @Singleton
    @MaxCapital
    BigDecimal max() {
        return MAX;
    }
}
