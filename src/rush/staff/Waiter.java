package rush.staff;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import rush.model.Customer;
import rush.model.CustomerStatus;
import rush.model.MenuItem;
import rush.model.Money;
import rush.model.Order;
import rush.model.OrderStatus;
import rush.model.RuleViolation;

/**
 * The Waiter completes at most one task per turn: seat a queued customer, take
 * a seated customer's order, or serve a READY dish.
 */
public class Waiter extends DirectedStaff {

    public Waiter(String name) {
        super(name, 1);
    }

    @Override
    public String role() {
        return "Waiter";
    }

    @Override
    public Set<Verb> duties() {
        return EnumSet.of(Verb.SEAT, Verb.ORDER, Verb.SERVE);
    }

    @Override
    protected String carryOut(Command command, TurnContext ctx) {
        return switch (command.verb()) {
            case SEAT -> seatCustomer(name(), command, ctx);
            case ORDER -> takeOrder(requireCustomer(command, ctx), ctx);
            case SERVE -> serve(requireCustomer(command, ctx), ctx);
            default -> throw new IllegalStateException("Not a waiter duty: " + command.verb());
        };
    }

    /** The customer chooses their own food; the price is locked now. */
    private String takeOrder(Customer customer, TurnContext ctx) {
        if (customer.status() != CustomerStatus.SEATED) {
            if (customer.status() == CustomerStatus.WAITING) {
                throw new RuleViolation(customer.label() + " has no table yet. Seat them first.");
            }
            throw new RuleViolation(customer.label() + " can't order now (" + customer.statusDescription() + ").");
        }
        MenuItem choice = customer.chooseFrom(ctx.restaurant().menu());
        Money price = ctx.pricing().quote(customer, choice, ctx.turn());
        ctx.restaurant().placeOrder(customer, choice, price, ctx.turn());
        return name() + " takes " + customer.label() + "'s order: " + choice.name() + " (" + price + ").";
    }

    private String serve(Customer customer, TurnContext ctx) {
        Order order = customer.order()
                .orElseThrow(() -> new RuleViolation(customer.label() + " hasn't ordered anything yet."));
        order.serve(ctx.turn());
        return name() + " serves " + order.describe() + ". Enjoy!";
    }

    @Override
    protected List<Command> usefulCommands(TurnContext ctx) {
        List<Command> options = new ArrayList<>();
        for (Order order : ctx.restaurant().orders()) {
            if (order.status() == OrderStatus.READY) {
                options.add(Command.serve(order.customer().id()));
            }
        }
        for (Customer customer : ctx.restaurant().guests()) {
            if (customer.status() == CustomerStatus.SEATED) {
                options.add(Command.takeOrder(customer.id()));
            }
        }
        options.addAll(seatingOptions(ctx));
        return options;
    }
}
