package rush.model;

/**
 * A single dish with a fixed price and number of preparation units, such as
 * Salad ($9.00, 1 unit).
 */
public final class Dish extends MenuItem {

    private final Money price;
    private final int prepUnits;

    public Dish(String name, Money price, int prepUnits) {
        super(name);
        if (prepUnits < 1) {
            throw new IllegalArgumentException("A dish needs at least one preparation unit");
        }
        this.price = price;
        this.prepUnits = prepUnits;
    }

    @Override
    public Money price() {
        return price;
    }

    @Override
    public int prepUnits() {
        return prepUnits;
    }
}
