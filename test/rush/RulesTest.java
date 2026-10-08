package rush;

import static rush.TestKit.baseGame;
import static rush.TestKit.check;
import static rush.TestKit.equal;
import static rush.TestKit.ok;
import static rush.TestKit.rejected;

import java.util.ArrayList;
import java.util.List;

import rush.control.ArrivalSchedule;
import rush.control.Game;
import rush.control.GameConfig;
import rush.control.Objective;
import rush.model.Customer;
import rush.model.CustomerStatus;
import rush.model.Menu;
import rush.model.ComboMeal;
import rush.model.Dish;
import rush.model.Money;
import rush.model.OrderStatus;
import rush.model.CriticCustomer;
import rush.model.RegularCustomer;
import rush.model.VIPCustomer;
import rush.policy.HappyHour;
import rush.policy.PricingPolicy;
import rush.staff.Command;

/**
 * Checks the rules of the brief against the real game objects.
 * Run with {@code ./test.sh}.
 */
public final class RulesTest {

    private RulesTest() {
    }

    public static void main(String[] args) {
        TestKit kit = new TestKit();
        System.out.println();
        System.out.println("  Restaurant Rush rule checks");
        System.out.println();

        kit.test("money is kept in cents and shown with two decimals", RulesTest::money);
        kit.test("worked example 3.3: Pasta from arrival to payment", RulesTest::workedExample);
        kit.test("a dish finished this turn is served next turn at the earliest", RulesTest::readyThenServe);
        kit.test("an invalid choice explains why and does not use the task", RulesTest::invalidChoice);
        kit.test("the Waiter completes at most one task per turn", RulesTest::oneTaskPerTurn);
        kit.test("the Cashier takes one payment per turn, oldest first", RulesTest::cashierOldestFirst);
        kit.test("a customer at 0 leaves, order cancelled, table freed, no revenue", RulesTest::abandonment);
        kit.test("served, eating and paying customers lose no satisfaction", RulesTest::noLossAfterServing);
        kit.test("customers choose their own food by rotating through the menu", RulesTest::foodChoice);
        kit.test("the game always runs all 18 turns", RulesTest::alwaysEighteenTurns);
        kit.test("VIP: loses 5 per turn and gets 10% off", RulesTest::vipBehaviour);
        kit.test("Critic: loses 12 per turn and orders the most elaborate dish", RulesTest::criticBehaviour);
        kit.test("base setting has six arrivals including a VIP and a Critic", RulesTest::stageTwoArrivals);
        kit.test("all customer types follow the same order and service flow", RulesTest::sameFlowForAllTypes);
        kit.test("combo: summed units, 90% of the combined price", RulesTest::comboMeal);
        kit.test("Host seats one customer in addition to the Waiter", RulesTest::hostSeatsToo);
        kit.test("Happy Hour: once per game, this turn and the next", RulesTest::happyHourDuration);
        kit.test("Happy Hour: 20% off after the VIP discount, locked when ordered", RulesTest::happyHourPricing);
        kit.test("Happy Hour: waiting customers recover 15, capped at 100", RulesTest::happyHourRecovery);
        kit.test("Happy Hour only starts before any staff member acts", RulesTest::happyHourStartOfTurn);
        kit.test("scripted victory run ends in Victory", RulesTest::scriptedVictory);
        kit.test("scripted defeat run ends in Defeat", RulesTest::scriptedDefeat);

        System.exit(kit.finish());
    }

    // ------------------------------------------------------------------

    private static void money() {
        equal("$24.30", Money.ofCents(2430).toString(), "format");
        equal("$0.05", Money.ofCents(5).toString(), "small amounts");
        equal(Money.dollars(42), Money.dollars(9).plus(Money.dollars(15)).plus(Money.dollars(18)), "sum");
    }

