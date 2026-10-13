package rush.staff;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import rush.model.Customer;
import rush.model.Order;
import rush.model.OrderStatus;
import rush.model.RuleViolation;

/**
 * The Chef advances one order by one preparation unit per turn. The Chef may
 * work on an order the Waiter took earlier in the same turn.
 */
public class Chef extends DirectedStaff {

    public Chef(String name) {
        super(name, 1);
    }

    @Override
    public String role() {
        return "Chef";
    }

    @Override
    public Set<Verb> duties() {
        return EnumSet.of(Verb.COOK);
    }

    @Override
    protected String carryOut(Command command, TurnContext ctx) {
        Customer customer = requireCustomer(command, ctx);
        Order order = customer.order()
                .orElseThrow(() -> new RuleViolation(customer.label() + " hasn't ordered anything yet."));
        order.prepareUnit(ctx.turn());
        String progress = order.unitsDone() + "/" + order.unitsRequired();
        if (order.status() == OrderStatus.READY) {
            return name() + " finishes " + order.describe() + " (" + progress + "). It is READY.";
        }
        return name() + " works on " + order.describe() + " (" + progress + ").";
    }

    @Override
    protected List<Command> usefulCommands(TurnContext ctx) {
        return ctx.restaurant().orders().stream()
                .filter(o -> o.status() == OrderStatus.IN_PREPARATION)
                .map(o -> Command.cook(o.customer().id()))
                .toList();
    }
}
