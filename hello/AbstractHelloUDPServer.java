package info.kgeorgiy.ja.goge.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static java.util.concurrent.Executors.newFixedThreadPool;

public abstract class AbstractHelloUDPServer implements NewHelloServer {

    protected ExecutorService receiveExecutor;

    protected static void checkMain(final Supplier<AbstractHelloUDPServer> server, final String[] args) {
        if (args == null || args.length != 2) {
            throw new IllegalArgumentException("2 arguments expected");
        }
        // :NOTE: same
        for (final String arg : args) {
            if (arg == null) {
                throw new IllegalArgumentException("Argument can't be null");
            }
        }
        try (final AbstractHelloUDPServer helloUDPServer = server.get()) {
            helloUDPServer.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            Thread.sleep(20000); // :NOTE: can ask for some input from console instead of sleeping
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Illegal arguments" + e.getMessage());
        } catch (final InterruptedException e) {
            System.err.println("Thread was interrupted");
        }
    }

    @Override
    public void start(final int threads, final Map<Integer, String> ports) {
        if (ports.isEmpty() || threads <= 0) {
            return;
        }
        receiveExecutor = newFixedThreadPool(threads);
        process(threads, ports);
    }

    protected abstract void process(final int threads, final Map<Integer, String> ports);
}
