package rush.policy;

import java.util.List;

import rush.model.Customer;
import rush.model.MenuItem;
import rush.model.Money;

/**
 * Decides the bill for an order at the moment it is taken. The result is
 * locked into the {@link rush.model.Order} and never recalculated.
 * <p>
 * The customer's own discount comes first (a VIP's 10%), then each
 * restaurant-wide modifier in turn (Happy Hour's 20%). A VIP Burger during
 * Happy Hour is therefore $15.00 x 0.9 x 0.8 = $10.80.
 */
public class PricingPolicy {

    private final List<PriceModifier> modifiers;

    public PricingPolicy(List<PriceModifier> modifiers) {
        this.modifiers = List.copyOf(modifiers);
    }

    public Money quote(Customer customer, MenuItem item, int turn) {
        Money price = customer.billFor(item.price());
        for (PriceModifier modifier : modifiers) {
            price = modifier.adjust(price, turn);
        }
        return price;
    }
}
