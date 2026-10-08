package rush.model;

import java.util.Optional;

/**
 * A guest of the restaurant.
 * <p>
 * This class owns the visit life cycle (queue, table, order, eating, paying,
 * leaving), which is the same for every kind of customer. Subclasses only supply
 * the behaviour that varies: how fast they lose patience, what they order and
 * what their bill is. The game controller never asks "what type are you?"; it
 * calls these methods and lets dynamic dispatch pick the right behaviour.
 */
public abstract class Customer {

    public static final int MAX_SATISFACTION = 100;

    private final int id;
    private final String name;
    private final int arrivalTurn;

    private int satisfaction = MAX_SATISFACTION;
    private CustomerStatus status = CustomerStatus.WAITING;
    private Table table;
    private Order order;
    private int servedTurn;
    private int readyToPayTurn;

    protected Customer(int id, String name, int arrivalTurn) {
        this.id = id;
        this.name = name;
        this.arrivalTurn = arrivalTurn;
    }

    // ------------------------------------------------------------------
    // Behaviour that varies by customer type
    // ------------------------------------------------------------------

    /** Display name of the customer type, e.g. "Regular". */
    public abstract String type();

    /** Satisfaction lost at the end of each turn spent waiting for service. */
    public abstract int satisfactionLossPerTurn();

    /**
     * Customers choose their own food; the manager never does.
     * <p>
     * Default rule: guests read the menu board in rotation by arrival number,
     * so customer #1 orders the first item, #2 the second, and so on, wrapping
     * around at the end of the board.
     */
    public MenuItem chooseFrom(Menu menu) {
        return menu.atPosition(id - 1);
    }

    /** What this customer is billed for an item with the given menu price. */
    public Money billFor(Money menuPrice) {
        return menuPrice;
    }

    /** A short comment the customer makes as they pay. */
    public String remarkOnPaying() {
        if (satisfaction >= 80) {
            return "Lovely meal, thank you!";
        }
        if (satisfaction >= 60) {
            return "Not bad at all.";
        }
        return "That took a while...";
    }

    // ------------------------------------------------------------------
    // Life cycle (the same for every customer)
    // ------------------------------------------------------------------

    void seatAt(Table table) {
        requireStatus(CustomerStatus.WAITING, label() + " is not waiting in the queue.");
        table.assign(this);
        this.table = table;
        status = CustomerStatus.SEATED;
    }

    void placeOrder(Order order) {
        if (status == CustomerStatus.WAITING) {
            throw new RuleViolation(label() + " has no table yet. Seat them first.");
        }
        requireStatus(CustomerStatus.SEATED, label() + " has already ordered (" + statusDescription() + ").");
        this.order = order;
        status = CustomerStatus.ORDERED;
    }

    void startEating(int turn) {
        requireStatus(CustomerStatus.ORDERED, label() + " is not waiting for food.");
        servedTurn = turn;
        status = CustomerStatus.EATING;
    }

    /**
     * A customer served on an earlier turn finishes one full turn of eating
     * and becomes ready to pay.
     *
     * @return true if the customer finished eating this turn
     */
    public boolean finishEating(int turn) {
        if (status != CustomerStatus.EATING || servedTurn >= turn) {
            return false;
        }
        readyToPayTurn = turn;
        status = CustomerStatus.READY_TO_PAY;
        return true;
    }

    /** A customer who became ready to pay this turn pays next turn at the earliest. */
    public boolean canPayOn(int turn) {
        return status == CustomerStatus.READY_TO_PAY && readyToPayTurn < turn;
    }

    void pay() {
        requireStatus(CustomerStatus.READY_TO_PAY, label() + " is not ready to pay.");
        status = CustomerStatus.PAID;
        order.markPaid();
        vacateTable();
    }

    /**
     * Applies one turn of waiting. Only customers still awaiting service lose
     * satisfaction, and it never drops below 0.
     *
     * @return the satisfaction actually lost (0 if the customer is not waiting)
     */
    public int loseSatisfaction() {
        if (!status.isAwaitingService()) {
            return 0;
        }
        int before = satisfaction;
        satisfaction = Math.max(0, satisfaction - satisfactionLossPerTurn());
        return before - satisfaction;
    }

    /**
     * Restores satisfaction to a customer still awaiting service, never above 100.
     *
     * @return the satisfaction actually gained
     */
    public int recover(int points) {
        if (!status.isAwaitingService()) {
            return 0;
        }
        int before = satisfaction;
        satisfaction = Math.min(MAX_SATISFACTION, satisfaction + points);
        return satisfaction - before;
    }

    void leave() {
        if (status.isGone()) {
            return;
        }
        if (order != null) {
            order.cancel();
        }
        vacateTable();
        status = CustomerStatus.LEFT;
    }

    private void vacateTable() {
        if (table != null) {
            table.release();
            table = null;
        }
    }

    private void requireStatus(CustomerStatus expected, String reason) {
        if (status != expected) {
            throw new RuleViolation(reason);
        }
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    /** Short label used in messages, e.g. "#2 Ben". */
    public String label() {
        return "#" + id + " " + name;
    }

    public int arrivalTurn() {
        return arrivalTurn;
    }

    public int satisfaction() {
        return satisfaction;
    }

    public boolean isUnhappy() {
        return satisfaction == 0;
    }

    public CustomerStatus status() {
        return status;
    }

    public Optional<Table> table() {
        return Optional.ofNullable(table);
    }

    public Optional<Order> order() {
        return Optional.ofNullable(order);
    }

    public int readyToPayTurn() {
        return readyToPayTurn;
    }

    /** Human-readable status, including order progress when relevant. */
    public String statusDescription() {
        if (status == CustomerStatus.ORDERED && order != null) {
            return "Waiting for " + order.item().name();
        }
        return status.label();
    }

    @Override
    public String toString() {
        return label() + " (" + type() + ")";
    }
}
