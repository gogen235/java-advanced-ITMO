package info.kgeorgiy.ja.goge.hello;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class HelloUDPServer extends AbstractHelloUDPServer {
    private List<SocketPattern> sockets = new ArrayList<>();
    private ExecutorService portsExecutor;

    /**
     * Creates HelloUDPServer with given arguments and start it.
     *
     * @param args arguments
     */
    public static void main(final String[] args) {
        checkMain(HelloUDPServer::new, args);
    }

    @Override
    protected void process(final int threads, final Map<Integer, String> ports) {
        portsExecutor = newFixedThreadPool(ports.size());
        sockets = ports.entrySet().stream().map((x) -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket(x.getKey());
            } catch (final SocketException e) {
                close();
            }
            return new SocketPattern(socket, x.getValue());
        }).toList();
        sockets.forEach((socketPattern) -> portsExecutor.submit(() -> runPort(socketPattern)));
    }

    private void runPort(final SocketPattern socketPattern) {
        final DatagramSocket socket = socketPattern.socket();
        final int socketSize;
        try {
            socketSize = socket.getReceiveBufferSize();
        } catch (final SocketException e) {
            close();
            return;
        }
        while (true) {
            try {
                final byte[] messageBytes = new byte[socketSize];
                final DatagramPacket responsePacket = new DatagramPacket(messageBytes, messageBytes.length);
                socket.receive(responsePacket);
                receiveExecutor.submit(() -> runReceive(responsePacket, socketPattern.pattern(), socket));
            } catch (final IOException ignored) {
                if (socket.isClosed()) {
                    System.err.println("Socket was closed");
                    return;
                }
            }
        }
    }

    private void runReceive(final DatagramPacket responsePacket, final String pattern, final DatagramSocket socket) {
        try {
            final DatagramPacket messagePacket = getDatagramPacket(responsePacket, pattern);
            socket.send(messagePacket);
        } catch (final IOException ignored) {
        }
    }

    private static DatagramPacket getDatagramPacket(final DatagramPacket responsePacket, final String hello) {
        final String message = new String(responsePacket.getData(),
                0,
                responsePacket.getLength(),
                StandardCharsets.UTF_8);
        final String response = hello.replace("$", message);

        final byte[] sendData = response.getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(sendData, sendData.length, responsePacket.getAddress(), responsePacket.getPort());
    }

    @Override
    public void close() {
        sockets.forEach((x) -> x.socket().close()); // :NOTE: close may throw
        if (portsExecutor != null) {
            portsExecutor.close();
        }
        if (receiveExecutor != null) {
            receiveExecutor.close();
        }
    }

    private record SocketPattern(DatagramSocket socket, String pattern) {
    }
}
