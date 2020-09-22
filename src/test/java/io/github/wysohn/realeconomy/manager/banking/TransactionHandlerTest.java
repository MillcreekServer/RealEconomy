package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.realeconomy.interfaces.banking.ITransactionHandler;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionHandlerTest {

    private ITransactionHandler handler;
    private Map<UUID, BigDecimal> capitals;
    private Currency currency;
    private UUID currencyUuid;

    @Before
    public void init() {
        handler = new TransactionHandler(
                BigDecimal.valueOf(Double.MAX_VALUE),
                BigDecimal.valueOf(-Double.MAX_VALUE)
        );

        capitals = new HashMap<>();
        currency = mock(Currency.class);
        currencyUuid = UUID.randomUUID();
        when(currency.getKey()).thenReturn(currencyUuid);
    }

    @Test
    public void balance() {
        Currency otherCurrency = mock(Currency.class);
        assertEquals(BigDecimal.ZERO, handler.balance(capitals, otherCurrency));
        capitals.put(currencyUuid, BigDecimal.valueOf(14235.33));
        assertEquals(BigDecimal.valueOf(14235.33), handler.balance(capitals, currency));
    }

    @Test
    public void deposit() {
        assertTrue(handler.deposit(capitals, BigDecimal.valueOf(1552.34), currency));
        assertEquals(0, handler.balance(capitals, currency).compareTo(BigDecimal.valueOf(1552.34)));
        assertTrue(handler.deposit(capitals, BigDecimal.valueOf(5000.2243), currency));
        assertEquals(0, handler.balance(capitals, currency).compareTo(BigDecimal.valueOf(1552.34 + 5000.2243)));
        assertFalse(handler.deposit(capitals, BigDecimal.valueOf(Double.MAX_VALUE), currency));
        assertEquals(0, handler.balance(capitals, currency).compareTo(BigDecimal.valueOf(1552.34 + 5000.2243)));
    }

    @Test
    public void withdraw() {
        assertTrue(handler.withdraw(capitals, BigDecimal.valueOf(3306.34), currency));
        assertEquals(0, handler.balance(capitals, currency).compareTo(BigDecimal.valueOf(-3306.34)));
        assertTrue(handler.withdraw(capitals, BigDecimal.valueOf(1254.673), currency));
        assertEquals(0, handler.balance(capitals, currency).compareTo(BigDecimal.valueOf(-3306.34 - 1254.673)));
        assertFalse(handler.withdraw(capitals, BigDecimal.valueOf(Double.MAX_VALUE), currency));
        assertEquals(0, handler.balance(capitals, currency).compareTo(BigDecimal.valueOf(-3306.34 - 1254.673)));
    }
}