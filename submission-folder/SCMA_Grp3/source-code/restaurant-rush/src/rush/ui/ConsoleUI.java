package rush.ui;

import java.util.List;
import java.util.Optional;

import rush.control.Game;
import rush.control.GameConfig;
import rush.control.GameEvent;
import rush.staff.ActionResult;
import rush.staff.Command;
import rush.staff.DirectedStaff;

/**
 * The command-line boundary. It collects the manager's decisions and shows the
 * state of the game; it contains no game rules.
 */
public final class ConsoleUI {

    /** Thrown when the player quits or the input runs out, to unwind out of a game. */
    private static final class QuitGame extends RuntimeException {
        private static final long serialVersionUID = 1L;

        QuitGame() {
            super(null, null, false, false);
        }
    }

    private final Terminal term;
    private final Dashboard dashboard;
    private final CommandParser parser = new CommandParser();
    private final String edition;

    public ConsoleUI(Terminal term, String edition) {
        this.term = term;
        this.dashboard = new Dashboard(term);
        this.edition = edition;
    }

    public void run() {
        dashboard.title(edition);
        while (true) {
            dashboard.mainMenu();
            Optional<String> choice = term.prompt("  " + term.theme().accent(term.theme().arrow()) + " ");
            if (choice.isEmpty()) {
                return;
            }
            switch (choice.get().toLowerCase()) {
                case "1", "p", "play" -> play();
                case "2", "h", "help" -> dashboard.help(new Game(GameConfig.base()));
                case "3", "q", "quit", "exit" -> {
                    term.println("  " + term.theme().gray("Thanks for playing. The kitchen is closed."));
                    return;
                }
                default -> dashboard.error("Choose 1, 2 or 3.");
            }
        }
    }

    /** Plays one full game of the base setting. */
    public void play() {
        Game game = new Game(GameConfig.base());
        try {
            while (!game.isOver()) {
                List<GameEvent> arrivals = game.beginTurn();
                dashboard.turnHeader(game, edition);
                dashboard.events(arrivals);
                dashboard.board(game);
                if (game.canStartHappyHour()) {
                    offerHappyHour(game);
                }
                for (DirectedStaff staff : game.directedStaff()) {
                    instruct(game, staff);
                }
                dashboard.endOfTurn(game, game.endTurn());
            }
            dashboard.finalResult(game, game.result());
        } catch (QuitGame quit) {
            term.println("  " + term.theme().gray("You hang up your apron. Game abandoned."));
            term.println();
        }
    }

    /** Happy Hour is a start-of-turn decision, so it is offered before any staff member acts. */
    private void offerHappyHour(Game game) {
        dashboard.happyHourOffer();
        while (true) {
            String answer = term.prompt(dashboard.happyHourPrompt()).orElseThrow(QuitGame::new).toLowerCase();
            switch (answer) {
                case "y", "yes" -> {
                    dashboard.actionResult(game.startHappyHour());
                    return;
                }
                case "", "n", "no" -> {
                    term.println();
                    return;
                }
                case "q", "quit", "exit" -> throw new QuitGame();
                default -> dashboard.error("Answer y to start Happy Hour now, or n (or just Enter) to keep it.");
            }
        }
    }

    /**
     * Asks for instructions until this staff member has used their task for the
     * turn. Someone who can only wait does so without asking.
     */
    private void instruct(Game game, DirectedStaff staff) {
        if (game.options(staff).size() == 1) {
            game.direct(staff, Command.waitTurn());
            dashboard.autoWait(staff);
            return;
        }
        dashboard.staffPrompt(game, staff);
        while (staff.hasTaskLeft()) {
            String line = term.prompt(dashboard.promptText(staff)).orElseThrow(QuitGame::new);
            CommandParser.Input input = parser.parse(line);
            switch (input.kind()) {
                case COMMAND -> {
                    ActionResult result = game.direct(staff, input.command());
                    dashboard.actionResult(result);
                }
                case HELP -> dashboard.help(game);
                case BOARD -> {
                    dashboard.turnHeader(game, edition);
                    dashboard.board(game);
                    dashboard.staffPrompt(game, staff);
                }
                case MENU -> dashboard.menu(game.restaurant().menu());
                case QUIT -> throw new QuitGame();
                case INVALID -> dashboard.error(input.error());
            }
        }
    }
}
