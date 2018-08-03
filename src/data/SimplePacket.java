package data;

import java.net.InetSocketAddress;

public class SimplePacket {
    public byte[] data;
    public InetSocketAddress header;

    public SimplePacket(byte[] data, InetSocketAddress header) {
        this.data = data;
        this.header = header;
    }
}