    /** One customer, one Pasta, full attention: matches the table in section 3.3 of the brief. */
    private static void workedExample() {
        Menu pastaFirst = new Menu(List.of(new Dish("Pasta", Money.dollars(18), 2)));
        GameConfig config = new GameConfig("Example", 18, 2, pastaFirst,
                new ArrivalSchedule().at(1, "Ada", RegularCustomer::new), Objective.base());
        Game game = new Game(config);

        game.beginTurn();
        ok(game, "Waiter", Command.seat(1, 1));
        game.endTurn();
        Customer ada = game.restaurant().customer(1);
        equal(92, ada.satisfaction(), "turn 1");

        game.beginTurn();
        ok(game, "Waiter", Command.takeOrder(1));
        ok(game, "Chef", Command.cook(1));
        game.endTurn();
        equal(84, ada.satisfaction(), "turn 2");

        game.beginTurn();
        ok(game, "Chef", Command.cook(1));
        equal(OrderStatus.READY, ada.order().orElseThrow().status(), "READY on turn 3");
        game.endTurn();
        equal(76, ada.satisfaction(), "turn 3");

        game.beginTurn();
        ok(game, "Waiter", Command.serve(1));
        equal(OrderStatus.SERVED, ada.order().orElseThrow().status(), "SERVED on turn 4");
        game.endTurn();
        equal(76, ada.satisfaction(), "turn 4, no further loss");

        game.beginTurn();
        game.endTurn();
        equal(CustomerStatus.READY_TO_PAY, ada.status(), "READY_TO_PAY on turn 5");
        equal(Money.ZERO, game.restaurant().revenue(), "cannot pay on the turn they become ready");

        game.beginTurn();
        game.endTurn();
        equal(CustomerStatus.PAID, ada.status(), "PAID on turn 6");
        equal(Money.dollars(18), game.restaurant().revenue(), "revenue");
        equal(76.0, game.scoreboard().averageSatisfaction().getAsDouble(), "recorded satisfaction");
        check(game.restaurant().table(1).isFree(), "table freed on payment");
    }

    private static void readyThenServe() {
        Game game = baseGame();
        game.beginTurn();
        ok(game, "Waiter", Command.seat(1, 1));
        game.endTurn();
        game.beginTurn();
        ok(game, "Waiter", Command.takeOrder(1));
        ok(game, "Chef", Command.cook(1));   // Salad is READY now, but the Waiter has already acted
        check(!TestKit.staff(game, "Waiter").hasTaskLeft(), "waiter acted before the chef");
        game.endTurn();
        game.beginTurn();
        ok(game, "Waiter", Command.serve(1)); // next turn it can be served
    }

    private static void invalidChoice() {
        Game game = baseGame();
        game.beginTurn();
        String why = rejected(game, "Waiter", Command.takeOrder(1));
        check(why.contains("no table"), "explains: " + why);
        why = rejected(game, "Waiter", Command.serve(1));
        check(why.contains("hasn't ordered"), "explains: " + why);
        why = rejected(game, "Waiter", Command.seat(1, 3));
        check(why.contains("no table 3"), "explains: " + why);
        why = rejected(game, "Waiter", Command.cook(1));
        check(why.contains("can't cook"), "explains: " + why);
        why = rejected(game, "Chef", Command.cook(9));
        check(why.contains("no customer #9"), "explains: " + why);
        check(TestKit.staff(game, "Waiter").hasTaskLeft(), "waiter still has the task");
        ok(game, "Waiter", Command.seat(1, 1));
    }

    private static void oneTaskPerTurn() {
        Game game = baseGame();
        game.beginTurn();
        ok(game, "Waiter", Command.seat(1, 1));
        String why = rejected(game, "Waiter", Command.takeOrder(1));
        check(why.contains("already used"), "explains: " + why);
    }

    /** Two customers finish eating on different turns; each pays on a later turn, one per turn. */
    private static void cashierOldestFirst() {
        Menu salads = new Menu(List.of(new Dish("Salad", Money.dollars(9), 1)));
        GameConfig config = new GameConfig("Two guests", 18, 2, salads,
                new ArrivalSchedule().at(1, "Ada", RegularCustomer::new).at(2, "Ben", RegularCustomer::new),
                Objective.base());
        Game game = new Game(config);
        game.beginTurn();                                     // T1
        ok(game, "Waiter", Command.seat(1, 1));
        game.endTurn();
        game.beginTurn();                                     // T2
        ok(game, "Waiter", Command.seat(2, 2));
        game.endTurn();
        game.beginTurn();                                     // T3
        ok(game, "Waiter", Command.takeOrder(1));
        ok(game, "Chef", Command.cook(1));
        game.endTurn();
        game.beginTurn();                                     // T4
        ok(game, "Waiter", Command.takeOrder(2));
        ok(game, "Chef", Command.cook(2));
        game.endTurn();
        game.beginTurn();                                     // T5
        ok(game, "Waiter", Command.serve(1));
        game.endTurn();
        game.beginTurn();                                     // T6: Ada finishes eating
        ok(game, "Waiter", Command.serve(2));
        game.endTurn();
        Customer ada = game.restaurant().customer(1);
        Customer ben = game.restaurant().customer(2);
        equal(CustomerStatus.READY_TO_PAY, ada.status(), "Ada ready on T6");
        game.beginTurn();                                     // T7: Ada pays, Ben finishes eating
        game.endTurn();
        equal(CustomerStatus.PAID, ada.status(), "Ada paid on T7");
        equal(CustomerStatus.READY_TO_PAY, ben.status(), "Ben ready on T7, pays later");
        game.beginTurn();                                     // T8
        game.endTurn();
        equal(CustomerStatus.PAID, ben.status(), "Ben paid on T8");
        equal(Money.dollars(18), game.restaurant().revenue(), "revenue");
    }

