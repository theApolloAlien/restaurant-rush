package rush.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Line-based input and output. When input comes from a script file the typed
 * line is echoed, so a saved transcript reads like a real session.
 */
public final class Terminal {

    private final BufferedReader in;
    private final PrintStream out;
    private final Theme theme;
    private final boolean echoInput;

    public Terminal(BufferedReader in, PrintStream out, Theme theme, boolean echoInput) {
        this.in = in;
        this.out = out;
        this.theme = theme;
        this.echoInput = echoInput;
    }

    public Theme theme() {
        return theme;
    }

    public void println(String line) {
        out.println(line);
    }

    public void println() {
        out.println();
    }

    /** Shows a prompt and reads one line; empty when the input has ended. */
    public Optional<String> prompt(String prompt) {
        out.print(prompt);
        out.flush();
        try {
            String line = in.readLine();
            if (line == null) {
                out.println();
                return Optional.empty();
            }
            if (echoInput) {
                out.println(theme.code(line));
            }
            return Optional.of(line.trim());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
