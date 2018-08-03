package server;

import data.MessageState;
import data.SimplePacket;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DataProcessor implements Runnable {

    private int sourcePort;
    private final ResourceManager resourceManager;
    private BlockingQueue<SimplePacket> queue;

    private MessageState msgState;


    public DataProcessor(int sourcePort, final ResourceManager resourceManager) {
        this.sourcePort = sourcePort;
        this.resourceManager = resourceManager;

        initialize();
    }

    private void initialize() {
        msgState = MessageState.INITIAL;
        queue = new ArrayBlockingQueue<>(100);
    }

    @Override
    public void run() {
        while (resourceManager.getState().isRunning()) {
            try {
                SimplePacket data = queue.take();
                byte[] tftpData = data.data;
                byte opsCode = tftpData[1];

                System.out.println("DataProcessor(" + data.header.getHostName() + ":" + data.header.getPort() + ") received OPSCODE(" + opsCode + ").");

                switch (msgState) {
                    case INITIAL: {
                        if (opsCode != 1 && opsCode != 2) {
                            generateIllegalTftpOperationPacket(opsCode, data);
                        } else {

                        }

                        break;
                    }
                    case RRQ_START: {
                        break;
                    }
                    case RRQ_NEXT: {
                        break;
                    }
                    case WRQ_START: {
                        break;
                    }
                    case WRQ_NEXT: {
                        break;
                    }
                    case END: {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        terminate();
    }

    private void terminate() {

    }

    private void generateIllegalTftpOperationPacket(int opsCode, SimplePacket data) {
        byte[] payload = null;

        String errorText = "Invalid server request - OPSCODE(" + opsCode + ").";

        payload = new byte[2 + 2 + errorText.length() + 1];

        int pos = 0;

        payload[pos++] = 0;
        payload[pos++] = 5;
        payload[pos++] = 0;
        payload[pos++] = 4;

        for (byte character : errorText.getBytes()) {
            payload[pos++] = character;
        }

        payload[pos++] = 0;

        SimplePacket packet = new SimplePacket(payload, data.header);
        resourceManager.addDataToPipe(data.header.getPort(), packet);
    }

    public void addToQueue(SimplePacket input) {
        try {
            queue.put(input);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
