package io.github.wysohn.realeconomy.interfaces.business;

public interface ISimpleStringStore {
    String getData(String key);

    boolean hasData(String key);

    void putData(String key, String value);
}
