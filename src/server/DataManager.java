package server;

import data.SimplePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DataManager implements Runnable {

    private final ResourceManager resourceManager;

    Map<Integer, DataProcessor> dataProcessors;
    Map<Integer, Thread> dataProcessorThreads;

    public DataManager(final ResourceManager resourceManager) {
        this.resourceManager = resourceManager;

        initialize();
    }

    @Override
    public void run() {
        while (resourceManager.getState().isRunning()) {
            try {
                SimplePacket data = resourceManager.getPipes().get(resourceManager.getServerPort()).take();

                if (dataProcessors.get(data.header.getPort()) == null) {
                    resourceManager.createOutputDataPipe(data.header.getPort());

                    DataProcessor dataProcessor = new DataProcessor(data.header.getPort(), resourceManager);
                    Thread dataProcessorThread = new Thread(dataProcessor);

                    dataProcessors.put(data.header.getPort(), dataProcessor);
                    dataProcessorThreads.put(data.header.getPort(), dataProcessorThread);

                    dataProcessorThread.start();
                    dataProcessor.addToQueue(data);
                } else {
                    dataProcessors.get(data.header.getPort()).addToQueue(data);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void initialize() {
        dataProcessors = new HashMap<>();
        dataProcessorThreads = new HashMap<>();
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
    }
}
