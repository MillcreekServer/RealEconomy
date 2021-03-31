package io.github.wysohn.realeconomy.interfaces.banking;

import io.github.wysohn.rapidframework3.interfaces.entity.IEntitySnapshot;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface IAccount extends IEntitySnapshot {
    IBankingType getType();

    Map<UUID, BigDecimal> getCurrencyMap();

//    IAccount clone();

    default BigDecimal minimumBalance() {
        return BigDecimal.valueOf(0.0);
    }
}
