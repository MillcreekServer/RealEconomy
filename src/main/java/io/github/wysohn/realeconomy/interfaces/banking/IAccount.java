package io.github.wysohn.realeconomy.interfaces.banking;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface IAccount {
    IBankingType getType();

    Map<UUID, BigDecimal> getBalanceMap();

    IAccount clone();
}
