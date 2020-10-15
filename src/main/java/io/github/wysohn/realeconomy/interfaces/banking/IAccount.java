package io.github.wysohn.realeconomy.interfaces.banking;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface IAccount extends IAssetHolder {
    IBankingType getType();

    Map<UUID, BigDecimal> getCurrencyMap();

    IAccount clone();
}
