package io.github.wysohn.realeconomy.manager.currency;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.core.caching.AbstractManagerElementCaching;
import io.github.wysohn.rapidframework3.core.database.Databases;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.utils.Validation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
public class CurrencyManager extends AbstractManagerElementCaching<UUID, Currency> {
    private final Map<String, UUID> codeMap = new HashMap<>();

    private final ManagerConfig config;

    @Inject
    public CurrencyManager(
            @Named("pluginName") String pluginName,
            @PluginLogger Logger logger,
            ManagerConfig config,
            @PluginDirectory File pluginDir,
            IShutdownHandle shutdownHandle,
            ISerializer serializer,
            Injector injector) {
        super(pluginName, logger, config, pluginDir, shutdownHandle, serializer, injector, Currency.class);
        this.config = config;
    }

    @Override
    protected Databases.DatabaseFactory createDatabaseFactory() {
        return getDatabaseFactory("currency");
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

        if (!config.get(KEY_MAX_LEN).isPresent()) {
            config.put(KEY_MAX_LEN, 3);
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

        int length_max = config.get(KEY_MAX_LEN)
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

    /**
     * Change currency code
     *
     * @param name    name of the currency (not the code)
     * @param newCode new code (refer to {@link #newCurrency(String, String)})
     * @return The result
     */
    public synchronized Result changeCode(String name, String newCode) {
        if (codeMap.containsKey(newCode))
            return Result.DUP_CODE;

        if (!get(name).isPresent())
            return Result.NOT_EXIST;

        get(name).map(Reference::get)
                .ifPresent(currency -> {
                    String previousCode = currency.getCode();
                    currency.setCode(newCode);
                    codeMap.put(newCode, currency.getKey());
                    if (previousCode != null)
                        codeMap.remove(previousCode);
                });
        return Result.OK;
    }

    public enum Result {
        OK, DUP_NAME, DUP_CODE, CODE_LENGTH, NOT_EXIST
    }

    public static final String KEY_MAX_LEN = "currency.code.maxlen";
}
