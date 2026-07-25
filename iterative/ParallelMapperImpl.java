package info.kgeorgiy.ja.goge.iterative;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threadList;
    private final SynchronisedQueue<Runnable> tasks = new SynchronisedQueue<>();
    private boolean isClosed = false;

    /**
     * Creates ParallelMapperImpl with {@code threads} threads. Start all threads.
     * @param threads num of threads
     */
    public ParallelMapperImpl(final int threads) {
        final Runnable worker = () -> {
            try {
                while (true) {
                    tasks.poll().run();
                }
            } catch (final InterruptedException ignored) {
            }
        };
        threadList = IntStream.range(0, threads).mapToObj(i -> new Thread(worker)).toList();
        threadList.forEach(Thread::start);
    }

    @Override
    public <T, R> List<R> map(
            final Function<? super T, ? extends R> f,
            final List<? extends T> args
    ) throws InterruptedException {
        final List<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));
        int[] counter = {0};
        RuntimeException[] exception = {null};
        IntStream.range(0, args.size()).forEach(i -> tasks.add(() -> {
            try {
                result.set(i, f.apply(args.get(i)));
            } catch (RuntimeException e) {
                synchronized (exception) {
                    exception[0] = e;
                }
            }
            synchronized (counter) {
                if (++counter[0] == args.size()) {
                    counter.notify();
                }
            }
        }));
        synchronized (counter) {
            while (counter[0] != args.size()) {
                counter.wait();
            }
        }
        if (exception[0] != null) {
            throw exception[0];
        }
        return result;
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }
        isClosed = true;
        threadList.forEach(Thread::interrupt);
        InterruptedException exception = null;
        for (final Thread thread : threadList) {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (final InterruptedException e) {
                    exception = e;
                }
            }
        }
        if (exception != null) {
            Thread.currentThread().interrupt();
        }
    }

    private static class SynchronisedQueue<T> {
        private final Deque<T> deque = new ArrayDeque<>();

        public synchronized T poll() throws InterruptedException {
            while (deque.isEmpty()) {
                wait();
            }
            return deque.removeFirst();
        }

        public synchronized void add(final T element) {
            deque.add(element);
            notify();
        }
    }
}
