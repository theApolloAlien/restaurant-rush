package rush.control;

import java.util.List;

/**
 * The outcome after turn 18: Victory only if every target is met.
 */
public record GameResult(List<Target> targets, Scoreboard score) {

    /** One line of the objective, e.g. "Revenue $66.00, needs at least $45.00". */
    public record Target(String name, String actual, String required, boolean met) {
    }

    public boolean isVictory() {
        return targets.stream().allMatch(Target::met);
    }
}
