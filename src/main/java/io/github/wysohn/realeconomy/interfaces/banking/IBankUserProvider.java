package io.github.wysohn.realeconomy.interfaces.banking;

import java.util.UUID;

public interface IBankUserProvider {
    IBankUser get(UUID bankUserUuid);
}
