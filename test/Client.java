import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;

public class Client {

    public Client() {

    }

    @Test
    public void test() {
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

        byte[] data = generateRrq("Test.txt", "octet");

        ByteBuffer output = ByteBuffer.wrap(data);

        try {
            channel.send(output, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        boolean isRunning = true;

        while (isRunning) {
            buffer.clear();

            try {
                InetSocketAddress clientAddress = (InetSocketAddress) channel.receive(buffer);
                buffer.flip();

                if (buffer.remaining() > 0) {
                    System.out.println(clientAddress.getHostName());
                    System.out.println(clientAddress.getPort());

                    byte[] response = new byte[buffer.remaining()];
                    buffer.get(response);

                    byte opsCode = response[1];
                    StringBuffer sb = new StringBuffer();

                    if (opsCode == 5) {
                        for (int i = 4; i < response.length; i++) {
                            if (response[i] == 0) {
                                break;
                            }

                            sb.append((char) response[i]);
                        }

                        System.out.println(sb.toString());
                    }

                    Assert.assertEquals(5, response[1]);

                    isRunning = false;
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
                return;
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
}
