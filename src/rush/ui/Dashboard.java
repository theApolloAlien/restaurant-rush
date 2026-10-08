package rush.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import rush.control.Game;
import rush.control.GameEvent;
import rush.control.GameResult;
import rush.control.Objective;
import rush.control.Scoreboard;
import rush.model.Customer;
import rush.model.CustomerStatus;
import rush.model.Menu;
import rush.model.MenuItem;
import rush.model.Order;
import rush.model.OrderStatus;
import rush.model.Table;
import rush.staff.ActionResult;
import rush.staff.Command;
import rush.staff.DirectedStaff;

/**
 * Draws the game state. It only reads from the game; it never changes it.
 */
public final class Dashboard {

    /** Inner width of the boxes; everything is laid out to fit an 80-column terminal. */
    private static final int WIDTH = 72;

    private final Terminal term;
    private final Theme t;

    public Dashboard(Terminal term) {
        this.term = term;
        this.t = term.theme();
    }

    // ------------------------------------------------------------------
    // Title and static screens
    // ------------------------------------------------------------------

    public void title(String edition) {
        term.println();
        if (t.unicode()) {
            term.println("   " + t.accent("╦═╗┌─┐┌─┐┌┬┐┌─┐┬ ┬┬─┐┌─┐┌┐┌┌┬┐  ╦═╗┬ ┬┌─┐┬ ┬"));
            term.println("   " + t.tomato("╠╦╝├┤ └─┐ │ ├─┤│ │├┬┘├─┤│││ │   ╠╦╝│ │└─┐├─┤"));
            term.println("   " + t.red("╩╚═└─┘└─┘ ┴ ┴ ┴└─┘┴└─┴ ┴┘└┘ ┴   ╩╚═└─┘└─┘┴ ┴"));
        } else {
            term.println("   " + t.accent("R E S T A U R A N T   R U S H"));
        }
        term.println("   " + t.gray("a turn-based restaurant management game ") + t.dim(t.bullet()) + " "
                + t.gold(edition));
        term.println();
    }

    public void mainMenu() {
        term.println("   " + t.code("1") + "  Play the base setting");
        term.println("   " + t.code("2") + "  How to play");
        term.println("   " + t.code("3") + "  Quit");
        term.println();
    }

    public void help(Game game) {
        section("HOW TO PLAY");
        paragraph("You are the manager. Every turn, decide what each member of staff does.",
                "Customers arrive at the start of turns 1, 4, 7, 10, 13 and 16. Anyone not yet",
                "served loses satisfaction at the end of every turn and leaves at 0.",
                "The Waiter acts before the Chef, so a dish finished this turn is served next turn.",
                "A served customer eats for one turn, then the Cashier collects one bill per turn.");
        term.println();
        section("GUESTS");
        guestType(t.gray("Regular"), "loses 8 per waiting turn", "pays full price");
        guestType(t.gold("VIP"), "loses 5 per waiting turn", "10% off the bill");
        guestType(t.purple("Critic"), "loses 12 per waiting turn", "always orders the most elaborate dish");
        term.println();
        section("COMMANDS");
        commandHelp("seat <customer> [table]", "Waiter: seat a queued customer (default: first free table)");
        commandHelp("order <customer>", "Waiter: take a seated customer's order (they choose the food)");
        commandHelp("serve <customer>", "Waiter: serve that customer's READY dish");
        commandHelp("cook <customer>", "Chef: spend one preparation unit on that customer's order");
        commandHelp("wait", "This member of staff does nothing this turn");
        commandHelp("board  menu  help  quit", "Redraw the board, show the menu, this help, leave the game");
        term.println("     " + t.gray("Short forms: s 2 1 " + t.bullet() + " o 2 " + t.bullet() + " v 2 "
                + t.bullet() + " c 2 " + t.bullet() + " w"));
        term.println();
        if (game != null) {
            objective(game.config().objective());
        }
    }

    public void menu(Menu menu) {
        section("MENU");
        for (MenuItem item : menu.items()) {
            term.println("     " + Theme.padRight(t.bold(item.name()), 26)
                    + Theme.padLeft(item.price().toString(), 8) + "   "
                    + t.pips(item.prepUnits(), item.prepUnits()) + " " + t.gray(units(item.prepUnits())));
        }
        term.println("     " + t.gray("Customers choose their own food: guest #n orders item n on the board,"));
        term.println("     " + t.gray("wrapping around (Salad, Burger, Pasta, Salad, ...). A Critic always"));
        term.println("     " + t.gray("orders the most elaborate dish (most preparation units)."));
        term.println();
    }

