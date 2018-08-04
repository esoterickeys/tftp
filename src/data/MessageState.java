package data;

public enum MessageState {
    INITIAL(0),
    RRQ_NEXT(1),
    WRQ_NEXT(2),
    END(3),
    TERMINATE(4);

    private final int state;

    MessageState(int state) {
        this.state = state;
    }

    public int getState() {
        return this.state;
    }
}
