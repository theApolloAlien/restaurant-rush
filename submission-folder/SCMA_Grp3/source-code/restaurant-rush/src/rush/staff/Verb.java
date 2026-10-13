package rush.staff;

/** The kinds of instruction the manager can give a member of staff. */
public enum Verb {
    SEAT("seat", "seat a queued customer at a free table"),
    ORDER("order", "take a seated customer's order"),
    SERVE("serve", "serve a READY dish"),
    COOK("cook", "spend one preparation unit on an order"),
    WAIT("wait", "do nothing this turn");

    private final String keyword;
    private final String description;

    Verb(String keyword, String description) {
        this.keyword = keyword;
        this.description = description;
    }

    public String keyword() {
        return keyword;
    }

    public String description() {
        return description;
    }
}