    private static void abandonment() {
        GameConfig config = new GameConfig("Ada alone", 18, 2, GameConfig.base().menu(),
                new ArrivalSchedule().at(1, "Ada", RegularCustomer::new), Objective.base());
        Game game = new Game(config);
        game.beginTurn();
        ok(game, "Waiter", Command.seat(1, 1));
        game.endTurn();
        game.beginTurn();
        ok(game, "Waiter", Command.takeOrder(1));          // Salad ordered but never cooked
        game.endTurn();
        Customer ada = game.restaurant().customer(1);
        for (int t = 3; t <= 13; t++) {
            game.beginTurn();
            game.endTurn();
        }
        equal(CustomerStatus.LEFT, ada.status(), "left after reaching 0");
        equal(0, ada.satisfaction(), "satisfaction floor");
        equal(OrderStatus.CANCELLED, ada.order().orElseThrow().status(), "order cancelled");
        check(game.restaurant().table(1).isFree(), "table freed");
        equal(Money.ZERO, game.restaurant().revenue(), "no revenue from abandoned orders");
        equal(1, game.scoreboard().departures(), "one unhappy departure");
    }

    private static void noLossAfterServing() {
        Game game = baseGame();
        game.beginTurn();
        ok(game, "Waiter", Command.seat(1, 1));
        game.endTurn();
        game.beginTurn();
        ok(game, "Waiter", Command.takeOrder(1));
        ok(game, "Chef", Command.cook(1));
        game.endTurn();
        game.beginTurn();
        ok(game, "Waiter", Command.serve(1));
        game.endTurn();
        Customer ada = game.restaurant().customer(1);
        int afterServing = ada.satisfaction();
        game.beginTurn();
        game.endTurn();   // eating -> ready to pay
        game.beginTurn();
        game.endTurn();   // pays
        equal(afterServing, ada.satisfaction(), "no loss once served");
    }

    private static void foodChoice() {
        Game game = baseGame();
        String[] expected = {"Salad", "Burger", "Pasta", "Salad + Burger Combo", "Salad + Pasta Combo", "Salad"};
        Menu menu = game.restaurant().menu();
        for (int id = 1; id <= 6; id++) {
            Customer c = new RegularCustomer(id, "Guest", 1);
            equal(expected[id - 1], c.chooseFrom(menu).name(), "customer #" + id);
        }
    }

    private static void vipBehaviour() {
        Customer vip = new VIPCustomer(1, "Vera", 1);
        equal(5, vip.satisfactionLossPerTurn(), "loss per turn");
        equal(Money.ofCents(1350), vip.billFor(Money.dollars(15)), "Burger with 10% off");
        equal(Money.ofCents(1620), vip.billFor(Money.dollars(18)), "Pasta with 10% off");
        equal(Money.ofCents(810), vip.billFor(Money.dollars(9)), "Salad with 10% off");
        vip.loseSatisfaction();
        equal(95, vip.satisfaction(), "after one waiting turn");
    }

