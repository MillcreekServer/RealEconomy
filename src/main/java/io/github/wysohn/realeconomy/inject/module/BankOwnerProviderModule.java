package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwnerProvider;

public class BankOwnerProviderModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), IBankOwnerProvider.class);
    }
}
