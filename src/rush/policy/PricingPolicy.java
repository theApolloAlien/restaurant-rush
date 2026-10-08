package rush.policy;

import rush.model.Customer;
import rush.model.MenuItem;
import rush.model.Money;

/**
 * Decides the bill for an order at the moment it is taken. The result is
 * locked into the {@link rush.model.Order} and never recalculated.
 */
public class PricingPolicy {

    public Money quote(Customer customer, MenuItem item, int turn) {
        return customer.billFor(item.price());
    }
}