    private static void criticBehaviour() {
        Game game = baseGame();
        Customer critic = new CriticCustomer(2, "Ben", 4);   // rotation alone would give a Burger
        equal(12, critic.satisfactionLossPerTurn(), "loss per turn");
        equal("Salad + Pasta Combo", critic.chooseFrom(game.restaurant().menu()).name(),
                "most elaborate item (3 units)");
        Menu dishesOnly = new Menu(List.of(new Dish("Salad", Money.dollars(9), 1),
                new Dish("Burger", Money.dollars(15), 1), new Dish("Pasta", Money.dollars(18), 2)));
        equal("Pasta", critic.chooseFrom(dishesOnly).name(), "most elaborate dish on the Stage 2 menu");
        equal(Money.dollars(18), critic.billFor(Money.dollars(18)), "full price");
        for (int i = 0; i < 9; i++) {
            critic.loseSatisfaction();
        }
        equal(0, critic.satisfaction(), "floor at 0 (100 - 9 x 12 < 0)");
    }

    private static void stageTwoArrivals() {
        List<String> types = new ArrayList<>();
        Game game = baseGame();
        while (!game.isOver()) {
            game.beginTurn();
            game.endTurn();
        }
        game.restaurant().guests().forEach(c -> types.add(c.type()));
        equal(6, types.size(), "six arrivals");
        check(types.contains("VIP") && types.contains("Critic"), "VIP and Critic present: " + types);
    }

    /** A VIP goes through exactly the same seat, order, cook, serve, eat, pay steps; only the bill differs. */
    private static void sameFlowForAllTypes() {
        Menu burgers = new Menu(List.of(new Dish("Burger", Money.dollars(15), 1)));
        GameConfig config = new GameConfig("VIP only", 18, 2, burgers,
                new ArrivalSchedule().at(1, "Vera", VIPCustomer::new), Objective.base());
        Game game = new Game(config);
        game.beginTurn();
        ok(game, "Waiter", Command.seat(1, 1));
        game.endTurn();
        game.beginTurn();
        ok(game, "Waiter", Command.takeOrder(1));
        ok(game, "Chef", Command.cook(1));
        game.endTurn();
        game.beginTurn();
        ok(game, "Waiter", Command.serve(1));
        game.endTurn();
        game.beginTurn();
        game.endTurn();
        game.beginTurn();
        game.endTurn();
        Customer vera = game.restaurant().customer(1);
        equal(CustomerStatus.PAID, vera.status(), "paid on turn 5");
        equal(90, vera.satisfaction(), "two waiting turns at -5");
        equal(Money.ofCents(1350), game.restaurant().revenue(), "discounted bill");
    }

    private static void alwaysEighteenTurns() {
        Game game = baseGame();
        int turns = 0;
        while (!game.isOver()) {
            game.beginTurn();
            game.endTurn();
            turns++;
        }
        equal(18, turns, "turns played");
        check(!game.result().isVictory(), "doing nothing is a defeat");
        equal("N/A", game.scoreboard().averageText(), "average with nobody paid");
    }

    private static void comboMeal() {
        Dish salad = new Dish("Salad", Money.dollars(9), 1);
        Dish burger = new Dish("Burger", Money.dollars(15), 1);
        Dish pasta = new Dish("Pasta", Money.dollars(18), 2);
        ComboMeal saladPasta = new ComboMeal(salad, pasta);
        equal(Money.ofCents(2430), saladPasta.price(), "Salad + Pasta price");
        equal(3, saladPasta.prepUnits(), "Salad + Pasta units");
        ComboMeal saladBurger = new ComboMeal(salad, burger);
        equal(Money.ofCents(2160), saladBurger.price(), "Salad + Burger price");
        equal(2, saladBurger.prepUnits(), "Salad + Burger units");
        equal(Money.ofCents(2970), new ComboMeal(burger, pasta).price(), "Burger + Pasta price");
    }

    private static void hostSeatsToo() {
        GameConfig config = new GameConfig("Two early guests", 18, 2, GameConfig.base().menu(),
                new ArrivalSchedule().at(1, "Ada", RegularCustomer::new).at(2, "Ben", RegularCustomer::new),
                Objective.base());
        Game game = new Game(config);
        game.beginTurn();
        game.endTurn();                                   // nobody seats Ada on turn 1
        game.beginTurn();
        String why = rejected(game, "Host", Command.takeOrder(1));
        check(why.contains("can't order"), "the Host only seats: " + why);
        ok(game, "Host", Command.seat(1, 1));
        why = rejected(game, "Host", Command.seat(2, 2));
        check(why.contains("already used"), "one seat per turn for the Host: " + why);
        ok(game, "Waiter", Command.seat(2, 2));          // both seated in the same turn
        check(game.restaurant().queue().isEmpty(), "queue empty");
        game.endTurn();

        Game second = new Game(config);
        second.beginTurn();
        ok(second, "Host", Command.seat(1, 1));
        ok(second, "Waiter", Command.takeOrder(1));      // order of a customer the Host just seated
        ok(second, "Chef", Command.cook(1));
    }

