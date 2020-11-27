package io.github.wysohn.realeconomy.interfaces.banking;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface IAccount {
    IBankingType getType();

    Map<UUID, BigDecimal> getCurrencyMap();

    IAccount clone();

    default BigDecimal minimumBalance(){
        return BigDecimal.valueOf(0.0);
    }
}
