package server;

import java.util.HashMap;
import java.util.Map;

public class ConnectionManager implements Runnable {

    Map<Integer, Thread> channelThreads;
    private final ResourceManager resourceManager;

    public ConnectionManager(final ResourceManager resourceManager) {
        this.resourceManager = resourceManager;

        initialize();
    }

    @Override
    public void run() {
        channelThreads.get(resourceManager.getServerPort()).start();
    }

    public void terminate() {
        channelThreads.get(resourceManager.getServerPort()).interrupt();

        try {
            for (Integer port : channelThreads.keySet()) {
                channelThreads.get(port).join();
            }
        } catch (InterruptedException ie) {
            System.out.println(ie.getMessage());
        }

        System.out.println("Connection manager successfully terminate.");
    }

    public void addOutputChannel(int port) {
        if (channelThreads.get(port) != null) {
            System.out.println("Channel exists for port " + port);
            return;
        }

        OutputChannel outputChannel = new OutputChannel(port, resourceManager);
        Thread serverOutputThread = new Thread(outputChannel);

        channelThreads.put(port, serverOutputThread);

        serverOutputThread.start();
    }

    public void teardownOutputChannel(int port) {
        try {
            channelThreads.get(port).join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        channelThreads.remove(port);

        System.out.println("Output channel for port(" + port + ") removed.");
    }

    private void initialize() {
        InputChannel inputChannel = new InputChannel(resourceManager);
        Thread serverInputThread = new Thread(inputChannel);

        channelThreads = new HashMap<>();
        channelThreads.put(resourceManager.getServerPort(), serverInputThread);
    }
}
