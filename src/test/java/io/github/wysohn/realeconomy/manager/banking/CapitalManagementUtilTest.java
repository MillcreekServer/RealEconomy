package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
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

public class CapitalManagementUtilTest extends AbstractBukkitManagerTest {
    private BigDecimal maximum;
    private BigDecimal minimum;
    private Map<UUID, BigDecimal> capitals;
    private Currency currency;
    private UUID currencyUuid;

    @Before
    public void init() {
        maximum = BigDecimal.valueOf(Double.MAX_VALUE);
        minimum = BigDecimal.valueOf(-Double.MAX_VALUE);
        capitals = new HashMap<>();
        currency = mock(Currency.class);
        currencyUuid = UUID.randomUUID();
        when(currency.getKey()).thenReturn(currencyUuid);
    }

    @Test
    public void balance() {
        Currency otherCurrency = mock(Currency.class);
        assertEquals(BigDecimal.valueOf(0.0), CapitalManagementUtil.balance(capitals, otherCurrency));
        capitals.put(currencyUuid, BigDecimal.valueOf(14235.33));
        assertEquals(BigDecimal.valueOf(14235.33), CapitalManagementUtil.balance(capitals, currency));
    }

    @Test
    public void deposit() {
        assertTrue(CapitalManagementUtil.deposit(maximum, capitals, BigDecimal.valueOf(1552.34), currency));
        assertEquals(0, CapitalManagementUtil.balance(capitals, currency).compareTo(BigDecimal.valueOf(1552.34)));
        assertTrue(CapitalManagementUtil.deposit(maximum, capitals, BigDecimal.valueOf(5000.2243), currency));
        assertEquals(0, CapitalManagementUtil.balance(capitals, currency).compareTo(BigDecimal.valueOf(1552.34 + 5000.2243)));
        assertFalse(CapitalManagementUtil.deposit(maximum, capitals, BigDecimal.valueOf(Double.MAX_VALUE), currency));
        assertEquals(0, CapitalManagementUtil.balance(capitals, currency).compareTo(BigDecimal.valueOf(1552.34 + 5000.2243)));
    }

    @Test
    public void withdraw() {
        assertFalse(CapitalManagementUtil.withdraw(minimum, capitals, BigDecimal.valueOf(3306.34), currency));
        assertTrue(CapitalManagementUtil.withdraw(minimum, capitals, BigDecimal.valueOf(3306.34), currency, true));
        assertEquals(0, CapitalManagementUtil.balance(capitals, currency).compareTo(BigDecimal.valueOf(-3306.34)));
        assertTrue(CapitalManagementUtil.withdraw(minimum, capitals, BigDecimal.valueOf(1254.673), currency, true));
        assertEquals(0, CapitalManagementUtil.balance(capitals, currency).compareTo(BigDecimal.valueOf(-3306.34 - 1254.673)));
        assertFalse(CapitalManagementUtil.withdraw(minimum, capitals, BigDecimal.valueOf(Double.MAX_VALUE), currency, true));
        assertEquals(0, CapitalManagementUtil.balance(capitals, currency).compareTo(BigDecimal.valueOf(-3306.34 - 1254.673)));
    }
}