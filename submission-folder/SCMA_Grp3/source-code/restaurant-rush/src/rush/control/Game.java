package rush.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import rush.model.Customer;
import rush.model.Restaurant;
import rush.model.RuleViolation;
import rush.policy.HappyHour;
import rush.policy.PricingPolicy;
import rush.staff.ActionResult;
import rush.staff.Cashier;
import rush.staff.Command;
import rush.staff.DirectedStaff;
import rush.staff.StaffMember;
import rush.staff.TurnContext;

/**
 * Runs the turn sequence. This is the only class that knows the order of the
 * six steps; the rules of each step live in the domain and staff objects.
 * <p>
 * A turn is driven by the interface like this:
 * <pre>
 *   beginTurn()                     step 1  arrivals (and Happy Hour recovery)
 *   startHappyHour()                optional, before any staff member acts
 *   direct(staff, command) ...      step 2-3 manager choices, kitchen and serving
 *   endTurn()                       steps 4-5 eating, payment, waiting, abandonment
 *   ... after the last turn: result()   step 6
 * </pre>
 */
public class Game {

    private final GameConfig config;
    private final Restaurant restaurant;
    private final List<DirectedStaff> directedStaff;
    private final Cashier cashier;
    private final HappyHour happyHour;
    private final PricingPolicy pricing;

    private int turn;
    private boolean turnInProgress;
    private boolean staffHaveActed;

    public Game(GameConfig config) {
        this.config = config;
        this.restaurant = new Restaurant(config.menu(), config.tableCount());
        this.directedStaff = config.hireDirectedStaff();
        this.cashier = config.hireCashier();
        this.happyHour = new HappyHour();
        this.pricing = new PricingPolicy(List.of(happyHour));
    }

    // ------------------------------------------------------------------
    // Turn sequence
    // ------------------------------------------------------------------

    /** Starts the next turn: staff are refreshed and the scheduled customer arrives (step 1). */
    public List<GameEvent> beginTurn() {
        if (turnInProgress) {
            throw new IllegalStateException("Turn " + turn + " has not ended yet");
        }
        if (isOver()) {
            throw new IllegalStateException("The game is over");
        }
        turn++;
        turnInProgress = true;
        staffHaveActed = false;
        allStaff().forEach(StaffMember::startTurn);

        List<GameEvent> events = new ArrayList<>();
        Optional<Customer> arrival = config.arrivals().arrivingOn(turn);
        arrival.ifPresent(customer -> {
            restaurant.admit(customer);
            events.add(new GameEvent(GameEvent.Kind.ARRIVAL, customer.label() + " (" + customer.type()
                    + ") arrives and joins the queue with satisfaction " + customer.satisfaction() + "."));
        });
        events.addAll(happyHourRecovery());
        return events;
    }

    /**
     * Switches Happy Hour on for this turn and the next. Allowed once per game,
     * at the start of a turn (before any staff member has acted).
     */
    public ActionResult startHappyHour() {
        requireTurnInProgress();
        if (staffHaveActed) {
            return ActionResult.rejected("Happy Hour can only start at the beginning of a turn, "
                    + "before any staff member acts.");
        }
        try {
            happyHour.activate(turn);
        } catch (RuleViolation violation) {
            return ActionResult.rejected(violation.getMessage());
        }
        List<GameEvent> recovery = happyHourRecovery();
        return ActionResult.done("Happy Hour is ON for turns " + happyHour.startTurn() + "-"
                + happyHour.lastTurn() + " (" + happyHour.turnsRemaining(turn) + " turns left, including this one): "
                + HappyHour.DISCOUNT_PERCENT + "% off orders taken, " + recovery.size()
                + (recovery.size() == 1 ? " waiting guest" : " waiting guests")
                + " +" + HappyHour.RECOVERY + " satisfaction.");
    }

    /** True if Happy Hour has not been used and no staff member has acted yet this turn. */
    public boolean canStartHappyHour() {
        return turnInProgress && !staffHaveActed && happyHour.isAvailable();
    }

