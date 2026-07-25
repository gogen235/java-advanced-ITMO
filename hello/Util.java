package info.kgeorgiy.ja.goge.hello;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;

public class Util {

    public static final int BYTE_BUFFER_SIZE = 1024;
    public static String getResponse(final byte[] bytes, final int len) {
        return new String(bytes, 0, len, StandardCharsets.UTF_8);
    }

    public static void bufferWrite(ByteBuffer buffer, SocketAddress address, byte[] bytes, DatagramChannel channel, SelectionKey key) throws IOException {
        buffer.clear();
        buffer.put(bytes);
        buffer.flip();
        channel.send(buffer, address);
        key.interestOps(SelectionKey.OP_READ);
    }

    public static AddressMessage bufferRead(ByteBuffer buffer, DatagramChannel channel) throws IOException {
        buffer.clear();
        SocketAddress address = channel.receive(buffer);
        int sz = BYTE_BUFFER_SIZE - buffer.remaining();
        byte[] bytes = new byte[sz];
        buffer.flip();
        buffer.get(bytes);
        return new AddressMessage(address, getResponse(bytes, sz));
    }

    public record AddressMessage(SocketAddress address, String message) {
    }
}
