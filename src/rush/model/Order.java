package rush.model;

/**
 * One customer's order. The price is locked when the order is taken and never
 * changes afterwards.
 */
public class Order {

    private final Customer customer;
    private final MenuItem item;
    private final Money price;
    private final int placedTurn;

    private int unitsDone;
    private OrderStatus status = OrderStatus.IN_PREPARATION;
    private int readyTurn;

    Order(Customer customer, MenuItem item, Money price, int placedTurn) {
        this.customer = customer;
        this.item = item;
        this.price = price;
        this.placedTurn = placedTurn;
    }

    /**
     * The Chef spends one preparation unit on this order. When the last unit is
     * done the order becomes READY.
     */
    public void prepareUnit(int turn) {
        if (status != OrderStatus.IN_PREPARATION) {
            throw new RuleViolation(describe() + " needs no more cooking: it is " + status.label() + ".");
        }
        unitsDone++;
        if (unitsDone == item.prepUnits()) {
            status = OrderStatus.READY;
            readyTurn = turn;
        }
    }

    /**
     * The Waiter takes the dish to the table. Only a dish that was already READY
     * before this turn's cooking can be served, so a dish finished this turn is
     * served next turn at the earliest.
     */
    public void serve(int turn) {
        switch (status) {
            case IN_PREPARATION -> throw new RuleViolation(describe() + " is still cooking ("
                    + unitsDone + "/" + item.prepUnits() + " units done).");
            case READY -> {
                if (readyTurn >= turn) {
                    throw new RuleViolation(describe() + " only became READY this turn. Serve it next turn.");
                }
            }
            default -> throw new RuleViolation(describe() + " is already " + status.label() + ".");
        }
        status = OrderStatus.SERVED;
        customer.startEating(turn);
    }

    void markPaid() {
        status = OrderStatus.PAID;
    }

    void cancel() {
        if (status == OrderStatus.IN_PREPARATION || status == OrderStatus.READY) {
            status = OrderStatus.CANCELLED;
        }
    }

    /** e.g. "#3 Chloe's Pasta". */
    public String describe() {
        return customer.label() + "'s " + item.name();
    }

    public Customer customer() {
        return customer;
    }

    public MenuItem item() {
        return item;
    }

    public Money price() {
        return price;
    }

    public int placedTurn() {
        return placedTurn;
    }

    public int unitsDone() {
        return unitsDone;
    }

    public int unitsRequired() {
        return item.prepUnits();
    }

    public OrderStatus status() {
        return status;
    }

    public boolean isActive() {
        return status == OrderStatus.IN_PREPARATION || status == OrderStatus.READY
                || status == OrderStatus.SERVED;
    }
}
