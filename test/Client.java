
import data.MessageState;
import data.SimplePacket;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class Client {

    public Client() {

    }

    @Test
    public void sendWrq() {
        DatagramChannel channel = null;

        try {
            channel = DatagramChannel.open();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            return;
        }

        InetSocketAddress socket = new InetSocketAddress("127.0.0.1", 63);

        try {
            channel.socket().bind(socket);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            return;
        }

        InetSocketAddress serverSocket = new InetSocketAddress("127.0.0.1", 60);

        byte[] data = generateWrq("File.txt", "octet");

        ByteBuffer output = ByteBuffer.wrap(data);

        try {
            channel.send(output, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        byte block = 0;

        File file = new File("C:\\TftpClientRepo\\File.txt");

        byte[] payload = new byte[0];

        try {
            payload = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            buffer.clear();

            try {
                InetSocketAddress clientAddress = (InetSocketAddress) channel.receive(buffer);
                buffer.flip();

                if (buffer.remaining() > 0) {
                    data = new byte[buffer.remaining()];
                    buffer.get(data);

                    if (data[1] == 4 && data[3] == block) {
                        block++;

                        byte[] dataChunk;

                        if (payload.length - (512 * (block)) <= 512) {
                            dataChunk = Arrays.copyOfRange(payload, 512 * (block - 1), payload.length);
                        } else {
                            dataChunk = Arrays.copyOfRange(payload, 512 * (block - 1), 512 * block);
                        }

                        byte[] packetBytes = new byte[2 + 2 + dataChunk.length];
                        packetBytes[0] = 0;
                        packetBytes[1] = 3;
                        packetBytes[2] = 0;
                        packetBytes[3] = block;

                        for (int i = 4; i < dataChunk.length; i++) {
                            packetBytes[i] = dataChunk[i - 4];
                        }

                        output = ByteBuffer.wrap(packetBytes);

                        try {
                            channel.send(output, serverSocket);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return;
                        }

                        if (packetBytes.length < 516) {
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Test
    public void sendRrq() {
        DatagramChannel channel = null;

        try {
            channel = DatagramChannel.open();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            return;
        }

        InetSocketAddress socket = new InetSocketAddress("127.0.0.1", 63);

        try {
            channel.socket().bind(socket);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            return;
        }

        InetSocketAddress serverSocket = new InetSocketAddress("127.0.0.1", 60);

        byte[] data = generateRrq("File.txt", "octet");

        ByteBuffer output = ByteBuffer.wrap(data);

        try {
            channel.send(output, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        byte block = 1;

        File file = new File("C:\\TftpClientRepo\\File.txt");

        if (file.exists()) {
            file.delete();
        }

        final ByteBuffer outBuffer = ByteBuffer.allocate(4096);
        FileChannel writeChannel = null;

        try {
            writeChannel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            buffer.clear();

            try {
                InetSocketAddress clientAddress = (InetSocketAddress) channel.receive(buffer);
                buffer.flip();

                if (buffer.remaining() > 0) {
                    data = new byte[buffer.remaining()];
                    int dataSize = data.length;
                    buffer.get(data);

                    System.out.println("Received packet reply of size: " + data.length);

                    outBuffer.put(Arrays.copyOfRange(data, 4, data.length));

                    data = generateAck(block++);

                    output = ByteBuffer.wrap(data);

                    try {
                        channel.send(output, serverSocket);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    if (dataSize < 512) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        outBuffer.flip();

        try {
            while (outBuffer.hasRemaining()) {
                writeChannel.write(outBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sendAck() {
        byte block = 1;

        DatagramChannel channel = null;

        try {
            channel = DatagramChannel.open();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            return;
        }

        InetSocketAddress socket = new InetSocketAddress("127.0.0.1", 63);

        try {
            channel.socket().bind(socket);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            return;
        }

        InetSocketAddress serverSocket = new InetSocketAddress("127.0.0.1", 60);

        byte[] data = generateAck(block);

        ByteBuffer output = ByteBuffer.wrap(data);

        try {
            channel.send(output, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        while (true) {
            buffer.clear();

            try {
                InetSocketAddress clientAddress = (InetSocketAddress) channel.receive(buffer);
                buffer.flip();

                if (buffer.remaining() > 0) {
                    data = new byte[buffer.remaining()];
                    buffer.get(data);

                    StringBuffer sb = new StringBuffer();

                    for (int i = 0; i < data.length; i++) {
                        sb.append((char) data[i]);
                    }

                    System.out.println(sb.toString());

                    break;
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private byte[] generateRrq(String fileName, String mode) {
        int rrqPacketSize = 2 + fileName.length() + 1 + mode.length() + 1;

        byte[] rrqData = new byte[rrqPacketSize];

        int pos = 0;

        rrqData[pos++] = 0;
        rrqData[pos++] = 1;
        for (byte filenameChar : fileName.getBytes()) {
            rrqData[pos++] = filenameChar;
        }

        rrqData[pos++] = 0;

        for (byte modeChar : mode.getBytes()) {
            rrqData[pos++] = modeChar;
        }

        rrqData[pos++] = 0;

        return rrqData;
    }

    private byte[] generateWrq(String fileName, String mode) {
        int rrqPacketSize = 2 + fileName.length() + 1 + mode.length() + 1;

        byte[] rrqData = new byte[rrqPacketSize];

        int pos = 0;

        rrqData[pos++] = 0;
        rrqData[pos++] = 2;
        for (byte filenameChar : fileName.getBytes()) {
            rrqData[pos++] = filenameChar;
        }

        rrqData[pos++] = 0;

        for (byte modeChar : mode.getBytes()) {
            rrqData[pos++] = modeChar;
        }

        rrqData[pos++] = 0;

        return rrqData;
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
