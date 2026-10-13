package rush.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import rush.model.Customer;

/**
 * Which customer walks in at the start of which turn. Customers are numbered
 * in the order they are scheduled (#1, #2, ...).
 */
public final class ArrivalSchedule {

    /** Creates a customer object of some type. Constructor references such as {@code RegularCustomer::new} fit. */
    @FunctionalInterface
    public interface CustomerFactory {
        Customer create(int id, String name, int arrivalTurn);
    }

    /** One scheduled arrival. */
    public record Arrival(int turn, String name, CustomerFactory factory) {
    }

    private final List<Arrival> arrivals = new ArrayList<>();

    /** Adds an arrival; turns must be added in increasing order, at most one per turn. */
    public ArrivalSchedule at(int turn, String name, CustomerFactory factory) {
        if (!arrivals.isEmpty() && arrivals.get(arrivals.size() - 1).turn() >= turn) {
            throw new IllegalArgumentException("Arrivals must be scheduled in increasing turn order");
        }
        arrivals.add(new Arrival(turn, name, factory));
        return this;
    }

    /** The customer arriving at the start of this turn, if any, as a fresh object. */
    public Optional<Customer> arrivingOn(int turn) {
        for (int i = 0; i < arrivals.size(); i++) {
            Arrival arrival = arrivals.get(i);
            if (arrival.turn() == turn) {
                return Optional.of(arrival.factory().create(i + 1, arrival.name(), turn));
            }
        }
        return Optional.empty();
    }

    public List<Arrival> arrivals() {
        return Collections.unmodifiableList(arrivals);
    }
}
