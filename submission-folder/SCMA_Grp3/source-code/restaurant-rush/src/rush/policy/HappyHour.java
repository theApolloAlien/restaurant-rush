package rush.policy;

import java.util.ArrayList;
import java.util.List;

import rush.model.Customer;
import rush.model.Money;
import rush.model.Restaurant;
import rush.model.RuleViolation;

/**
 * A pricing policy the manager may switch on once per game, at the start of a
 * turn. It lasts for that turn and the next.
 * <ul>
 *   <li>Orders taken while it is active get 20% off (after any VIP discount).
 *       The price is locked when the order is taken, so orders placed before or
 *       after are unaffected.</li>
 *   <li>At the start of each active turn, customers still waiting for a table or
 *       for food recover 15 satisfaction, up to 100. Customers who have been
 *       served or have left are unaffected.</li>
 * </ul>
 */
public class HappyHour implements PriceModifier {

    public static final int DURATION_TURNS = 2;
    public static final int DISCOUNT_PERCENT = 20;
    public static final int RECOVERY = 15;

    /** The turn it was switched on, or 0 if it has not been used yet. */
    private int startTurn;

    public boolean isAvailable() {
        return startTurn == 0;
    }

    public void activate(int turn) {
        if (!isAvailable()) {
            throw new RuleViolation("Happy Hour can only be used once per game. It ran on turns "
                    + startTurn + "-" + lastTurn() + ".");
        }
        startTurn = turn;
    }

    public boolean isActiveOn(int turn) {
        return !isAvailable() && turn >= startTurn && turn <= lastTurn();
    }

    /** Active turns left, counting the current one: 2 on the first turn, 1 on the last, then 0. */
    public int turnsRemaining(int turn) {
        return isActiveOn(turn) ? lastTurn() - turn + 1 : 0;
    }

    public int startTurn() {
        return startTurn;
    }

    public int lastTurn() {
        return startTurn + DURATION_TURNS - 1;
    }

    @Override
    public Money adjust(Money price, int turn) {
        return isActiveOn(turn) ? price.percent(100 - DISCOUNT_PERCENT) : price;
    }

    /**
     * Applies this turn's satisfaction recovery to everyone still waiting.
     *
     * @return the customers who actually gained satisfaction
     */
    public List<Customer> refresh(Restaurant restaurant, int turn) {
        List<Customer> refreshed = new ArrayList<>();
        if (!isActiveOn(turn)) {
            return refreshed;
        }
        for (Customer customer : restaurant.awaitingService()) {
            if (customer.recover(RECOVERY) > 0) {
                refreshed.add(customer);
            }
        }
        return refreshed;
    }
}
