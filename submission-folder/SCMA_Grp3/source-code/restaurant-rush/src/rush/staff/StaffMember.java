package rush.staff;

/**
 * Anyone who works in the restaurant. Every staff member has a fixed number of
 * tasks per turn; the budget is reset at the start of each turn.
 */
public abstract class StaffMember {

    private final String name;
    private final int tasksPerTurn;
    private int tasksUsed;

    protected StaffMember(String name, int tasksPerTurn) {
        this.name = name;
        this.tasksPerTurn = tasksPerTurn;
    }

    /** The job title, e.g. "Waiter". */
    public abstract String role();

    public String name() {
        return name;
    }

    /** e.g. "Waiter Wendy". */
    public String title() {
        return role() + " " + name;
    }

    public void startTurn() {
        tasksUsed = 0;
    }

    public boolean hasTaskLeft() {
        return tasksUsed < tasksPerTurn;
    }

    protected void useTask() {
        tasksUsed++;
    }
}
