package io.github.wysohn.realeconomy.manager.currency;

import io.github.wysohn.rapidframework2.core.database.Database;
import io.github.wysohn.rapidframework2.core.manager.caching.AbstractManagerElementCaching;
import io.github.wysohn.rapidframework2.tools.Validation;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CurrencyManager extends AbstractManagerElementCaching<UUID, Currency> {
    private final Map<String, UUID> codeMap = new HashMap<>();

    public CurrencyManager(int loadPriority) {
        super(loadPriority);
    }

    @Override
    protected Database.DatabaseFactory<Currency> createDatabaseFactory() {
        return getDatabaseFactory(Currency.class, "currency");
    }

    @Override
    protected UUID fromString(String s) {
        return UUID.fromString(s);
    }

    @Override
    protected Currency newInstance(UUID uuid) {
        return new Currency(uuid);
    }

    @Override
    public void enable() throws Exception {
        super.enable();

        // remap code to UUID
        forEach(currency -> codeMap.put(currency.getCode(), currency.getKey()));

        if (!main().conf().get(KEY_MAX_LEN).isPresent()) {
            main().conf().put(KEY_MAX_LEN, 3);
        }
    }

    /**
     * @param key
     * @return
     * @deprecated Use {@link #newCurrency(String, String)}
     */
    @Override
    public Optional<WeakReference<Currency>> getOrNew(UUID key) {
        throw new RuntimeException("Do not use this directly. Use #newCurrency(String, String)");
    }

    /**
     * Create a new currency.
     *
     * @param name an unique String to represent this currency.
     * @param code an unique currency code (USD for example). The maximum length can be found in config.yml by the key
     *             stated in {@link #KEY_MAX_LEN}
     * @return The result
     */
    public synchronized Result newCurrency(String name, String code) {
        Validation.assertNotNull(name);
        Validation.assertNotNull(code);
        code = code.toUpperCase();

        int length_max = main().conf().get(KEY_MAX_LEN)
                .map(Integer.class::cast)
                .orElse(3);

        if (code.length() > length_max)
            return Result.CODE_LENGTH;

        if (codeMap.containsKey(code))
            return Result.DUP_CODE;

        if (get(name).isPresent())
            return Result.DUP_NAME;

        String finalCode = code;
        super.getOrNew(UUID.randomUUID())
                .map(Reference::get)
                .ifPresent(currency -> {
                    currency.setStringKey(name);

                    currency.setCode(finalCode);
                    codeMap.put(finalCode, currency.getKey());
                });
        return Result.OK;
    }

    public enum Result {
        OK, DUP_NAME, DUP_CODE, CODE_LENGTH
    }

    public static final String KEY_MAX_LEN = "currency.code.maxlen";
}
