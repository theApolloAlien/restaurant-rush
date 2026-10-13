package rush.ui;

import java.util.regex.Pattern;

/**
 * Colours and glyphs for the console. A plain theme falls back to ASCII and no
 * colour, for old terminals or for saving transcripts to a file.
 */
public final class Theme {

    private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-9;]*m");
    private static final String RESET = "\u001B[0m";
    private static final String[] EIGHTHS = {"", "▏", "▎", "▍", "▌", "▋", "▊", "▉"};

    private final boolean color;
    private final boolean unicode;

    public Theme(boolean color, boolean unicode) {
        this.color = color;
        this.unicode = unicode;
    }

    public boolean unicode() {
        return unicode;
    }

    // ------------------------------------------------------------------
    // Colour
    // ------------------------------------------------------------------

    private String paint(String text, String code) {
        return color && !text.isEmpty() ? "\u001B[" + code + "m" + text + RESET : text;
    }

    public String bold(String s)    { return paint(s, "1"); }
    public String dim(String s)     { return paint(s, "2"); }
    public String italic(String s)  { return paint(s, "3"); }
    public String accent(String s)  { return paint(s, "1;38;5;209"); }
    public String tomato(String s)  { return paint(s, "38;5;203"); }
    public String gold(String s)    { return paint(s, "38;5;221"); }
    public String green(String s)   { return paint(s, "38;5;114"); }
    public String red(String s)     { return paint(s, "38;5;203"); }
    public String blue(String s)    { return paint(s, "38;5;111"); }
    public String purple(String s)  { return paint(s, "38;5;177"); }
    public String cyan(String s)    { return paint(s, "38;5;117"); }
    public String gray(String s)    { return paint(s, "38;5;245"); }
    public String faint(String s)   { return paint(s, "38;5;240"); }
    public String code(String s)    { return paint(s, "1;38;5;223"); }

    /** Green when good, yellow when worrying, red when close to walking out. */
    public String bySatisfaction(int satisfaction, String s) {
        if (satisfaction >= 70) {
            return green(s);
        }
        return satisfaction >= 40 ? gold(s) : red(s);
    }

    // ------------------------------------------------------------------
    // Glyphs
    // ------------------------------------------------------------------

    public String h()        { return unicode ? "─" : "-"; }
    public String v()        { return unicode ? "│" : "|"; }
    public String topLeft()  { return unicode ? "╭" : "+"; }
    public String topRight() { return unicode ? "╮" : "+"; }
    public String botLeft()  { return unicode ? "╰" : "+"; }
    public String botRight() { return unicode ? "╯" : "+"; }
    public String teeLeft()  { return unicode ? "├" : "+"; }
    public String teeRight() { return unicode ? "┤" : "+"; }
    public String tick()     { return unicode ? "✔" : "+"; }
    public String cross()    { return unicode ? "✘" : "x"; }
    public String arrow()    { return unicode ? "›" : ">"; }
    public String bullet()   { return unicode ? "·" : "-"; }
    public String diamond()  { return unicode ? "◆" : "*"; }
    public String pointer()  { return unicode ? "▸" : ">"; }
    public String down()     { return unicode ? "▾" : "v"; }
    public String star()     { return unicode ? "★" : "*"; }
    public String hollowStar() { return unicode ? "☆" : "."; }

    public String line(int width) {
        return h().repeat(Math.max(0, width));
    }

    /** A 10-cell bar for a value from 0 to 100, e.g. {@code ████████▍░}. */
    public String satisfactionBar(int value) {
        int cells = 10;
        String filled;
        String empty;
        if (unicode) {
            int eighths = Math.round(value * cells * 8 / 100f);
            int full = eighths / 8;
            String partial = EIGHTHS[eighths % 8];
            filled = "█".repeat(full) + partial;
            empty = "░".repeat(cells - full - (partial.isEmpty() ? 0 : 1));
        } else {
            int full = Math.round(value * cells / 100f);
            filled = "#".repeat(full);
            empty = ".".repeat(cells - full);
        }
        return bySatisfaction(value, filled) + faint(empty);
    }

    /** Filled and hollow pips, e.g. {@code ■■□} for 2 of 3 units. */
    public String pips(int done, int total) {
        String on = unicode ? "■" : "#";
        String off = unicode ? "□" : ".";
        return on.repeat(done) + faint(off.repeat(Math.max(0, total - done)));
    }

    /** The turn track, e.g. {@code ▰▰▰▰▱▱▱▱...}. */
    public String track(int done, int total) {
        String on = unicode ? "▰" : "=";
        String off = unicode ? "▱" : ".";
        return accent(on.repeat(done)) + faint(off.repeat(Math.max(0, total - done)));
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    /** Length of the text as it appears on screen, ignoring colour codes. */
    public static int visibleLength(String s) {
        String plain = ANSI.matcher(s).replaceAll("");
        return plain.codePointCount(0, plain.length());
    }

    public static String padRight(String s, int width) {
        int pad = width - visibleLength(s);
        return pad > 0 ? s + " ".repeat(pad) : s;
    }

    public static String padLeft(String s, int width) {
        int pad = width - visibleLength(s);
        return pad > 0 ? " ".repeat(pad) + s : s;
    }
}
