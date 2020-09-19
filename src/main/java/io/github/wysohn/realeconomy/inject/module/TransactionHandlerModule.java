package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import io.github.wysohn.realeconomy.interfaces.banking.ITransactionHandler;
import io.github.wysohn.realeconomy.manager.banking.TransactionHandler;

public class TransactionHandlerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ITransactionHandler.class).to(TransactionHandler.class);
    }
}
