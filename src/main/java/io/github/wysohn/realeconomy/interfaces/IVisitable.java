package io.github.wysohn.realeconomy.interfaces;

public interface IVisitable {
    boolean addVisitor(IVisitor visitor);

    boolean removeVisitor(IVisitor visitor);
}
