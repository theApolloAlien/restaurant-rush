package rush;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import rush.ui.ConsoleUI;
import rush.ui.Terminal;
import rush.ui.Theme;

/**
 * Entry point.
 * <pre>
 *   java -cp out rush.Main                         play interactively
 *   java -cp out rush.Main --script FILE           replay commands from a file
 *   java -cp out rush.Main --plain                 no colour, ASCII only
 * </pre>
 */
public final class Main {

    /** Which stage of the assignment this build is. */
    public static final String EDITION = "Stage 3 · Host, combos, Happy Hour";

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        boolean color = System.getenv("NO_COLOR") == null;
        boolean unicode = true;
        Path script = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--plain" -> {
                    color = false;
                    unicode = false;
                }
                case "--no-color" -> color = false;
                case "--ascii" -> unicode = false;
                case "--script" -> {
                    if (i + 1 >= args.length) {
                        usage("--script needs a file name");
                        return;
                    }
                    script = Path.of(args[++i]);
                }
                case "--help", "-h" -> {
                    usage(null);
                    return;
                }
                default -> {
                    usage("Unknown option: " + args[i]);
                    return;
                }
            }
        }

        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        BufferedReader in = script != null
                ? Files.newBufferedReader(script, StandardCharsets.UTF_8)
                : new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        Theme theme = new Theme(color, unicode);
        String edition = unicode ? EDITION : EDITION.replace('·', '-');
        new ConsoleUI(new Terminal(in, out, theme, script != null), edition).run();
    }

    private static void usage(String problem) {
        if (problem != null) {
            System.err.println(problem);
        }
        System.err.println("Usage: java -cp out rush.Main [--script FILE] [--plain | --no-color | --ascii]");
    }
}
