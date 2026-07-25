package info.kgeorgiy.ja.goge.hello;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class HelloUDPClient extends AbstractHelloUDPClient {
    /**
     * Creates HelloUDPClient with given arguments and run it.
     *
     * @param args arguments
     */
    public static void main(final String[] args) {
        checkMain(HelloUDPClient::new, args);
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        try (final ExecutorService threadPool = newFixedThreadPool(threads)) {
            IntStream.range(1, threads + 1).forEach(i ->
                    threadPool.execute(() -> runRequests(host, requests, prefix, i, port))
            );
        }
    }

    protected void runRequests(final String host, final int requests, final String prefix, final int threadNum, final int port) {
        final InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(host);
        } catch (final UnknownHostException e) {
            System.err.println(e.getMessage());
            return;
        }
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);
            for (int j = 1; j <= requests; j++) {
                final String message = getMessage(prefix, threadNum, j);
                byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
                int length = responseBytes.length;
                final DatagramPacket messagePacket = new DatagramPacket(
                        responseBytes,
                        length,
                        inetAddress,
                        port);
                responseBytes = new byte[socket.getReceiveBufferSize()];
                final DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length);
                while (true) {
                    socket.send(messagePacket);
                    try {
                        socket.receive(responsePacket);

                        final String response = Util.getResponse(responsePacket.getData(), responsePacket.getLength());
                        if (!checkResponse(response, threadNum, j)) {
                            System.err.println("Request: " + message + ", response: " + response + " - wrong answer");
                            continue;
                        }
                        System.out.println("Request: " + message + ", response: " + response);
                        break;
                    } catch (final SocketTimeoutException e) {
                        System.err.println("Timeout exceeded");
                    }
                }
            }
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        }
    }

}
