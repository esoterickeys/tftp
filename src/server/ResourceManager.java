package server;

import data.SimplePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ResourceManager {

    private final int serverPort;

    private ConnectionManager connectionManager;
    private DataManager dataManager;
    private final State state;
    private Map<Integer, BlockingQueue<SimplePacket>> pipes;

    private Map<Integer, State> portResourceStates;
    private Map<Integer, Timer> resourceTimers;

    public ResourceManager(int serverPort, final State state) {
        this.serverPort = serverPort;
        this.state = state;

        initialize();
    }

    private void initialize() {
        pipes = new HashMap<>();
        portResourceStates = new HashMap<>();
        resourceTimers = new HashMap<>();

        BlockingQueue<SimplePacket> rcvPipe = new ArrayBlockingQueue<>(100);
        pipes.put(serverPort, rcvPipe);
    }

    public void terminate() {
        for (Integer port : portResourceStates.keySet()) {
            portResourceStates.get(port).setRunning(false);
        }

        System.out.println("Resource manager successfully terminated.");
    }

    public void createDataPipe(int port) {
        if (pipes.get(port) == null) {
            portResourceStates.put(port, new State());
            portResourceStates.get(port).setRunning(true);

            Timer t = new Timer();
            t.scheduleAtFixedRate(new ResourceTimer(port, this), 0, 1000);

            resourceTimers.put(port, t);

            BlockingQueue<SimplePacket> sendPipe = new ArrayBlockingQueue<>(100);
            pipes.put(port, sendPipe);

            connectionManager.addOutputChannel(port);
        } else {
            System.out.println("Attempted to generate an existing data pipe for port(" + port + ")");
        }
    }

    public void addDataToPipe(int port, SimplePacket data) {
        try {
            pipes.get(port).put(data);

            resourceTimers.get(port).cancel();

            Timer t = new Timer();
            t.scheduleAtFixedRate(new ResourceTimer(port, this), 0, 1000);

            resourceTimers.put(port, t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void loadConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void loadDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public Map<Integer, BlockingQueue<SimplePacket>> getPipes() {
        return pipes;
    }

    public int getServerPort() {
        return serverPort;
    }

    public State getState() {
        return state;
    }

    public boolean getPortResourceState(int port) {
        return portResourceStates.get(port).isRunning();
    }

    public void teardownPortResource(int port) {
        portResourceStates.get(port).setRunning(false);

        pipes.remove(port);
        resourceTimers.remove(port);

        dataManager.teardownProcessor(port);
        connectionManager.teardownOutputChannel(port);
    }

    class ResourceTimer extends TimerTask {
        int timeoutCounter = 10;
        int port;
        private ResourceManager rm;

        public ResourceTimer(int port, ResourceManager rm) {
            this.port = port;
            this.rm = rm;
        }

        @Override
        public void run() {
            if (timeoutCounter > 0) {
                timeoutCounter--;
            } else {
                System.out.println("Resource timeout occurred on port(" + port + ").");

                rm.teardownPortResource(port);
                this.cancel();
            }
        }

        public void reset() {
            timeoutCounter = 30;
        }

    }
}
