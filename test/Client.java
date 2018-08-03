import org.junit.Test;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class Client {

    public Client() {

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

    private byte[] generateAck(byte block) {
        byte[] ackData = new byte[4];

        ackData[0] = 0;
        ackData[1] = 4;
        ackData[2] = 0;
        ackData[3] = block;

        return ackData;
    }
}
