package rush.model;

/**
 * A dish on the menu: a name, a price and how many preparation units the Chef
 * must spend on it.
 */
public final class MenuItem {

    private final String name;
    private final Money price;
    private final int prepUnits;

    public MenuItem(String name, Money price, int prepUnits) {
        if (prepUnits < 1) {
            throw new IllegalArgumentException("A menu item needs at least one preparation unit");
        }
        this.name = name;
        this.price = price;
        this.prepUnits = prepUnits;
    }

    public String name() {
        return name;
    }

    public Money price() {
        return price;
    }

    public int prepUnits() {
        return prepUnits;
    }

    @Override
    public String toString() {
        return name;
    }
}
