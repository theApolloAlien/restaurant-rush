package rush.model;

/**
 * Anything a customer can order: it has a name, a price and a number of
 * preparation units the Chef must spend on it.
 * <p>
 * Callers never need to know whether an item is a single {@link Dish} or a
 * {@link ComboMeal}; they ask for the price and the units and each kind of item
 * answers in its own way.
 */
public abstract class MenuItem {

    private final String name;

    protected MenuItem(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public abstract Money price();

    public abstract int prepUnits();

    @Override
    public String toString() {
        return name;
    }
}
