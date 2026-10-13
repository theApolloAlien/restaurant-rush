package rush.staff;

/**
 * The outcome of a manager instruction. A rejected instruction does not use up
 * the staff member's task, so the manager can choose again.
 */
public record ActionResult(boolean succeeded, String message) {

    public static ActionResult done(String message) {
        return new ActionResult(true, message);
    }

    public static ActionResult rejected(String reason) {
        return new ActionResult(false, reason);
    }
}
