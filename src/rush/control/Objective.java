package rush.control;

import java.util.List;

import rush.model.Money;

/**
 * The win condition checked after the last turn. Every target must be met.
 */
public record Objective(int minPaidCustomers, Money minRevenue, int minAverageSatisfaction) {

    /** Base setting: at least 4 paid customers, $45 revenue and average satisfaction 60. */
    public static Objective base() {
        return new Objective(4, Money.dollars(45), 60);
    }

    public GameResult evaluate(Scoreboard score) {
        List<GameResult.Target> targets = List.of(
                new GameResult.Target("Paid customers",
                        String.valueOf(score.paidCount()),
                        "at least " + minPaidCustomers,
                        score.paidCount() >= minPaidCustomers),
                new GameResult.Target("Revenue",
                        score.revenue().toString(),
                        "at least " + minRevenue,
                        score.revenue().isAtLeast(minRevenue)),
                new GameResult.Target("Average satisfaction",
                        score.averageText(),
                        "at least " + minAverageSatisfaction,
                        score.averageSatisfaction().isPresent()
                                && score.averageSatisfaction().getAsDouble() >= minAverageSatisfaction));
        return new GameResult(targets, score);
    }
}
