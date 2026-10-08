package rush.model;

/**
 * An immutable, non-negative amount of money held as integer cents.
 * <p>
 * Floating point is never used for money, so $9.00 + $15.00 is always exactly $24.00.
 */
public final class Money implements Comparable<Money> {

    public static final Money ZERO = new Money(0);

    private final long cents;

    private Money(long cents) {
        if (cents < 0) {
            throw new IllegalArgumentException("Money cannot be negative: " + cents + " cents");
        }
        this.cents = cents;
    }

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    public static Money dollars(long dollars) {
        return new Money(dollars * 100);
    }

    public long cents() {
        return cents;
    }

    public Money plus(Money other) {
        return new Money(cents + other.cents);
    }

    public boolean isAtLeast(Money other) {
        return cents >= other.cents;
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(cents, other.cents);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money other && other.cents == cents;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(cents);
    }

    /** Dollars with two decimals, e.g. {@code $24.30}. */
    @Override
    public String toString() {
        return String.format("$%d.%02d", cents / 100, cents % 100);
    }
}