    private void objective(Objective objective) {
        section("TO WIN, BY THE END OF TURN 18");
        term.println("     " + t.diamond() + " at least " + t.bold(String.valueOf(objective.minPaidCustomers()))
                + " paid customers");
        term.println("     " + t.diamond() + " revenue of at least " + t.bold(objective.minRevenue().toString()));
        term.println("     " + t.diamond() + " average satisfaction of paid customers at least "
                + t.bold(String.valueOf(objective.minAverageSatisfaction())));
        term.println();
    }

    // ------------------------------------------------------------------
    // Start of turn
    // ------------------------------------------------------------------

    public void turnHeader(Game game, String edition) {
        Scoreboard score = game.scoreboard();
        Objective goal = game.config().objective();

        String left = t.accent("TURN " + game.turn()) + t.gray(" of " + score.maxTurns()) + "   "
                + t.track(game.turn(), score.maxTurns());
        String right = t.gray(edition);

        boolean revenueMet = score.revenue().isAtLeast(goal.minRevenue());
        boolean paidMet = score.paidCount() >= goal.minPaidCustomers();
        boolean averageMet = score.averageSatisfaction().isPresent()
                && score.averageSatisfaction().getAsDouble() >= goal.minAverageSatisfaction();
        String stats = stat("Revenue", score.revenue().toString(), goal.minRevenue().toString(), revenueMet)
                + "   " + stat("Paid", String.valueOf(score.paidCount()),
                String.valueOf(goal.minPaidCustomers()), paidMet)
                + "   " + stat("Avg satisfaction", score.averageText(),
                String.valueOf(goal.minAverageSatisfaction()), averageMet)
                + "   " + t.gray("Left ") + (score.departures() > 0
                ? t.red(String.valueOf(score.departures())) : t.bold("0"));

        term.println();
        term.println("  " + t.faint(t.topLeft() + t.line(WIDTH + 2) + t.topRight()));
        boxLine(spread(left, right));
        term.println("  " + t.faint(t.teeLeft() + t.line(WIDTH + 2) + t.teeRight()));
        boxLine(stats);
        term.println("  " + t.faint(t.botLeft() + t.line(WIDTH + 2) + t.botRight()));
        term.println();
    }

    public void board(Game game) {
        queue(game);
        tables(game);
        kitchen(game);
        staff(game);
    }

    private void queue(Game game) {
        section("WAITING QUEUE");
        List<Customer> queue = game.restaurant().queue();
        if (queue.isEmpty()) {
            term.println("     " + t.faint("nobody waiting"));
        }
        for (Customer c : queue) {
            term.println("     " + customerCells(c) + "  " + t.gold(c.statusDescription())
                    + t.gray(" since turn " + c.arrivalTurn()));
        }
        term.println();
    }

    private void tables(Game game) {
        section("TABLES");
        for (Table table : game.restaurant().tables()) {
            String number = t.bold("T" + table.number());
            String row = table.occupant()
                    .map(c -> customerCells(c) + "  " + status(c))
                    .orElse(t.faint("free"));
            term.println("   " + number + "  " + row);
        }
        term.println();
    }

    private void kitchen(Game game) {
        section("KITCHEN");
        List<Order> active = game.restaurant().orders().stream().filter(Order::isActive).toList();
        if (active.isEmpty()) {
            term.println("     " + t.faint("no orders yet"));
        }
        for (Order order : active) {
            term.println("     " + Theme.padRight(customerName(order.customer()), 10)
                    + Theme.padRight(t.bold(order.item().name()), 24)
                    + Theme.padLeft(order.price().toString(), 7) + "   "
                    + Theme.padRight(t.pips(order.unitsDone(), order.unitsRequired()), 5)
                    + t.gray(order.unitsDone() + "/" + order.unitsRequired()) + "  "
                    + orderStatus(order.status()));
        }
        term.println();
    }

    private void staff(Game game) {
        section("STAFF TASKS AVAILABLE");
        for (DirectedStaff s : game.directedStaff()) {
            String options = game.options(s).stream()
                    .map(c -> t.code(c.toString()))
                    .collect(Collectors.joining(t.gray("  " + t.bullet() + "  ")));
            term.println("     " + Theme.padRight(t.bold(s.title()), 18) + options);
        }
        String cashier = game.nextToPay()
                .map(c -> "will charge " + c.label() + " at turn end")
                .orElse("nobody is ready to pay yet");
        term.println("     " + Theme.padRight(t.bold(game.cashier().title()), 18)
                + t.gray("automatic " + t.bullet() + " " + cashier));
        term.println();
    }

