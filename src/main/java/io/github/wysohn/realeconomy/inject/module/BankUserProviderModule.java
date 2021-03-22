package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.ProvidesIntoSet;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUserProvider;
import io.github.wysohn.realeconomy.manager.simulation.MarketSimulationManager;
import io.github.wysohn.realeconomy.manager.user.UserManager;

public class BankUserProviderModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), IBankUserProvider.class);
    }

    @ProvidesIntoSet
    @Singleton
    public IBankUserProvider bankUserProvider(UserManager userManager){
        return userManager.bankUserProvider();
    }

    @ProvidesIntoSet
    @Singleton
    public IBankUserProvider bankUserProvider(MarketSimulationManager simulationManager){
        return simulationManager.getAgentProvider();
    }
}
