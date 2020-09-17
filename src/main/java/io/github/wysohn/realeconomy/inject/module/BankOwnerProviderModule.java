package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwnerProvider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BankOwnerProviderModule extends AbstractModule {
    private final Set<IBankOwnerProvider> providerSet = new HashSet<>();

    public BankOwnerProviderModule(IBankOwnerProvider... providers) {
        providerSet.addAll(Arrays.asList(providers));
    }

    @Override
    protected void configure() {
        Multibinder<IBankOwnerProvider> multibinder
                = Multibinder.newSetBinder(binder(), IBankOwnerProvider.class);
        for (IBankOwnerProvider iBankOwnerProvider : providerSet) {
            multibinder.addBinding().toInstance(iBankOwnerProvider);
        }
    }
}
