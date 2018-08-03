package server;

import data.SimplePacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class InputChannel implements Runnable {
    private DatagramChannel channel;
    private final Server server;

    public InputChannel(final Server server) {
        this.server = server;

        initialize();
    }

    @Override
    public void run() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        while (server.getState().isRunning()) {
            buffer.clear();

            try {
                InetSocketAddress clientAddress = (InetSocketAddress) channel.receive(buffer);
                buffer.flip();

                if (buffer.remaining() > 0) {
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);

                    SimplePacket packet = new SimplePacket(data, clientAddress);

                    server.getPipes().get(server.getServerPort()).put(packet);
                }
            } catch (IOException | InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        terminate();
    }

    private void initialize() {
        openChannel();
        bindChannel();
    }

    private void terminate() {
        unbindChannel();
        closeChannel();
    }

    private void openChannel() {
        try {
            channel = DatagramChannel.open();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    private void closeChannel() {
        try {
            channel.close();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    private void bindChannel() {
        InetSocketAddress socket = new InetSocketAddress("127.0.0.1", server.getServerPort());

        try {
            channel.socket().bind(socket);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    private void unbindChannel() {
        try {
            channel.disconnect();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }
}