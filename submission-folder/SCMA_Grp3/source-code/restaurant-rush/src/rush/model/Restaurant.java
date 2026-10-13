package rush.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * The restaurant floor: the menu, the tables, the waiting queue, every guest
 * who has arrived, their orders and the takings.
 * <p>
 * It is composed of its tables and queue (they do not exist without it) and is
 * the only object allowed to move a customer between the queue, a table and
 * the door, so those three places can never disagree.
 */
public class Restaurant {

    private final Menu menu;
    private final List<Table> tables = new ArrayList<>();
    private final List<Customer> queue = new ArrayList<>();
    private final List<Customer> guests = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();
    private Money revenue = Money.ZERO;

    public Restaurant(Menu menu, int tableCount) {
        this.menu = menu;
        for (int number = 1; number <= tableCount; number++) {
            tables.add(new Table(number));
        }
    }

    /** A newly arrived customer joins the back of the waiting queue. */
    public void admit(Customer customer) {
        guests.add(customer);
        queue.add(customer);
    }

    /** Moves a queued customer to a free table. */
    public void seat(Customer customer, Table table) {
        if (!queue.contains(customer)) {
            throw new RuleViolation(customer.label() + " is not in the waiting queue ("
                    + customer.statusDescription() + ").");
        }
        customer.seatAt(table);
        queue.remove(customer);
    }

    /** Records the order a seated customer has chosen, at the price locked now. */
    public Order placeOrder(Customer customer, MenuItem item, Money price, int turn) {
        Order order = new Order(customer, item, price, turn);
        customer.placeOrder(order);
        orders.add(order);
        return order;
    }

    /** Takes payment from a customer who is ready to pay, adds it to revenue and frees the table. */
    public Money collectPayment(Customer customer) {
        Money bill = customer.order()
                .orElseThrow(() -> new RuleViolation(customer.label() + " has nothing to pay for."))
                .price();
        customer.pay();
        revenue = revenue.plus(bill);
        return bill;
    }

    /** An unhappy customer walks out: their unfinished order is cancelled and their table freed. */
    public void depart(Customer customer) {
        queue.remove(customer);
        customer.leave();
    }

    // ------------------------------------------------------------------
    // Look-ups
    // ------------------------------------------------------------------

    public Customer customer(int id) {
        return guests.stream()
                .filter(c -> c.id() == id)
                .findFirst()
                .orElseThrow(() -> new RuleViolation("There is no customer #" + id + " in the restaurant."));
    }

    public Table table(int number) {
        if (number < 1 || number > tables.size()) {
            throw new RuleViolation("There is no table " + number + ". Tables are numbered 1 to "
                    + tables.size() + ".");
        }
        return tables.get(number - 1);
    }

    public Optional<Table> firstFreeTable() {
        return tables.stream().filter(Table::isFree).findFirst();
    }

    // ------------------------------------------------------------------
    // Read-only views
    // ------------------------------------------------------------------

    public Menu menu() {
        return menu;
    }

    public List<Table> tables() {
        return Collections.unmodifiableList(tables);
    }

    public List<Customer> queue() {
        return Collections.unmodifiableList(queue);
    }

    public List<Customer> guests() {
        return Collections.unmodifiableList(guests);
    }

    public List<Order> orders() {
        return Collections.unmodifiableList(orders);
    }

    /** Customers who are inside the restaurant and have not been served yet. */
    public List<Customer> awaitingService() {
        return guests.stream().filter(c -> c.status().isAwaitingService()).toList();
    }

    public Money revenue() {
        return revenue;
    }

    public List<Customer> paidCustomers() {
        return guests.stream().filter(c -> c.status() == CustomerStatus.PAID).toList();
    }

    public int servedCount() {
        return (int) guests.stream()
                .filter(c -> c.status() == CustomerStatus.EATING
                        || c.status() == CustomerStatus.READY_TO_PAY
                        || c.status() == CustomerStatus.PAID)
                .count();
    }

    public int departureCount() {
        return (int) guests.stream().filter(c -> c.status() == CustomerStatus.LEFT).count();
    }

    /**
     * Average satisfaction of paid customers. A paid customer's satisfaction no
     * longer changes, so this is the value recorded when each one paid.
     */
    public OptionalDouble averagePaidSatisfaction() {
        return paidCustomers().stream().mapToInt(Customer::satisfaction).average();
    }
}
