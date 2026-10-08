package rush.control;

import java.util.List;

import rush.model.CriticCustomer;
import rush.model.Menu;
import rush.model.MenuItem;
import rush.model.Money;
import rush.model.RegularCustomer;
import rush.model.VIPCustomer;
import rush.staff.Cashier;
import rush.staff.Chef;
import rush.staff.DirectedStaff;
import rush.staff.Waiter;

/**
 * The setting a game is played under. {@link #base()} is the required,
 * reproducible base setting from the brief.
 */
public final class GameConfig {

    private final String name;
    private final int maxTurns;
    private final int tableCount;
    private final Menu menu;
    private final ArrivalSchedule arrivals;
    private final Objective objective;

    public GameConfig(String name, int maxTurns, int tableCount, Menu menu,
                      ArrivalSchedule arrivals, Objective objective) {
        this.name = name;
        this.maxTurns = maxTurns;
        this.tableCount = tableCount;
        this.menu = menu;
        this.arrivals = arrivals;
        this.objective = objective;
    }

    /**
     * 18 turns, 2 tables, one Waiter, Chef and Cashier, one customer at the
     * start of turns 1, 4, 7, 10, 13 and 16. Two of the six are a Critic and a
     * VIP; the rest are Regulars.
     */
    public static GameConfig base() {
        Menu menu = new Menu(List.of(
                new MenuItem("Salad", Money.dollars(9), 1),
                new MenuItem("Burger", Money.dollars(15), 1),
                new MenuItem("Pasta", Money.dollars(18), 2)));
        ArrivalSchedule arrivals = new ArrivalSchedule()
                .at(1, "Ada", RegularCustomer::new)
                .at(4, "Ben", CriticCustomer::new)
                .at(7, "Chloe", VIPCustomer::new)
                .at(10, "Dev", RegularCustomer::new)
                .at(13, "Elif", RegularCustomer::new)
                .at(16, "Farid", RegularCustomer::new);
        return new GameConfig("Base setting", 18, 2, menu, arrivals, Objective.base());
    }

    /** The staff who take instructions, in the order they act each turn. */
    public List<DirectedStaff> hireDirectedStaff() {
        return List.of(new Waiter("Wendy"), new Chef("Carlo"));
    }

    public Cashier hireCashier() {
        return new Cashier("Cassie");
    }

    public String name() {
        return name;
    }

    public int maxTurns() {
        return maxTurns;
    }

    public int tableCount() {
        return tableCount;
    }

    public Menu menu() {
        return menu;
    }

    public ArrivalSchedule arrivals() {
        return arrivals;
    }

    public Objective objective() {
        return objective;
    }
}
