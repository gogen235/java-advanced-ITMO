package info.kgeorgiy.ja.goge.hello;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class HelloUDPNonblockingServer extends AbstractHelloUDPServer {
    private Selector selector;
    private Thread thread;

    /**
     * Creates HelloUDPServer with given arguments and start it.
     *
     * @param args arguments
     */
    public static void main(final String[] args) {
        checkMain(HelloUDPNonblockingServer::new, args);
    }

    @Override
    protected void process(int threads, Map<Integer, String> ports) {
        try {
            selector = Selector.open();
            for (var port : ports.entrySet()) {
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                channel.bind(new InetSocketAddress(port.getKey()));
                channel.register(selector, SelectionKey.OP_READ, new AddressResponse(null, null, port.getValue()));
            }
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted() && selector.isOpen()) {
                    try {
                        selector.select(key -> {
                            try {
                                final DatagramChannel channel = (DatagramChannel) key.channel();
                                AddressResponse addressResponse = (AddressResponse) key.attachment();
                                if (key.isReadable()) {
                                    Util.AddressMessage addressMessage = Util.bufferRead(buffer, channel);
                                    Future<String> future = receiveExecutor.submit(() ->
                                            addressResponse.pattern.replace("$", addressMessage.message())
                                    );
                                    key.attach(new AddressResponse(addressMessage.address(), future, addressResponse.pattern()));
                                    key.interestOps(SelectionKey.OP_WRITE);
                                } else if (key.isWritable()) {
                                    byte[] messageBytes = addressResponse.response().get().getBytes(StandardCharsets.UTF_8);
                                    Util.bufferWrite(buffer, addressResponse.address(), messageBytes, channel, key);
                                }
                            } catch (IOException | ExecutionException | InterruptedException | CancelledKeyException e) {
                                System.err.println(e.getMessage());
                            }
                        });
                    } catch (IOException | ClosedSelectorException e) {
                        System.err.println(e.getMessage());
                    }
                }
            });
            thread.start();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void close() {
        if (thread != null) {
            thread.interrupt(); // NOTE: don't wait end of thread
        }
        try {
            if (selector != null) {
                selector.keys().forEach(x -> {
                    try {
                        x.channel().close();
                    } catch (IOException e) {
                        System.err.println("Exception while close" + e.getMessage());
                    }
                });
                selector.close();
            }
        } catch (IOException e) {
            System.err.println("Exception while close" + e.getMessage());
        }
        if (receiveExecutor != null) {
            receiveExecutor.close();
        }
    }

    private record AddressResponse(SocketAddress address, Future<String> response, String pattern) {
    }
}
