package rush.model;

import java.util.Optional;

/**
 * A dining table. It stays occupied until its customer pays or leaves.
 */
public class Table {

    private final int number;
    private Customer occupant;

    public Table(int number) {
        this.number = number;
    }

    public int number() {
        return number;
    }

    public boolean isFree() {
        return occupant == null;
    }

    public Optional<Customer> occupant() {
        return Optional.ofNullable(occupant);
    }

    void assign(Customer customer) {
        if (!isFree()) {
            throw new RuleViolation("Table " + number + " is occupied by " + occupant.label() + ".");
        }
        occupant = customer;
    }

    void release() {
        occupant = null;
    }

    @Override
    public String toString() {
        return "Table " + number;
    }
}
