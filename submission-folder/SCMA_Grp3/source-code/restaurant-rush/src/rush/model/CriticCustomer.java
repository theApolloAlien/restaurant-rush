package rush.model;

import java.util.Comparator;

/**
 * A food critic: impatient (loses 12 satisfaction per waiting turn) and pays
 * full price.
 * <p>
 * <b>Special behaviour (order preference):</b> a critic judges the kitchen by
 * its most elaborate dish. Instead of reading the menu in rotation, the critic
 * always orders the item that needs the most preparation units, choosing the
 * dearer one on a tie. On the base menu that is the Pasta (2 units), so the
 * manager must plan two Chef turns for a guest who loses patience fastest.
 * When paying, the critic publishes a star rating based on their satisfaction.
 */
public class CriticCustomer extends Customer {

    public static final int SATISFACTION_LOSS = 12;

    private static final Comparator<MenuItem> MOST_ELABORATE =
            Comparator.comparingInt(MenuItem::prepUnits).thenComparing(MenuItem::price);

    public CriticCustomer(int id, String name, int arrivalTurn) {
        super(id, name, arrivalTurn);
    }

    @Override
    public String type() {
        return "Critic";
    }

    @Override
    public int satisfactionLossPerTurn() {
        return SATISFACTION_LOSS;
    }

    @Override
    public MenuItem chooseFrom(Menu menu) {
        return menu.items().stream().max(MOST_ELABORATE).orElseThrow();
    }

    /** One star per full 20 points of satisfaction (1 to 5), with a one-line verdict. */
    @Override
    public String remarkOnPaying() {
        int stars = Math.max(1, Math.min(5, satisfaction() / 20));
        String verdict = switch (stars) {
            case 5 -> "Flawless service.";
            case 4 -> "Confident cooking, attentive staff.";
            case 3 -> "Good food, let down by the wait.";
            case 2 -> "Patience is not on the menu here.";
            default -> "Avoid.";
        };
        return stars + "/5 stars: " + verdict;
    }
}
