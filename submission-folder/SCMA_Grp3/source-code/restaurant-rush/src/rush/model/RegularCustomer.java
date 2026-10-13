package rush.model;

/**
 * An ordinary guest: loses 8 satisfaction per waiting turn and pays full price.
 */
public class RegularCustomer extends Customer {

    public static final int SATISFACTION_LOSS = 8;

    public RegularCustomer(int id, String name, int arrivalTurn) {
        super(id, name, arrivalTurn);
    }

    @Override
    public String type() {
        return "Regular";
    }

    @Override
    public int satisfactionLossPerTurn() {
        return SATISFACTION_LOSS;
    }
}
