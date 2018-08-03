package server;

import data.SimplePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Server {
    private State state;

    private Map<Integer, BlockingQueue<SimplePacket>> pipes;

    private ConnectionManager connectionManager;
    private DataManager dataManager;

    private int serverPort = 60;

    public Server() {

    }

    public static void main(String[] args) {
        Server server = new Server();
        server.initialize();

        try {
            server.run();
        } catch (Exception e) {
            server.getState().setRunning(false);

            server.connectionManager.terminate();
            server.dataManager.terminate();

            System.exit(0);
        }
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

    private void initialize() {
        pipes = new HashMap<>();

        BlockingQueue<SimplePacket> rcvPipe = new ArrayBlockingQueue<>(100);
        pipes.put(serverPort, rcvPipe);

        state = new State();
        state.setRunning(false);

        connectionManager = new ConnectionManager(this);
        dataManager = new DataManager(this);
    }

    private void run() {
        state.setRunning(true);

        connectionManager.run();
        dataManager.run();

    }

    public State getState() {
        return state;
    }

    public Map<Integer, BlockingQueue<SimplePacket>> getPipes() {
        return pipes;
    }

    public int getServerPort() {
        return serverPort;
    }
}