    // ------------------------------------------------------------------
    // Manager choices
    // ------------------------------------------------------------------

    public void staffPrompt(Game game, DirectedStaff staff) {
        String hints = game.options(staff).stream()
                .map(Command::toString)
                .map(t::code)
                .collect(Collectors.joining(t.gray("  " + t.bullet() + "  ")));
        term.println("  " + t.accent(t.diamond() + " " + staff.title().toUpperCase()) + t.gray("  "
                + staff.dutyList()));
        term.println("    " + t.gray("try ") + hints);
    }

    public String promptText(DirectedStaff staff) {
        return "  " + t.accent(staff.role().toLowerCase()) + " " + t.accent(t.arrow()) + " ";
    }

    public void actionResult(ActionResult result) {
        if (result.succeeded()) {
            term.println("    " + t.green(t.tick() + " " + result.message()));
            term.println();
        } else {
            term.println("    " + t.red(t.cross() + " " + result.message()));
            term.println("    " + t.gray("  Nothing was used up. Choose again."));
        }
    }

    public void error(String message) {
        term.println("    " + t.gold(t.cross() + " " + message));
    }

    // ------------------------------------------------------------------
    // Events and end of turn
    // ------------------------------------------------------------------

    public void events(List<GameEvent> events) {
        for (GameEvent event : events) {
            List<String> lines = wrap(event.message(), WIDTH);
            term.println("    " + icon(event.kind()) + " " + colour(event.kind(), lines.get(0)));
            for (String more : lines.subList(1, lines.size())) {
                term.println("      " + colour(event.kind(), more));
            }
        }
        if (!events.isEmpty()) {
            term.println();
        }
    }

    public void endOfTurn(Game game, List<GameEvent> events) {
        term.println("  " + t.faint(t.line(4)) + " " + t.bold("End of turn " + game.turn()) + " "
                + t.faint(t.line(WIDTH - 14 - String.valueOf(game.turn()).length())));
        if (events.isEmpty()) {
            term.println("    " + t.faint("A quiet turn."));
        }
        events(events);
        summary(game.scoreboard());
    }

    /** Served count, unhappy departures, revenue, average satisfaction and turns used. */
    private void summary(Scoreboard s) {
        term.println("  " + String.join(t.faint(" " + t.bullet() + " "),
                summaryStat("Served", String.valueOf(s.servedCount())),
                summaryStat("Unhappy departures", String.valueOf(s.departures())),
                summaryStat("Revenue", s.revenue().toString()),
                summaryStat("Avg sat.", s.averageText()),
                summaryStat("Turns", s.turnsUsed() + "/" + s.maxTurns())));
    }

    // ------------------------------------------------------------------
    // Final result
    // ------------------------------------------------------------------

    public void finalResult(Game game, GameResult result) {
        term.println();
        term.println();
        if (result.isVictory()) {
            if (t.unicode()) {
                term.println("     " + t.green("╦  ╦┬┌─┐┌┬┐┌─┐┬─┐┬ ┬"));
                term.println("     " + t.green("╚╗╔╝││   │ │ │├┬┘└┬┘"));
                term.println("     " + t.green(" ╚╝ ┴└─┘ ┴ └─┘┴└─ ┴ "));
            } else {
                term.println("     " + t.green("V I C T O R Y"));
            }
            term.println("     " + t.gray("The dining room hums. Every target was met."));
        } else {
            if (t.unicode()) {
                term.println("     " + t.red("╔╦╗┌─┐┌─┐┌─┐┌─┐┌┬┐"));
                term.println("     " + t.red(" ║║├┤ ├┤ ├┤ ├─┤ │ "));
                term.println("     " + t.red("═╩╝└─┘└  └─┘┴ ┴ ┴ "));
            } else {
                term.println("     " + t.red("D E F E A T"));
            }
            term.println("     " + t.gray("The restaurant fell short of its targets this time."));
        }
        term.println();

        section("TARGETS");
        for (GameResult.Target target : result.targets()) {
            String mark = target.met() ? t.green(t.tick()) : t.red(t.cross());
            term.println("     " + mark + "  " + Theme.padRight(target.name(), 24)
                    + Theme.padLeft(t.bold(target.actual()), 10) + "   " + t.gray(target.required()));
        }
        term.println();

        section("GUESTS");
        term.println("     " + t.gray(Theme.padRight("Guest", 12) + Theme.padRight("Type", 10)
                + Theme.padRight("Ordered", 20) + Theme.padLeft("Bill", 7) + Theme.padLeft("Sat.", 6)
                + "   Outcome"));
        for (Customer c : game.restaurant().guests()) {
            String ordered = c.order().map(o -> o.item().name()).orElse("-");
            String bill = c.order().map(o -> o.price().toString()).orElse("-");
            term.println("     " + Theme.padRight(customerName(c), 12) + Theme.padRight(typeTag(c), 10)
                    + Theme.padRight(ordered, 20) + Theme.padLeft(bill, 7)
                    + Theme.padLeft(t.bySatisfaction(c.satisfaction(), String.valueOf(c.satisfaction())), 6)
                    + "   " + status(c));
        }
        term.println();
        summary(result.score());
        term.println();
    }

