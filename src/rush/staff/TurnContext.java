package rush.staff;

import rush.model.Restaurant;
import rush.policy.PricingPolicy;

/**
 * Everything a member of staff needs to do their job during one turn.
 */
public record TurnContext(int turn, Restaurant restaurant, PricingPolicy pricing) {
}