    private List<GameEvent> happyHourRecovery() {
        List<GameEvent> events = new ArrayList<>();
        for (Customer customer : happyHour.refresh(restaurant, turn)) {
            events.add(new GameEvent(GameEvent.Kind.POLICY, "Happy Hour: " + customer.label()
                    + " perks up to satisfaction " + customer.satisfaction() + "."));
        }
        return events;
    }

    /**
     * Gives one instruction to a staff member (steps 2-3). The Waiter is asked
     * before the Chef, so a dish the Chef finishes this turn cannot be served
     * until next turn. A rejected instruction costs nothing.
     */
    public ActionResult direct(DirectedStaff staff, Command command) {
        requireTurnInProgress();
        ActionResult result = staff.perform(command, context());
        staffHaveActed |= result.succeeded();
        return result;
    }

    /** What a staff member could usefully do right now. */
    public List<Command> options(DirectedStaff staff) {
        return staff.options(context());
    }

    /** Ends the turn: eating, payment (step 4), then waiting and abandonment (step 5). */
    public List<GameEvent> endTurn() {
        requireTurnInProgress();
        List<GameEvent> events = new ArrayList<>();
        TurnContext ctx = context();

        // Step 4a: customers served on an earlier turn finish eating.
        for (Customer customer : restaurant.guests()) {
            if (customer.finishEating(turn)) {
                events.add(new GameEvent(GameEvent.Kind.EATING,
                        customer.label() + " finishes eating and is ready to pay."));
            }
        }

        // Step 4b: the Cashier takes at most one payment, oldest first.
        cashier.collectPayment(ctx).ifPresent(payment -> events.add(new GameEvent(GameEvent.Kind.PAYMENT,
                cashier.name() + " collects " + payment.amount() + " from " + payment.customer().label()
                        + " (satisfaction " + payment.customer().satisfaction() + " recorded). Table "
                        + payment.tableNo() + " is free. \"" + payment.customer().remarkOnPaying() + "\"")));

        // Step 5: everyone not yet served loses patience; anyone at 0 walks out.
        for (Customer customer : restaurant.awaitingService()) {
            int before = customer.satisfaction();
            int lost = customer.loseSatisfaction();
            events.add(new GameEvent(GameEvent.Kind.PATIENCE, customer.label() + " ("
                    + customer.statusDescription() + "): satisfaction " + before + " -> "
                    + customer.satisfaction() + " (-" + lost + ")"));
            if (customer.isUnhappy()) {
                restaurant.depart(customer);
                events.add(new GameEvent(GameEvent.Kind.DEPARTURE,
                        customer.label() + " has had enough and leaves without paying."));
            }
        }

        turnInProgress = false;
        return events;
    }

    /** Step 6: the result, available once every turn has been played. */
    public GameResult result() {
        if (!isOver()) {
            throw new IllegalStateException("The result is only known after turn " + config.maxTurns());
        }
        return config.objective().evaluate(scoreboard());
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    /** The game always runs every turn; there is no early ending. */
    public boolean isOver() {
        return turn >= config.maxTurns() && !turnInProgress;
    }

    public Scoreboard scoreboard() {
        return new Scoreboard(restaurant.servedCount(), restaurant.paidCustomers().size(),
                restaurant.departureCount(), restaurant.revenue(), restaurant.averagePaidSatisfaction(),
                turn, config.maxTurns());
    }

    public int turn() {
        return turn;
    }

    public GameConfig config() {
        return config;
    }

    public Restaurant restaurant() {
        return restaurant;
    }

    /** Staff who take instructions, in the order they act. */
    public List<DirectedStaff> directedStaff() {
        return directedStaff;
    }

    public Cashier cashier() {
        return cashier;
    }

    /** Read access for display: availability, active turns and turns remaining. */
    public HappyHour happyHour() {
        return happyHour;
    }

    /** The customer the Cashier will charge at the end of this turn, if any. */
    public Optional<Customer> nextToPay() {
        return cashier.nextToPay(context());
    }

    private List<StaffMember> allStaff() {
        List<StaffMember> all = new ArrayList<>(directedStaff);
        all.add(cashier);
        return all;
    }

    private TurnContext context() {
        return new TurnContext(turn, restaurant, pricing);
    }

    private void requireTurnInProgress() {
        if (!turnInProgress) {
            throw new IllegalStateException("No turn in progress");
        }
    }
}
