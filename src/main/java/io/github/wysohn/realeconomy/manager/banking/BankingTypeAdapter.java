package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.banking.IAccount;
import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class BankingTypeAdapter<A extends IAccount> implements IBankingType {
    final UUID uuid;
    final Supplier<A> accountConstructor;

    public BankingTypeAdapter(UUID uuid, Supplier<A> accountConstructor) {
        Validation.assertNotNull(uuid);
        Validation.assertNotNull(accountConstructor);

        this.uuid = uuid;
        this.accountConstructor = accountConstructor;
        BankingTypeRegistry.registerType(uuid, this);
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public IAccount createAccount() {
        return accountConstructor.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankingTypeAdapter<?> that = (BankingTypeAdapter<?>) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
