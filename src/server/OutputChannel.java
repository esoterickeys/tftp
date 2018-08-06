package server;

import data.SimplePacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.TimeUnit;

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
        while (resourceManager.getPortResourceState(port)) {
            SimplePacket data = null;

            try {
                data = resourceManager.getPipes().get(port).poll(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (data != null) {
                try {
                    ByteBuffer output = ByteBuffer.wrap(data.data);

                    channel.send(output, data.header);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        terminate();
    }

    private void initialize() {
        openChannel();
        bindChannel();
    }

    private void terminate() {
        System.out.println("Tearing down output channel for port(" + port + ").");
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