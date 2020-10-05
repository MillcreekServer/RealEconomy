package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.interfaces.banking.ITransactionHandler;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

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
        assertFalse(handler.withdraw(capitals, BigDecimal.valueOf(3306.34), currency));
        assertTrue(handler.withdraw(capitals, BigDecimal.valueOf(3306.34), currency, true));
        assertEquals(0, handler.balance(capitals, currency).compareTo(BigDecimal.valueOf(-3306.34)));
        assertTrue(handler.withdraw(capitals, BigDecimal.valueOf(1254.673), currency, true));
        assertEquals(0, handler.balance(capitals, currency).compareTo(BigDecimal.valueOf(-3306.34 - 1254.673)));
        assertFalse(handler.withdraw(capitals, BigDecimal.valueOf(Double.MAX_VALUE), currency, true));
        assertEquals(0, handler.balance(capitals, currency).compareTo(BigDecimal.valueOf(-3306.34 - 1254.673)));
    }

    @Test
    public void send() {
        IFinancialEntity from = mock(IFinancialEntity.class);
        IFinancialEntity to = mock(IFinancialEntity.class);

        assertEquals(ITransactionHandler.Result.NO_OWNER,
                handler.send(from, to, BigDecimal.valueOf(10342.33), currency));

        verify(from, never()).deposit(any(BigDecimal.class), any(Currency.class));
        verify(from, never()).withdraw(any(BigDecimal.class), any(Currency.class));
        verify(to, never()).deposit(any(BigDecimal.class), any(Currency.class));
        verify(to, never()).withdraw(any(BigDecimal.class), any(Currency.class));
    }

    @Test
    public void send2() {
        IFinancialEntity from = mock(IFinancialEntity.class);
        IFinancialEntity to = mock(IFinancialEntity.class);
        CentralBank bank = mock(CentralBank.class);

        when(currency.ownerBank()).thenReturn(bank);

        assertEquals(ITransactionHandler.Result.FROM_WITHDRAW_REFUSED,
                handler.send(from, to, BigDecimal.valueOf(3034.88), currency));

        verify(from, never()).deposit(any(BigDecimal.class), any(Currency.class));
        verify(from, times(1)).withdraw(eq(BigDecimal.valueOf(3034.88)), eq(currency));
        verify(to, never()).deposit(any(BigDecimal.class), any(Currency.class));
        verify(to, never()).withdraw(any(BigDecimal.class), any(Currency.class));

        verify(from).restoreState(any());
        verify(to).restoreState(any());
    }

    @Test
    public void send3() {
        IFinancialEntity from = mock(IFinancialEntity.class);
        IFinancialEntity to = mock(IFinancialEntity.class);
        CentralBank bank = mock(CentralBank.class);

        when(currency.ownerBank()).thenReturn(bank);
        when(from.withdraw(any(BigDecimal.class), any(Currency.class))).thenReturn(true);

        assertEquals(ITransactionHandler.Result.TO_DEPOSIT_REFUSED,
                handler.send(from, to, BigDecimal.valueOf(20314.87), currency));

        verify(from, never()).deposit(any(BigDecimal.class), any(Currency.class));
        verify(from, times(1)).withdraw(eq(BigDecimal.valueOf(20314.87)), eq(currency));
        verify(to, times(1)).deposit(eq(BigDecimal.valueOf(20314.87)), eq(currency));
        verify(to, never()).withdraw(any(BigDecimal.class), any(Currency.class));

        verify(from).restoreState(any());
        verify(to).restoreState(any());
    }

    @Test
    public void send4() {
        IFinancialEntity from = mock(IFinancialEntity.class);
        IFinancialEntity to = mock(IFinancialEntity.class);
        CentralBank bank = mock(CentralBank.class);

        when(currency.ownerBank()).thenReturn(bank);
        when(from.withdraw(any(BigDecimal.class), any(Currency.class))).thenReturn(true);
        when(to.deposit(any(BigDecimal.class), any(Currency.class))).thenReturn(true);

        assertEquals(ITransactionHandler.Result.OK,
                handler.send(from, to, BigDecimal.valueOf(87943.44), currency));

        verify(from, never()).deposit(any(BigDecimal.class), any(Currency.class));
        verify(from, times(1))
                .withdraw(eq(BigDecimal.valueOf(87943.44)), eq(currency));
        verify(to, times(1))
                .deposit(eq(BigDecimal.valueOf(87943.44)), eq(currency));
        verify(to, never()).withdraw(any(BigDecimal.class), any(Currency.class));

        verify(from, never()).restoreState(any());
        verify(to, never()).restoreState(any());
    }
}