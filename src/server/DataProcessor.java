package server;

import data.SimplePacket;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DataProcessor implements Runnable {

    private int sourcePort;
    private final Server server;
    private BlockingQueue<SimplePacket> queue;


    public DataProcessor(int sourcePort, final Server server) {
        this.sourcePort = sourcePort;
        this.server = server;

        initialize();
    }

    private void initialize() {
        queue = new ArrayBlockingQueue<>(100);
    }

    @Override
    public void run() {
        while (server.getState().isRunning()) {
            try {
                SimplePacket data = queue.take();

                byte[] tftpData = data.data;
                byte opsCode = tftpData[1];
                StringBuffer sb = new StringBuffer();

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

                    File file = new File(fileName);
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
                    }

                    SimplePacket packet = new SimplePacket(payload, data.header);

                    server.addDataToPipe(data.header.getPort(), packet);
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
