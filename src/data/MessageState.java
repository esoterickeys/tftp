package data;

public enum MessageState {
    INITIAL(0),
    RRQ_START(1),
    RRQ_NEXT(2),
    WRQ_START(4),
    WRQ_NEXT(5),
    END(6);

    private final int state;

    MessageState(int state) {
        this.state = state;
    }

    public int getState() {
        return this.state;
    }
}
