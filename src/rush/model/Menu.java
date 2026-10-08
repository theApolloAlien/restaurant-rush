package rush.model;

import java.util.List;

/**
 * The restaurant's menu board, in a fixed display order.
 */
public final class Menu {

    private final List<MenuItem> items;

    public Menu(List<MenuItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("A menu needs at least one item");
        }
        this.items = List.copyOf(items);
    }

    public List<MenuItem> items() {
        return items;
    }

    /**
     * Reads the board as if it wraps around: position 0 is the first item and
     * position {@code size()} is the first item again.
     */
    public MenuItem atPosition(int position) {
        return items.get(Math.floorMod(position, items.size()));
    }

    public int size() {
        return items.size();
    }
}
