package rush.staff;

import java.util.Comparator;
import java.util.Optional;

import rush.model.Customer;
import rush.model.Money;
import rush.model.Table;

/**
 * The Cashier works automatically: each turn it takes payment from at most one
 * customer who is ready to pay, oldest first. It takes no instructions from
 * the manager, so it is a plain {@link StaffMember} rather than a
 * {@link DirectedStaff}.
 */
public class Cashier extends StaffMember {

    /** Whoever became ready to pay first is served first; ties go to the earlier arrival. */
    private static final Comparator<Customer> OLDEST_FIRST =
            Comparator.comparingInt(Customer::readyToPayTurn).thenComparingInt(Customer::id);

    public Cashier(String name) {
        super(name, 1);
    }

    @Override
    public String role() {
        return "Cashier";
    }

    /** The customer this cashier would charge this turn, if any. */
    public Optional<Customer> nextToPay(TurnContext ctx) {
        return ctx.restaurant().guests().stream()
                .filter(c -> c.canPayOn(ctx.turn()))
                .min(OLDEST_FIRST);
    }

    /** Takes one payment, if anyone can pay and this turn's task is unused. */
    public Optional<Payment> collectPayment(TurnContext ctx) {
        if (!hasTaskLeft()) {
            return Optional.empty();
        }
        return nextToPay(ctx).map(customer -> {
            int tableNo = customer.table().map(Table::number).orElse(0);
            Payment payment = new Payment(customer, ctx.restaurant().collectPayment(customer), tableNo);
            useTask();
            return payment;
        });
    }

    /** A completed payment. */
    public record Payment(Customer customer, Money amount, int tableNo) {
    }
}
