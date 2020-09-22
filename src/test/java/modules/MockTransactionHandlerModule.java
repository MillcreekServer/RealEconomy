package modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.github.wysohn.realeconomy.interfaces.banking.ITransactionHandler;

import static org.mockito.Mockito.mock;

public class MockTransactionHandlerModule extends AbstractModule {
    public final ITransactionHandler transactionHandler = mock(ITransactionHandler.class);

    @Provides
    ITransactionHandler transactionHandler() {
        return transactionHandler;
    }
}
