package rush.model;

/** Life cycle of an order in the kitchen. */
public enum OrderStatus {
    IN_PREPARATION("In preparation"),
    READY("READY"),
    SERVED("SERVED"),
    PAID("PAID"),
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
