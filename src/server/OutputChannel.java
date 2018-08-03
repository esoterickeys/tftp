package server;

import data.SimplePacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class OutputChannel implements Runnable {
    private DatagramChannel channel;
    private final ResourceManager resourceManager;
    private int port;

    public OutputChannel(int port, final ResourceManager resourceManager) {
        this.port = port;
        this.resourceManager = resourceManager;

        initialize();
    }

    @Override
    public void run() {
        while (resourceManager.getState().isRunning()) {
            try {
                SimplePacket data = resourceManager.getPipes().get(port).take();

                ByteBuffer output = ByteBuffer.wrap(data.data);

                channel.send(output, data.header);
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
        InetSocketAddress socket = new InetSocketAddress(port);

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