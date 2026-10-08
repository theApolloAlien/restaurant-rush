package rush.model;

/**
 * Thrown by domain objects when a requested action would break a game rule.
 * <p>
 * The message is written for the player ("Table 1 is occupied by #1 Ada"), so the
 * control layer can show it unchanged and let the manager choose again.
 */
public class RuleViolation extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RuleViolation(String message) {
        super(message);
    }
}
