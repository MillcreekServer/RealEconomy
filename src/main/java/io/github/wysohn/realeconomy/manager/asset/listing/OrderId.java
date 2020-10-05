package io.github.wysohn.realeconomy.manager.asset.listing;

import java.util.Objects;
import java.util.Random;

/**
 * Use 12 digit random number as id.
 * <p>
 * According to Birthday Problem, the probability of collision would be following:
 * Assuming the daily trade volume would be at most 10000,
 * probability of having duplicated id is approximately
 * <p>
 * p(10000, 999999999999) = 1 - e^(-10000^2/(2*999999999999))
 * = 0.00005
 * = 0.005%
 */
public class OrderId {
    private static final Random rand = new Random();
    private static final int ID_MAX = 999999;

    private final int b;
    private final int a;

    private OrderId(int b, int a) {
        this.b = b;
        this.a = a;
    }

    private OrderId() {
        this(rand.nextInt(ID_MAX), rand.nextInt(ID_MAX));
    }

    public static OrderId randomId() {
        return new OrderId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderId orderId = (OrderId) o;
        return b == orderId.b &&
                a == orderId.a;
    }

    @Override
    public int hashCode() {
        return Objects.hash(b, a);
    }

    @Override
    public String toString() {
        return b + "-" + a;
    }

    public static OrderId valueOf(String str) {
        String[] split = str.split("-");
        if (split.length != 2)
            throw new RuntimeException(str + " is not valid id.");

        return new OrderId(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }
}
