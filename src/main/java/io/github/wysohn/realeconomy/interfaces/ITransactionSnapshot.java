package io.github.wysohn.realeconomy.interfaces;

public interface ITransactionSnapshot {
    IMemento saveState();

    void restoreState(IMemento memento);
}