    private static void happyHourDuration() {
        HappyHour happyHour = new HappyHour();
        check(happyHour.isAvailable(), "available at the start");
        happyHour.activate(5);
        equal(2, happyHour.turnsRemaining(5), "remaining on turn 5");
        equal(1, happyHour.turnsRemaining(6), "remaining on turn 6");
        equal(0, happyHour.turnsRemaining(7), "over on turn 7");
        check(!happyHour.isActiveOn(4) && happyHour.isActiveOn(5) && happyHour.isActiveOn(6)
                && !happyHour.isActiveOn(7), "active on turns 5 and 6 only");
        try {
            happyHour.activate(9);
            throw new AssertionError("a second activation should be refused");
        } catch (rush.model.RuleViolation expected) {
            check(expected.getMessage().contains("once per game"), "explains: " + expected.getMessage());
        }
    }

    private static void happyHourPricing() {
        HappyHour happyHour = new HappyHour();
        PricingPolicy pricing = new PricingPolicy(List.of(happyHour));
        Dish burger = new Dish("Burger", Money.dollars(15), 1);
        Customer vip = new VIPCustomer(1, "Vera", 1);
        Customer regular = new RegularCustomer(2, "Rex", 1);
        equal(Money.ofCents(1350), pricing.quote(vip, burger, 3), "VIP before Happy Hour");
        happyHour.activate(4);
        equal(Money.ofCents(1080), pricing.quote(vip, burger, 4), "VIP Burger in Happy Hour: 15 x 0.9 x 0.8");
        equal(Money.dollars(12), pricing.quote(regular, burger, 5), "Regular Burger in Happy Hour");
        equal(Money.dollars(15), pricing.quote(regular, burger, 6), "after Happy Hour");

        // In a real game the price stays as it was when the order was taken.
        Game game = baseGame();
        game.beginTurn();
        ok(game, "Host", Command.seat(1, 1));
        ok(game, "Waiter", Command.takeOrder(1));
        game.endTurn();
        game.beginTurn();
        check(game.startHappyHour().succeeded(), "Happy Hour starts on turn 2");
        equal(Money.dollars(9), game.restaurant().customer(1).order().orElseThrow().price(),
                "an order placed before Happy Hour keeps its price");
    }

    private static void happyHourRecovery() {
        Game game = baseGame();
        game.beginTurn();                                 // Ada waits in the queue: 100 -> 92
        game.endTurn();
        game.beginTurn();                                 // 92 -> 84
        game.endTurn();
        game.beginTurn();
        check(game.startHappyHour().succeeded(), "starts");
        Customer ada = game.restaurant().customer(1);
        equal(99, ada.satisfaction(), "84 + 15 on the first Happy Hour turn");
        game.endTurn();                                   // 99 -> 91
        game.beginTurn();                                 // second turn: 91 + 15 capped at 100
        equal(100, ada.satisfaction(), "capped at 100 on the second turn");
        game.endTurn();
        game.beginTurn();                                 // over: no more recovery
        equal(92, ada.satisfaction(), "no recovery after Happy Hour");
        check(!game.canStartHappyHour(), "cannot start again");
        String why = game.startHappyHour().message();
        check(why.contains("once per game"), "explains: " + why);
    }

    private static void happyHourStartOfTurn() {
        Game game = baseGame();
        game.beginTurn();
        ok(game, "Host", Command.seat(1, 1));
        check(!game.canStartHappyHour(), "not offered once staff have acted");
        String why = game.startHappyHour().message();
        check(why.contains("beginning of a turn"), "explains: " + why);
    }

    private static void scriptedVictory() {
        String transcript = TestKit.playScript("scripts/stage3-victory.txt");
        check(transcript.contains("V I C T O R Y"), "victory banner");
        check(transcript.contains("Revenue $92.16"), "revenue $92.16");
    }

    private static void scriptedDefeat() {
        String transcript = TestKit.playScript("scripts/stage3-defeat.txt");
        check(transcript.contains("D E F E A T"), "defeat banner");
    }
}
