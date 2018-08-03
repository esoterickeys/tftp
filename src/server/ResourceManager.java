package server;

import data.SimplePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ResourceManager {

    private final int serverPort;

    private ConnectionManager connectionManager;
    private final State state;
    private Map<Integer, BlockingQueue<SimplePacket>> pipes;

    public ResourceManager(int serverPort, final State state) {
        this.serverPort = serverPort;
        this.state = state;

        initialize();
    }

    private void initialize() {
        pipes = new HashMap<>();

        BlockingQueue<SimplePacket> rcvPipe = new ArrayBlockingQueue<>(100);
        pipes.put(serverPort, rcvPipe);
    }

    public void createOutputDataPipe(int port) {
        if (pipes.get(port) == null) {
            BlockingQueue<SimplePacket> sendPipe = new ArrayBlockingQueue<>(100);
            pipes.put(port, sendPipe);

            connectionManager.addOutputChannel(port);
        }
    }

    public void addDataToPipe(int port, SimplePacket data) {
        try {
            pipes.get(port).put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void loadConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
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
}
