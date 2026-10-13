package rush.model;

import java.util.List;

/**
 * Two existing dishes sold together.
 * <ul>
 *   <li>Preparation units = the sum of the two dishes' units.</li>
 *   <li>Price = 90% of the dishes' combined price, rounded to the nearest cent.
 *       Salad + Pasta = ($9.00 + $18.00) x 0.9 = $24.30, 3 units.</li>
 * </ul>
 * Both values are derived from the components, so a change to a dish's price
 * is reflected in every combo that contains it.
 */
public final class ComboMeal extends MenuItem {

    public static final int PRICE_PERCENT = 90;

    private final Dish first;
    private final Dish second;

    public ComboMeal(Dish first, Dish second) {
        super(first.name() + " + " + second.name() + " Combo");
        this.first = first;
        this.second = second;
    }

    @Override
    public Money price() {
        return first.price().plus(second.price()).percent(PRICE_PERCENT);
    }

    @Override
    public int prepUnits() {
        return first.prepUnits() + second.prepUnits();
    }

    public List<Dish> components() {
        return List.of(first, second);
    }
}
