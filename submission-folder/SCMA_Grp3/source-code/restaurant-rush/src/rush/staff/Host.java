package rush.staff;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The Host seats at most one queued customer per turn, in addition to the
 * Waiter's task. The Host acts first, so the Waiter may take the order of a
 * customer the Host has just seated.
 */
public class Host extends DirectedStaff {

    public Host(String name) {
        super(name, 1);
    }

    @Override
    public String role() {
        return "Host";
    }

    @Override
    public Set<Verb> duties() {
        return EnumSet.of(Verb.SEAT);
    }

    @Override
    protected String carryOut(Command command, TurnContext ctx) {
        return seatCustomer(name(), command, ctx);
    }

    @Override
    protected List<Command> usefulCommands(TurnContext ctx) {
        return seatingOptions(ctx);
    }
}
