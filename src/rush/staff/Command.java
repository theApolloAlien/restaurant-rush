package rush.staff;

/**
 * One instruction from the manager, e.g. "seat customer 2 at table 1".
 * The command only names its targets; the staff member checks whether it is
 * allowed when carrying it out.
 *
 * @param verb       what to do
 * @param customerId the customer it concerns, or null
 * @param tableNo    the table it concerns, or null to mean "any free table"
 */
public record Command(Verb verb, Integer customerId, Integer tableNo) {

    public static Command seat(int customerId, int tableNo) {
        return new Command(Verb.SEAT, customerId, tableNo);
    }

    public static Command takeOrder(int customerId) {
        return new Command(Verb.ORDER, customerId, null);
    }

    public static Command serve(int customerId) {
        return new Command(Verb.SERVE, customerId, null);
    }

    public static Command cook(int customerId) {
        return new Command(Verb.COOK, customerId, null);
    }

    public static Command waitTurn() {
        return new Command(Verb.WAIT, null, null);
    }

    /** The command as the player would type it, e.g. {@code seat 2 1}. */
    @Override
    public String toString() {
        StringBuilder text = new StringBuilder(verb.keyword());
        if (customerId != null) {
            text.append(' ').append(customerId);
        }
        if (tableNo != null) {
            text.append(' ').append(tableNo);
        }
        return text.toString();
    }
}
