package rush.staff;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import rush.model.Customer;
import rush.model.Restaurant;
import rush.model.RuleViolation;
import rush.model.Table;

/**
 * A staff member who acts on the manager's instructions each turn.
 * <p>
 * {@link #perform} is a template method: it checks the task budget and that the
 * instruction is part of this role's job, lets the subclass carry it out, and
 * only spends the task if the action succeeded. A broken rule is reported back
 * to the manager without using up the turn.
 */
public abstract class DirectedStaff extends StaffMember {

    protected DirectedStaff(String name, int tasksPerTurn) {
        super(name, tasksPerTurn);
    }

    /** The instructions this role understands, apart from {@link Verb#WAIT}. */
    public abstract Set<Verb> duties();

    /** Carries out an instruction that is one of {@link #duties()}; returns what happened. */
    protected abstract String carryOut(Command command, TurnContext ctx);

    /** The instructions that would currently succeed, apart from waiting. */
    protected abstract List<Command> usefulCommands(TurnContext ctx);

    public final ActionResult perform(Command command, TurnContext ctx) {
        if (!hasTaskLeft()) {
            return ActionResult.rejected(title() + " has already used this turn's task.");
        }
        if (command.verb() == Verb.WAIT) {
            useTask();
            return ActionResult.done(title() + " waits this turn.");
        }
        if (!duties().contains(command.verb())) {
            return ActionResult.rejected("The " + role() + " can't " + command.verb().keyword()
                    + ". " + role() + " commands: " + dutyList() + ".");
        }
        try {
            String outcome = carryOut(command, ctx);
            useTask();
            return ActionResult.done(outcome);
        } catch (RuleViolation violation) {
            return ActionResult.rejected(violation.getMessage());
        }
    }

    /** Commands that would succeed right now, ending with "wait". */
    public List<Command> options(TurnContext ctx) {
        List<Command> options = new ArrayList<>(usefulCommands(ctx));
        options.add(Command.waitTurn());
        return options;
    }

    /** e.g. "seat, order, serve, wait". */
    public String dutyList() {
        return duties().stream().map(Verb::keyword).collect(Collectors.joining(", ")) + ", wait";
    }

    // ------------------------------------------------------------------
    // Helpers shared by roles
    // ------------------------------------------------------------------

    protected static Customer requireCustomer(Command command, TurnContext ctx) {
        if (command.customerId() == null) {
            throw new RuleViolation("Which customer? For example: " + command.verb().keyword() + " 2");
        }
        return ctx.restaurant().customer(command.customerId());
    }

    /** Seats the commanded customer at the commanded table, or at the first free table. */
    protected static String seatCustomer(String who, Command command, TurnContext ctx) {
        Restaurant restaurant = ctx.restaurant();
        Customer customer = requireCustomer(command, ctx);
        Table table = command.tableNo() != null
                ? restaurant.table(command.tableNo())
                : restaurant.firstFreeTable()
                        .orElseThrow(() -> new RuleViolation("Every table is occupied."));
        restaurant.seat(customer, table);
        return who + " seats " + customer.label() + " at table " + table.number() + ".";
    }

    /** One "seat" suggestion per queued customer, at the first free table. */
    protected static List<Command> seatingOptions(TurnContext ctx) {
        Restaurant restaurant = ctx.restaurant();
        return restaurant.firstFreeTable()
                .map(table -> restaurant.queue().stream()
                        .map(c -> Command.seat(c.id(), table.number()))
                        .toList())
                .orElse(List.of());
    }
}
