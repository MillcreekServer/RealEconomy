package io.github.wysohn.realeconomy.interfaces.banking;

public interface IAccount {
    IBankingType getType();

    IAccount clone();
}
