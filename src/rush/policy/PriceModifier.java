package rush.policy;

import rush.model.Money;

/**
 * A restaurant-wide pricing rule applied after the customer's own discount,
 * such as Happy Hour.
 */
public interface PriceModifier {

    /** The price after this rule, for an order taken on the given turn. */
    Money adjust(Money price, int turn);
}
