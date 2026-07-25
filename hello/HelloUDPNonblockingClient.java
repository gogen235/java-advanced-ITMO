package info.kgeorgiy.ja.goge.hello;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;

public class HelloUDPNonblockingClient extends AbstractHelloUDPClient {

    /**
     * Creates HelloUDPClient with given arguments and run it.
     *
     * @param args arguments
     */
    public static void main(final String[] args) {
        checkMain(HelloUDPNonblockingClient::new, args);
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        ByteBuffer buffer = ByteBuffer.allocate(Util.BYTE_BUFFER_SIZE);
        try (Selector selector = Selector.open()) {
            for (int i = 1; i <= threads; i++) {
                DatagramChannel channel = DatagramChannel.open(); // NOTE: not closed
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_WRITE, new IntPair(i, 1));
            }
            while (!Thread.currentThread().isInterrupted() && !selector.keys().isEmpty()) {
                if (selector.select(key ->
                {
                    try {
                        final DatagramChannel channel = (DatagramChannel) key.channel();
                        IntPair pair = (IntPair) key.attachment();
                        int threadNum = pair.first;
                        int requestNum = pair.second;
                        final String message = getMessage(prefix, threadNum, requestNum);

                        if (key.isWritable()) {
                            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                            Util.bufferWrite(buffer, new InetSocketAddress(host, port), messageBytes, channel, key);
                        } else if (key.isReadable()) {
                            final String response = Util.bufferRead(buffer, channel).message();
                            if (checkResponse(response, threadNum, requestNum)) {
                                System.out.println("Request: " + message + ", response: " + response);
                                if (++requestNum <= requests) {
                                    key.attach(new IntPair(threadNum, requestNum));
                                    key.interestOps(SelectionKey.OP_WRITE);
                                } else {
                                    key.cancel();
                                }
                            } else {
                                System.err.println("Request: " + message + ", response: " + response + " - wrong answer");
                                key.interestOps(SelectionKey.OP_WRITE);
                            }
                        }
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }
                }, TIMEOUT) == 0) {
                    selector.keys().forEach(key -> key.interestOps(SelectionKey.OP_WRITE));
                }
            }
            selector.keys().forEach(x -> {
                try {
                    x.channel().close();
                } catch (IOException e) {
                    System.err.println("Exception while closing");
                }
            });
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private record IntPair(int first, int second){}

}
