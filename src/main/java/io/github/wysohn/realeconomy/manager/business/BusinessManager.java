package io.github.wysohn.realeconomy.manager.business;

import io.github.wysohn.rapidframework3.core.main.Manager;
import io.github.wysohn.realeconomy.interfaces.business.IBusiness;
import io.github.wysohn.realeconomy.interfaces.business.IBusinessProvider;

import java.util.*;

public class BusinessManager extends Manager {
    private static final List<IBusinessProvider> BUSINESSES_PROVIDERS = new LinkedList<>();

    /**
     * Register the IBusinessProvider. Note that all the methods should be thread-safe, so that
     * it can be used in the update thread along with the server thread.
     *
     * @param provider provider
     * @throws RuntimeException same provider is registered again
     */
    public static void registerProvider(IBusinessProvider provider) {
        if (BUSINESSES_PROVIDERS.contains(provider))
            throw new RuntimeException("Duplicated provider.");

        BUSINESSES_PROVIDERS.add(provider);
    }

    private final long INTERVAL = 1000L;
    private Timer updateTimer;

    @Override
    public void enable() throws Exception {

    }

    @Override
    public void load() throws Exception {
        if (updateTimer != null)
            updateTimer.cancel();

        BUSINESSES_PROVIDERS.forEach(provider -> provider.keys().forEach(key -> {
            Optional.ofNullable(provider.get(key))
                    .ifPresent(IBusiness::init);
        }));

        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new UpdateTask(), INTERVAL, INTERVAL);
    }

    @Override
    public void disable() throws Exception {
        if (updateTimer != null)
            updateTimer.cancel();

        BUSINESSES_PROVIDERS.forEach(provider -> provider.keys().forEach(key -> {
            Optional.ofNullable(provider.get(key))
                    .ifPresent(IBusiness::stop);
        }));
    }

    private class UpdateTask extends TimerTask {
        @Override
        public void run() {
            BUSINESSES_PROVIDERS.forEach(provider ->
                    provider.keys().forEach(key -> Optional.ofNullable(provider.get(key))
                            .ifPresent(IBusiness::init)));
        }
    }
}
