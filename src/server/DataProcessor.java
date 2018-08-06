package server;

import data.MessageState;
import data.SimplePacket;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class DataProcessor implements Runnable {

    private final ResourceManager resourceManager;
    private final int port;

    private BlockingQueue<SimplePacket> queue;
    private MessageState msgState;

    public DataProcessor(final int port, final ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        this.port = port;

        initialize();
    }

    private void initialize() {
        msgState = MessageState.INITIAL;
        queue = new ArrayBlockingQueue<>(100);
    }

    @Override
    public void run() {
        byte block = 0;
        byte[] payload = null;

        final ByteBuffer outBuffer = ByteBuffer.allocate(4096);
        FileChannel writeChannel = null;

        while (resourceManager.getPortResourceState(port)) {
            SimplePacket data = null;

            try {
                data = queue.poll(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (data != null) {
                byte[] tftpData = data.data;
                byte opsCode = tftpData[1];

                System.out.println("DataProcessor(" + data.header.getHostName() + ":" + data.header.getPort() + ") received OPSCODE(" + opsCode + ").");

                switch (msgState) {
                    case INITIAL: {
                        if (opsCode != 1 && opsCode != 2) {
                            generateIllegalTftpOperationPacket(opsCode, data);
                            msgState = MessageState.TERMINATE;
                        } else {
                            if (opsCode == 1) {
                                StringBuilder sb = new StringBuilder();
                                for (int i = 2; i < tftpData.length; i++) {
                                    if (tftpData[i] == 0) {
                                        break;
                                    }

                                    sb.append((char) tftpData[i]);
                                }

                                System.out.println("DataProcessor(" + data.header.getHostName() + ":" + data.header.getPort() + ") requested to upload " + sb.toString() + " to client.");

                                File fileToStream = new File("C:\\TftpRepo\\" + sb.toString());

                                if (!fileToStream.exists()) {
                                    generateFileNotFoundPacket(opsCode, data);

                                    msgState = MessageState.TERMINATE;
                                } else {
                                    try {
                                        payload = Files.readAllBytes(fileToStream.toPath());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    block = 1;

                                    byte[] firstDataChunk;

                                    if (payload.length > 512) {
                                        firstDataChunk = Arrays.copyOfRange(payload, 0, 512);
                                    } else {
                                        firstDataChunk = Arrays.copyOfRange(payload, 0, payload.length);
                                    }

                                    byte[] packetBytes = new byte[2 + 2 + firstDataChunk.length];
                                    packetBytes[0] = 0;
                                    packetBytes[1] = 3;
                                    packetBytes[2] = 0;
                                    packetBytes[3] = block;

                                    System.arraycopy(firstDataChunk, 0, packetBytes, 4, firstDataChunk.length - 4);

                                    SimplePacket packet = new SimplePacket(packetBytes, data.header);
                                    resourceManager.addDataToPipe(data.header.getPort(), packet);

                                    System.out.println("DataProcessor(" + data.header.getHostName() + ":" + data.header.getPort() + ") sent Block(" + block + ")");

                                    msgState = MessageState.RRQ_NEXT;
                                }
                            } else if (opsCode == 2) {
                                StringBuilder sb = new StringBuilder();
                                for (int i = 2; i < tftpData.length; i++) {
                                    if (tftpData[i] == 0) {
                                        break;
                                    }

                                    sb.append((char) tftpData[i]);
                                }

                                if (writeChannel != null) {
                                    try {
                                        writeChannel.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                                File file = new File("C:\\TftpRepo\\" + sb.toString());

                                if (file.exists()) {
                                    file.delete();
                                }

                                try {
                                    writeChannel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                SimplePacket packet = new SimplePacket(generateAck(block++), data.header);
                                resourceManager.addDataToPipe(data.header.getPort(), packet);

                                msgState = MessageState.WRQ_NEXT;
                            }
                        }

                        break;
                    }
                    case RRQ_NEXT: {
                        if (opsCode != 4) {
                            generateIllegalTftpOperationPacket(opsCode, data);
                            msgState = MessageState.TERMINATE;
                        } else {
                            if (payload.length <= 512) {
                                if (tftpData[3] == block) {
                                    msgState = MessageState.END;
                                } else {
                                    generateAccessViolationPacket(opsCode, data);
                                    msgState = MessageState.TERMINATE;
                                }
                            } else {
                                if (tftpData[3] == block) {
                                    block++;

                                    byte[] dataChunk;

                                    if (payload.length - (512 * (block - 1)) <= 512) {
                                        dataChunk = Arrays.copyOfRange(payload, 512 * (block - 1), payload.length);

                                        msgState = MessageState.END;
                                    } else {
                                        dataChunk = Arrays.copyOfRange(payload, 512 * (block - 1), 512 * block);
                                    }

                                    byte[] packetBytes = new byte[2 + 2 + dataChunk.length];
                                    packetBytes[0] = 0;
                                    packetBytes[1] = 3;
                                    packetBytes[2] = 0;
                                    packetBytes[3] = block;

                                    System.arraycopy(dataChunk, 0, packetBytes, 4, dataChunk.length - 4);

                                    SimplePacket packet = new SimplePacket(packetBytes, data.header);
                                    resourceManager.addDataToPipe(data.header.getPort(), packet);

                                    System.out.println("DataProcessor(" + data.header.getHostName() + ":" + data.header.getPort() + ") sent Block(" + block + ")");
                                } else {
                                    System.out.println("DataProcessor(" + data.header.getHostName() + ":" + data.header.getPort() + ") received invalid ACK for Block(" + tftpData[3] + ")");
                                    generateAccessViolationPacket(opsCode, data);
                                    msgState = MessageState.TERMINATE;
                                }
                            }
                        }

                        break;
                    }
                    case WRQ_NEXT: {
                        if (opsCode != 3) {
                            generateIllegalTftpOperationPacket(opsCode, data);
                            msgState = MessageState.TERMINATE;
                        } else {
                            if (tftpData[3] == block) {
                                outBuffer.put(Arrays.copyOfRange(tftpData, 4, tftpData.length));

                                SimplePacket packet = new SimplePacket(generateAck(block++), data.header);
                                resourceManager.addDataToPipe(data.header.getPort(), packet);

                                if (tftpData.length < 516) {
                                    outBuffer.flip();

                                    try {
                                        while (outBuffer.hasRemaining()) {
                                            writeChannel.write(outBuffer);
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    msgState = MessageState.TERMINATE;
                                }
                            } else {
                                System.out.println("DataProcessor(" + data.header.getHostName() + ":" + data.header.getPort() + ") received invalid ACK for Block(" + tftpData[3] + ")");
                                generateAccessViolationPacket(opsCode, data);
                                msgState = MessageState.TERMINATE;
                            }

                            break;
                        }
                    }
                    case END: {
                        if (tftpData[3] == block) {
                            System.out.println("DataProcessor(" + data.header.getHostName() + ":" + data.header.getPort() + ") transferred data successfully.");
                        }

                        msgState = MessageState.TERMINATE;

                        break;
                    }
                }

                if (msgState == MessageState.TERMINATE) {
                    System.out.println("DataProcessor(" + data.header.getHostName() + ":" + data.header.getPort() + ") finished servicing request.");

                    block = 0;
                    payload = null;

                    outBuffer.clear();

                    msgState = MessageState.INITIAL;
                }
            }
        }

        terminate();

    }

    private void terminate() {
        System.out.println("Tearing down data processor for port(" + port + ").");
    }

    private void generateIllegalTftpOperationPacket(final int opsCode, SimplePacket data) {
        System.out.println("DataProcessor(" + data.header.getHostName() + ":" + data.header.getPort() + ") received illegal tftp request.");

        byte[] payload;

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

        payload[pos] = 0;

        SimplePacket packet = new SimplePacket(payload, data.header);
        resourceManager.addDataToPipe(data.header.getPort(), packet);
    }

    private void generateFileNotFoundPacket(final int opsCode, SimplePacket data) {
        System.out.println("DataProcessor(" + data.header.getHostName() + ":" + data.header.getPort() + ") cannot find file for client.");

        byte[] payload;

        String errorText = "File not found - OPSCODE(" + opsCode + ").";

        payload = new byte[2 + 2 + errorText.length() + 1];

        int pos = 0;

        payload[pos++] = 0;
        payload[pos++] = 5;
        payload[pos++] = 0;
        payload[pos++] = 1;

        for (byte character : errorText.getBytes()) {
            payload[pos++] = character;
        }

        payload[pos] = 0;

        SimplePacket packet = new SimplePacket(payload, data.header);
        resourceManager.addDataToPipe(data.header.getPort(), packet);
    }

    private void generateAccessViolationPacket(final int opsCode, SimplePacket data) {
        System.out.println("DataProcessor(" + data.header.getHostName() + ":" + data.header.getPort() + ") encountered erroneous client request state.");

        byte[] payload;

        String errorText = "Access violation - OPSCODE(" + opsCode + ").";

        payload = new byte[2 + 2 + errorText.length() + 1];

        int pos = 0;

        payload[pos++] = 0;
        payload[pos++] = 5;
        payload[pos++] = 0;
        payload[pos++] = 2;

        for (byte character : errorText.getBytes()) {
            payload[pos++] = character;
        }

        payload[pos] = 0;

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

    private byte[] generateAck(byte block) {
        byte[] ackData = new byte[4];

        ackData[0] = 0;
        ackData[1] = 4;
        ackData[2] = 0;
        ackData[3] = block;

        return ackData;
    }
}