    // ------------------------------------------------------------------
    // Pieces
    // ------------------------------------------------------------------

    /** Name, type, satisfaction bar and number, in fixed-width columns. */
    private String customerCells(Customer c) {
        return Theme.padRight(customerName(c), 10)
                + Theme.padRight(typeTag(c), 9)
                + t.satisfactionBar(c.satisfaction()) + " "
                + Theme.padLeft(t.bySatisfaction(c.satisfaction(), String.valueOf(c.satisfaction())), 3);
    }

    private String customerName(Customer c) {
        return t.gray("#" + c.id()) + " " + t.bold(c.name());
    }

    /** Customer types get their own colour; unknown types fall back to grey. */
    private String typeTag(Customer c) {
        return switch (c.type()) {
            case "VIP" -> t.gold(c.type());
            case "Critic" -> t.purple(c.type());
            default -> t.gray(c.type());
        };
    }

    private String status(Customer c) {
        String text = c.statusDescription();
        CustomerStatus s = c.status();
        return switch (s) {
            case WAITING -> t.gold(text);
            case SEATED, ORDERED -> t.blue(text);
            case EATING -> t.green(text);
            case READY_TO_PAY -> t.gold(text);
            case PAID -> t.gray(text);
            case LEFT -> t.red(text);
        };
    }

    private String orderStatus(OrderStatus status) {
        return switch (status) {
            case IN_PREPARATION -> t.gold(status.label());
            case READY -> t.green(t.bold(status.label()));
            case SERVED -> t.blue(status.label());
            case PAID -> t.gray(status.label());
            case CANCELLED -> t.red(status.label());
        };
    }

    private String icon(GameEvent.Kind kind) {
        return switch (kind) {
            case ARRIVAL -> t.cyan(t.pointer());
            case EATING -> t.green(t.tick());
            case PAYMENT -> t.gold("$");
            case PATIENCE -> t.faint(t.down());
            case DEPARTURE -> t.red(t.cross());
        };
    }

    private String colour(GameEvent.Kind kind, String text) {
        return switch (kind) {
            case ARRIVAL -> t.cyan(text);
            case EATING -> text;
            case PAYMENT -> t.gold(text);
            case PATIENCE -> t.gray(text);
            case DEPARTURE -> t.red(text);
        };
    }

    private String stat(String label, String value, String target, boolean met) {
        String shown = met ? t.green(value) : t.bold(value);
        return t.gray(label + " ") + shown + t.faint("/" + target);
    }

    private String summaryStat(String label, String value) {
        return t.gray(label + " ") + t.bold(value);
    }

    private void section(String name) {
        term.println("  " + t.accent(name) + " " + t.faint(t.line(WIDTH + 3 - name.length())));
    }

    private void paragraph(String... lines) {
        for (String line : lines) {
            term.println("     " + line);
        }
    }

    private void guestType(String type, String patience, String bill) {
        term.println("     " + Theme.padRight(type, 10) + Theme.padRight(patience, 28) + t.gray(bill));
    }

    private void commandHelp(String usage, String meaning) {
        term.println("     " + Theme.padRight(t.code(usage), 27) + t.gray(meaning));
    }

    private void boxLine(String content) {
        term.println("  " + t.faint(t.v()) + " " + Theme.padRight(content, WIDTH) + " " + t.faint(t.v()));
    }

    private static String spread(String left, String right) {
        int gap = WIDTH - Theme.visibleLength(left) - Theme.visibleLength(right);
        return left + " ".repeat(Math.max(1, gap)) + right;
    }

    /** Splits text into lines of at most {@code width} characters, breaking at spaces. */
    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        lines.add(line.toString());
        return lines;
    }

    private static String units(int n) {
        return n == 1 ? "1 unit" : n + " units";
    }
}
