package server;

public class Server {
    private State state;

    private ResourceManager resourceManager;
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
        } catch (InterruptedException e) {
            server.terminate();

            System.exit(0);
        }
    }

    private void initialize() {
        state = new State();
        state.setRunning(false);

        resourceManager = new ResourceManager(serverPort, state);

        connectionManager = new ConnectionManager(resourceManager);
        dataManager = new DataManager(resourceManager);

        resourceManager.loadConnectionManager(connectionManager);
    }

    private void run() throws InterruptedException {
        state.setRunning(true);

        Thread connectionThread = new Thread(connectionManager);
        Thread dataThread = new Thread(dataManager);

        connectionThread.start();
        dataThread.start();

        dataThread.join();
    }

    private void terminate() {
        state.setRunning(false);

        connectionManager.terminate();
        dataManager.terminate();
    }
}