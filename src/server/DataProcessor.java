package server;

import data.SimplePacket;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DataProcessor implements Runnable {

    private int sourcePort;
    private final ResourceManager resourceManager;
    private BlockingQueue<SimplePacket> queue;


    public DataProcessor(int sourcePort, final ResourceManager resourceManager) {
        this.sourcePort = sourcePort;
        this.resourceManager = resourceManager;

        initialize();
    }

    private void initialize() {
        queue = new ArrayBlockingQueue<>(100);
    }

    @Override
    public void run() {
        while (resourceManager.getState().isRunning()) {
            try {
                SimplePacket data = queue.take();

                byte[] tftpData = data.data;
                byte opsCode = tftpData[1];
                StringBuffer sb = new StringBuffer();

                if (opsCode == 4) {
                    System.out.println("Ack received from " + data.header.getHostName() + ":" + data.header.getPort() + " for block " + tftpData[3]);
                }

                if (opsCode == 1) {
                    for (int i = 2; i < tftpData.length; i++) {
                        if (tftpData[i] == 0) {
                            break;
                        }

                        sb.append((char) tftpData[i]);
                    }

                    String fileName = sb.toString();

                    System.out.println("RRQ received from " + data.header.getHostName() + ":" + data.header.getPort() + " for file " + fileName);

                    byte[] payload = null;

                    File file = new File("C:\\TftpRepo\\" + fileName);

                    if (!file.exists()) {
                        String errorText = "File does not exist.";

                        payload = new byte[2 + 2 + errorText.length() + 1];

                        int pos = 0;

                        payload[pos++] = 0;
                        payload[pos++] = 5;
                        payload[pos++] = 0;
                        payload[pos++] = 1;

                        for (byte character : errorText.getBytes()) {
                            payload[pos++] = character;
                        }

                        payload[pos++] = 0;

                        SimplePacket packet = new SimplePacket(payload, data.header);

                        resourceManager.addDataToPipe(data.header.getPort(), packet);
                    } else {
                        byte[] dataPayload = new byte[0];

                        try {
                            dataPayload = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (dataPayload.length <= 512) {
                            payload = new byte[dataPayload.length + 4];

                            int pos = 0;

                            payload[pos++] = 0;
                            payload[pos++] = 3;
                            payload[pos++] = 0;
                            payload[pos++] = 1;

                            for (byte aData : dataPayload) {
                                payload[pos++] = aData;
                            }

                            SimplePacket packet = new SimplePacket(payload, data.header);

                            resourceManager.addDataToPipe(data.header.getPort(), packet);
                        } else {
                            int sizeLeft = dataPayload.length;
                            int lowerWin = 4;
                            int upperWin = 516;

                            while (true) {
                                int block = 1;

                                if (sizeLeft > 512) {
                                    sizeLeft -= 512;
                                    payload = new byte[516];
                                    int pos = 0;

                                    payload[pos++] = 0;
                                    payload[pos++] = 3;
                                    payload[pos++] = 0;
                                    payload[pos++] = (byte) (block);

                                    byte[] payloadChunk = Arrays.copyOfRange(dataPayload, lowerWin, upperWin);
                                    lowerWin += 512;
                                    upperWin += 512;

                                    for (byte aData : payloadChunk) {
                                        payload[pos++] = aData;
                                    }

                                    SimplePacket packet = new SimplePacket(payload, data.header);

                                    resourceManager.addDataToPipe(data.header.getPort(), packet);

                                    SimplePacket ack = queue.take();

                                    if (ack.data[3] == block) {
                                        block++;
                                    }
                                } else {
                                    payload = new byte[dataPayload.length + 4];

                                    int pos = 0;

                                    payload[pos++] = 0;
                                    payload[pos++] = 3;
                                    payload[pos++] = 0;
                                    payload[pos++] = 1;

                                    for (byte aData : dataPayload) {
                                        payload[pos++] = aData;
                                    }

                                    SimplePacket packet = new SimplePacket(payload, data.header);

                                    resourceManager.addDataToPipe(data.header.getPort(), packet);

                                    SimplePacket ack = queue.take();

                                    if (ack.data[3] == block) {
                                        System.out.println("Success");

                                        break;
                                    }
                                }
                            }
                        }
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

    public void addToQueue(SimplePacket input) {
        try {
            queue.put(input);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
