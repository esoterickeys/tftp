package server;

import java.util.HashMap;
import java.util.Map;

public class ConnectionManager {

    Map<Integer, Thread> channelThreads;
    private final Server server;

    public ConnectionManager(final Server server) {
        this.server = server;

        initialize();
    }

    public void run() {
        channelThreads.get(server.getServerPort()).start();
    }

    public void terminate() {
        try {
            for (Integer port : channelThreads.keySet()) {
                channelThreads.get(port).join();
            }
        } catch (InterruptedException ie) {
            System.out.println(ie.getMessage());
        }
    }

    public void addOutputChannel(int port) {
        if (channelThreads.get(port) != null) {
            System.out.println("Channel exists for port " + port);
            return;
        }

        OutputChannel outputChannel = new OutputChannel(port, server);
        Thread serverOutputThread = new Thread(outputChannel);

        channelThreads.put(port, serverOutputThread);

        serverOutputThread.start();
    }

    private void initialize() {
        InputChannel inputChannel = new InputChannel(server);
        Thread serverInputThread = new Thread(inputChannel);

        channelThreads = new HashMap<>();
        channelThreads.put(server.getServerPort(), serverInputThread);
    }
}
