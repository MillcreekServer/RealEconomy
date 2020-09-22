package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.rapidframework3.utils.Sampling;
import io.github.wysohn.rapidframework3.utils.Validation;

import java.util.Objects;

public class BankId {
    Type type;
    int node;

    public BankId(Type type) {
        Validation.assertNotNull(type);

        this.type = type;
        this.node = Sampling.uniform(10000000, 1)[0];
    }

    public BankId(Type type, int node) {
        Validation.assertNotNull(type);

        this.type = type;
        this.node = node;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankId bankId = (BankId) o;
        return node == bankId.node && type == bankId.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, node);
    }

    @Override
    public String toString() {
        return String.format("02%d", type.ordinal()) + String.format("07%d", node);
    }

    public static BankId fromString(String str) {
        char[] c = str.toCharArray();
        if (c.length < 3)
            throw new IllegalArgumentException(str + " is not a valid BankId");

        int ordinal = Integer.parseInt(String.copyValueOf(c, 0, 2));
        int node = Integer.parseInt(String.copyValueOf(c, 2, c.length - 2));

        BankId id = new BankId(Type.values()[ordinal]);
        id.node = node;
        return id;
    }

    public enum Type {
        CENTRAL_BANK,
    }
}
