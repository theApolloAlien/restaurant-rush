package rush.model;

/**
 * A valued guest: more patient than a Regular (loses 5 satisfaction per waiting
 * turn) and receives 10% off the bill.
 */
public class VIPCustomer extends Customer {

    public static final int SATISFACTION_LOSS = 5;
    public static final int DISCOUNT_PERCENT = 10;

    public VIPCustomer(int id, String name, int arrivalTurn) {
        super(id, name, arrivalTurn);
    }

    @Override
    public String type() {
        return "VIP";
    }

    @Override
    public int satisfactionLossPerTurn() {
        return SATISFACTION_LOSS;
    }

    /** 10% off the menu price, rounded to the nearest cent. */
    @Override
    public Money billFor(Money menuPrice) {
        return menuPrice.percent(100 - DISCOUNT_PERCENT);
    }

    @Override
    public String remarkOnPaying() {
        return satisfaction() >= 70 ? "Splendid as always. I'll be back." : "I expected better, frankly.";
    }
}
