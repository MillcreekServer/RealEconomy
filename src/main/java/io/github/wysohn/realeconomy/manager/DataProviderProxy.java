package io.github.wysohn.realeconomy.manager;

import io.github.wysohn.rapidframework3.core.language.Pagination;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class DataProviderProxy<T> implements Pagination.DataProvider<T> {
    private final Supplier<List<T>> updater;
    private final Comparator<T> comparator;
    private final long QUERY_DELAY;

    private Pagination.DataProvider<T> cache;
    private long lastQuery = -1L;

    public DataProviderProxy(Supplier<List<T>> updater, Comparator<T> comparator, long queryDelay) {
        QUERY_DELAY = queryDelay;
        this.updater = updater;
        this.comparator = comparator;
    }

    public DataProviderProxy(Supplier<List<T>> updater, Comparator<T> comparator) {
        this(updater, comparator, 1000L);
    }

    private void update() {
        if (System.currentTimeMillis() < lastQuery + QUERY_DELAY)
            return;
        lastQuery = System.currentTimeMillis();

        List<T> copy = updater.get();
        copy.sort(comparator);
        cache = new Pagination.DataProvider<T>() {
            @Override
            public int size() {
                return copy.size();
            }

            @Override
            public T get(int i) {
                return copy.get(i);
            }
        };
    }

    @Override
    public int size() {
        update();
        return cache.size();
    }

    @Override
    public T get(int index) {
        update();
        return cache.get(index);
    }
}
