package io.github.wysohn.realeconomy.interfaces.banking;

import java.util.UUID;

public interface IOrderIssuerProvider {
    IOrderIssuer get(UUID issuerUuid);
}
