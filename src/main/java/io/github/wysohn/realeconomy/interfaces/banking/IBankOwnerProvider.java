package io.github.wysohn.realeconomy.interfaces.banking;

import java.util.UUID;

public interface IBankOwnerProvider {
    IBankOwner get(UUID ownerUuid);
}
