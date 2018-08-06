package server;

import data.SimplePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DataManager implements Runnable {

    private final ResourceManager resourceManager;

    private Map<Integer, DataProcessor> dataProcessors;
    private Map<Integer, Thread> dataProcessorThreads;

    public DataManager(final ResourceManager resourceManager) {
        this.resourceManager = resourceManager;

        initialize();
    }

    @Override
    public void run() {
        while (resourceManager.getState().isRunning()) {
            SimplePacket data = null;

            try {
                data = resourceManager.getPipes().get(resourceManager.getServerPort()).poll(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (data != null) {
                if (dataProcessors.get(data.header.getPort()) == null) {
                    resourceManager.createDataPipe(data.header.getPort());

                    DataProcessor dataProcessor = new DataProcessor(data.header.getPort(), resourceManager);
                    Thread dataProcessorThread = new Thread(dataProcessor);

                    dataProcessors.put(data.header.getPort(), dataProcessor);
                    dataProcessorThreads.put(data.header.getPort(), dataProcessorThread);

                    dataProcessorThread.start();
                    dataProcessor.addToQueue(data);
                } else {
                    dataProcessors.get(data.header.getPort()).addToQueue(data);
                }
            }
        }

        terminate();
    }

    private void initialize() {
        dataProcessors = new HashMap<>();
        dataProcessorThreads = new HashMap<>();
    }

    public void teardownProcessor(int port) {
        try {
            dataProcessorThreads.get(port).join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        dataProcessorThreads.remove(port);
        dataProcessors.remove(port);

        System.out.println("Data processor and thread for port(" + port + ") removed.");
    }

    public void terminate() {
        Set<Integer> ports = dataProcessors.keySet();

        for (Integer port : ports) {
            Thread dataProcessorThread = dataProcessorThreads.get(port);

            try {
                dataProcessorThread.join();
            } catch (InterruptedException ie) {
                System.out.println(ie.getMessage());
            }
        }

        System.out.println("Data manager successfully terminate.");
    }
}
