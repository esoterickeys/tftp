package server;

import java.util.concurrent.atomic.AtomicBoolean;

public class State {
    private AtomicBoolean running;

    public State() {
        running = new AtomicBoolean(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public void setRunning(boolean running) {
        this.running.lazySet(running);
    }
}
