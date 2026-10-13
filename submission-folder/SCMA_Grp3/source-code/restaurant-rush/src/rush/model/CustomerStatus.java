package rush.model;

/**
 * Where a customer is in their visit. A customer only ever moves forward
 * through these states (or jumps to {@link #LEFT}).
 */
public enum CustomerStatus {
    WAITING("Waiting for a table", true),
    SEATED("Seated, ready to order", true),
    ORDERED("Waiting for food", true),
    EATING("Eating", false),
    READY_TO_PAY("Ready to pay", false),
    PAID("Paid", false),
    LEFT("Left unhappy", false);

    private final String label;
    private final boolean awaitingService;

    CustomerStatus(String label, boolean awaitingService) {
        this.label = label;
        this.awaitingService = awaitingService;
    }

    public String label() {
        return label;
    }

    /** True while the customer has not been served yet and is still losing patience. */
    public boolean isAwaitingService() {
        return awaitingService;
    }

    /** True once the customer has left the restaurant, paid or not. */
    public boolean isGone() {
        return this == PAID || this == LEFT;
    }
}
