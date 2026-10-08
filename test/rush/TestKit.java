package rush;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import rush.control.Game;
import rush.control.GameConfig;
import rush.staff.ActionResult;
import rush.staff.Command;
import rush.staff.DirectedStaff;
import rush.ui.ConsoleUI;
import rush.ui.Terminal;
import rush.ui.Theme;

/**
 * A very small test harness, so the tests run offline with nothing but a JDK.
 */
final class TestKit {

    /** A named test. */
    interface Check {
        void run() throws Exception;
    }

    private final List<String> failures = new ArrayList<>();
    private int passed;

    void test(String name, Check check) {
        try {
            check.run();
            passed++;
            System.out.println("  \u001B[32mPASS\u001B[0m  " + name);
        } catch (Throwable e) {
            failures.add(name + ": " + e.getMessage());
            System.out.println("  \u001B[31mFAIL\u001B[0m  " + name + "\n        " + e);
        }
    }

    int finish() {
        System.out.println();
        System.out.println("  " + passed + " passed, " + failures.size() + " failed");
        return failures.isEmpty() ? 0 : 1;
    }

    // ------------------------------------------------------------------
    // Assertions
    // ------------------------------------------------------------------

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void equal(Object expected, Object actual, String what) {
        if (!expected.equals(actual)) {
            throw new AssertionError(what + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    // ------------------------------------------------------------------
    // Game helpers
    // ------------------------------------------------------------------

    static DirectedStaff staff(Game game, String role) {
        return game.directedStaff().stream()
                .filter(s -> s.role().equals(role))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No " + role + " on staff"));
    }

    /** Gives an instruction that must succeed. */
    static void ok(Game game, String role, Command command) {
        ActionResult result = game.direct(staff(game, role), command);
        check(result.succeeded(), role + " '" + command + "' should succeed but: " + result.message());
    }

    /** Gives an instruction that must be rejected. */
    static String rejected(Game game, String role, Command command) {
        ActionResult result = game.direct(staff(game, role), command);
        check(!result.succeeded(), role + " '" + command + "' should be rejected");
        return result.message();
    }

    /** Plays a whole scripted session through the real console and returns the plain-text transcript. */
    static String playScript(String file) {
        try (BufferedReader in = Files.newBufferedReader(Path.of(file), StandardCharsets.UTF_8)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
            new ConsoleUI(new Terminal(in, out, new Theme(false, false), true), "test").run();
            return bytes.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Game baseGame() {
        return new Game(GameConfig.base());
    }
}
