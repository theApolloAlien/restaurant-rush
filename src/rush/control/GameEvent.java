package rush.control;

/**
 * Something that happened during a turn, for the interface to display.
 */
public record GameEvent(Kind kind, String message) {

    /** Lets the interface choose an icon and colour without parsing the text. */
    public enum Kind {
        ARRIVAL,
        EATING,
        PAYMENT,
        PATIENCE,
        DEPARTURE
    }
}
