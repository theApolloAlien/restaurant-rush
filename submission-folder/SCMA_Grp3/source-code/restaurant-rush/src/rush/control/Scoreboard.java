package rush.control;

import java.util.OptionalDouble;

import rush.model.Money;

/**
 * A snapshot of the numbers shown at the end of each turn and used for the
 * final result.
 */
public record Scoreboard(int servedCount,
                         int paidCount,
                         int departures,
                         Money revenue,
                         OptionalDouble averageSatisfaction,
                         int turnsUsed,
                         int maxTurns) {

    /** Average satisfaction of paid customers to one decimal place, or "N/A" if nobody has paid. */
    public String averageText() {
        return averageSatisfaction.isPresent()
                ? String.format("%.1f", averageSatisfaction.getAsDouble())
                : "N/A";
    }
}
