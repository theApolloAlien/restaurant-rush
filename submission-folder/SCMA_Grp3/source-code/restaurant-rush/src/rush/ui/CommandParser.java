package rush.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import rush.staff.Command;
import rush.staff.Verb;

/**
 * Turns what the player typed into a {@link Command} or an interface action.
 * It only checks the shape of the input ("seat 2 1"); whether the move is
 * allowed is decided by the game, not here.
 */
public final class CommandParser {

    /** What a line of input asks for. */
    public enum Kind { COMMAND, HELP, BOARD, MENU, QUIT, INVALID }

    /** A parsed line: a command for staff, an interface action, or an error message. */
    public record Input(Kind kind, Command command, String error) {
        static Input of(Kind kind) {
            return new Input(kind, null, null);
        }

        static Input invalid(String error) {
            return new Input(Kind.INVALID, null, error);
        }
    }

    private static final Map<String, Verb> VERBS = Map.ofEntries(
            Map.entry("seat", Verb.SEAT), Map.entry("s", Verb.SEAT), Map.entry("sit", Verb.SEAT),
            Map.entry("order", Verb.ORDER), Map.entry("o", Verb.ORDER), Map.entry("take", Verb.ORDER),
            Map.entry("serve", Verb.SERVE), Map.entry("v", Verb.SERVE), Map.entry("deliver", Verb.SERVE),
            Map.entry("cook", Verb.COOK), Map.entry("c", Verb.COOK), Map.entry("prep", Verb.COOK),
            Map.entry("wait", Verb.WAIT), Map.entry("w", Verb.WAIT), Map.entry("skip", Verb.WAIT),
            Map.entry("pass", Verb.WAIT));

    private static final Map<String, Kind> ACTIONS = Map.ofEntries(
            Map.entry("help", Kind.HELP), Map.entry("h", Kind.HELP), Map.entry("?", Kind.HELP),
            Map.entry("board", Kind.BOARD), Map.entry("b", Kind.BOARD), Map.entry("status", Kind.BOARD),
            Map.entry("menu", Kind.MENU), Map.entry("m", Kind.MENU),
            Map.entry("quit", Kind.QUIT), Map.entry("q", Kind.QUIT), Map.entry("exit", Kind.QUIT));

    /** Words that may appear between numbers and are ignored, as in "seat #2 at table 1". */
    private static final Set<String> FILLER = Set.of("at", "to", "table", "customer", "guest", "the", "for", "on");

    public Input parse(String line) {
        String[] words = line.trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) {
            return Input.invalid("Type a command, for example 'wait', or 'help' for the full list.");
        }
        if (ACTIONS.containsKey(words[0]) && words.length == 1) {
            return Input.of(ACTIONS.get(words[0]));
        }
        Verb verb = VERBS.get(words[0]);
        if (verb == null) {
            return Input.invalid("I don't know the command '" + words[0] + "'. Type 'help' for the list.");
        }

        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i < words.length; i++) {
            String word = words[i];
            if (FILLER.contains(word)) {
                continue;
            }
            String digits = word.replaceFirst("^(#|table|customer|t|c)", "");
            if (!digits.matches("\\d{1,4}")) {
                return Input.invalid("'" + words[i] + "' is not a customer or table number.");
            }
            numbers.add(Integer.parseInt(digits));
        }

        int allowed = switch (verb) {
            case SEAT -> 2;
            case WAIT -> 0;
            default -> 1;
        };
        if (numbers.size() > allowed) {
            return Input.invalid(usage(verb));
        }
        Integer customer = numbers.isEmpty() ? null : numbers.get(0);
        Integer table = numbers.size() > 1 ? numbers.get(1) : null;
        return new Input(Kind.COMMAND, new Command(verb, customer, table), null);
    }

    /** How a command is typed, e.g. "seat <customer> [table]". */
    public static String usage(Verb verb) {
        return switch (verb) {
            case SEAT -> "Usage: seat <customer> [table], e.g. 'seat 2 1'";
            case WAIT -> "Usage: wait";
            default -> "Usage: " + verb.keyword() + " <customer>, e.g. '" + verb.keyword() + " 2'";
        };
    }
}
