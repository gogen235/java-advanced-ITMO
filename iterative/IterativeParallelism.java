package info.kgeorgiy.ja.goge.iterative;

import info.kgeorgiy.java.advanced.iterative.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.min;

public class IterativeParallelism implements AdvancedIP {

    private final ParallelMapper mapper;

    /**
     * Creates IterativeParallelism with default realisation.
     */
    public IterativeParallelism() {
        this.mapper = null;
    }

    /**
     * Creates IterativeParallelism with mapper.
     *
     * @param mapper mapper that will be used.
     */
    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        return maximum(threads, values, comparator, 1);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        return minimum(threads, values, comparator, 1);

    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return all(threads, values, predicate, 1);
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return any(threads, values, predicate, 1);
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return count(threads, values, predicate, 1);
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return join(threads, values, 1);
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return filter(threads, values, predicate, 1);
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f)
            throws InterruptedException {
        return map(threads, values, f, 1);
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator, int step)
            throws InterruptedException {
        return method(threads, values, step,
                x -> x.max(comparator).orElseThrow(),
                x -> x.max(comparator).orElseThrow());
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator, int step)
            throws InterruptedException {
        return maximum(threads, values, comparator.reversed(), step);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate, int step)
            throws InterruptedException {
        return method(threads, values, step, x -> x.allMatch(predicate), x -> x.allMatch(it -> it));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate, int step)
            throws InterruptedException {
        return !all(threads, values, predicate.negate(), step);
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate, int step)
            throws InterruptedException {
        return method(threads, values, step,
                x -> (int) x.filter(predicate).count(),
                x -> x.reduce(Integer::sum).orElseThrow());
    }

    @Override
    public String join(int threads, List<?> values, int step) throws InterruptedException {
        return method(threads, values, step,
                x -> x.map(Object::toString).collect(Collectors.joining()),
                x -> x.collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate, int step)
            throws InterruptedException {
        return method(threads, values, step,
                x -> x.filter(predicate).toList(),
                x -> x.flatMap(y -> y.stream().map(z -> (T) z)).toList());
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f, int step)
            throws InterruptedException {
        return method(threads, values, step,
                x -> x.map(f).toList(),
                x -> x.flatMap(y -> y.stream().map(z -> (U) z)).toList());
    }

    @Override
    public <T> T reduce(int threads, List<T> values, T identity, BinaryOperator<T> operator, int step)
            throws InterruptedException {
        return mapReduce(threads, values, Function.identity(), identity, operator, step);
    }

    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, R identity, BinaryOperator<R> operator, int step)
            throws InterruptedException {
        return method(threads, values, step,
                x -> x.map(lift).reduce(identity, operator),
                x -> x.reduce(identity, operator));
    }

    private <T, U, W> U method(int threads,
                               List<? extends T> values, int step,
                               Function<Stream<? extends T>, W> f1,
                               Function<Stream<W>, U> f2)
            throws InterruptedException {
        int elementsNum = Math.ceilDiv(values.size(), step);
        threads = min(threads, elementsNum);
        int blockSize = elementsNum / threads;
        int reminder = elementsNum % threads;
        List<W> result;
        if (mapper == null) {
            ArrayList<Thread> threadsList = new ArrayList<>(threads);
            result = new ArrayList<>(Collections.nCopies(threads, null));
            IntStream.range(0, threads).forEach(i -> threadsList.add(
                    new Thread(() -> result.set(i, f1.apply(getSubList(i, values, blockSize, reminder, step).stream())))
            ));
            startThreads(threadsList);
        } else {
            List<List<? extends T>> subLists = getSubLists(threads, values, blockSize, reminder, step);
            result = mapper.map((x) -> f1.apply(getStepElements(x, step).stream()), subLists);
        }
        return f2.apply(result.stream());
    }

    private <T> List<List<? extends T>> getSubLists(int threads, List<? extends T> values, int blockSize, int reminder, int step) {
        ArrayList<List<? extends T>> subLists = new ArrayList<>(threads);
        int idx = 0;
        for (int i = 0; i < threads; i++) {
            int newIdx = min(values.size(), idx + (blockSize + (reminder > i ? 1 : 0)) * step);
            subLists.add(values.subList(idx, newIdx));
            idx = newIdx;
        }
        return subLists;
    }

    private <T> List<? extends T> getStepElements(List<? extends T> values, int step) {
        List<T> newValue = new ArrayList<>();
        for (int i = 0; i < values.size(); i += step) {
            newValue.add(values.get(i));
        }
        return newValue;
    }

    private <T> List<T> getSubList(int threadIdx, List<? extends T> values, int blockSize, int reminder, int step) {
        ArrayList<T> subList = new ArrayList<>();
        int idx = (threadIdx * blockSize + Math.min(reminder, threadIdx)) * step;
        for (int i = 0; i < blockSize + (reminder > threadIdx ? 1 : 0); i++) {
            subList.add(values.get(idx));
            idx += step;
        }
        return subList;
    }

    private void startThreads(ArrayList<Thread> threadsList) throws InterruptedException {
        threadsList.forEach(Thread::start);
        InterruptedException exp = null;
        for (Thread thread : threadsList) {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException e) {
                    threadsList.forEach(Thread::interrupt);
                    if (exp == null) {
                        exp = e;
                    } else {
                        exp.addSuppressed(e);
                    }
                }
            }
        }
        if (exp != null) {
            throw exp;
        }
    }
}
