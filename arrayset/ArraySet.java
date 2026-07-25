package info.kgeorgiy.ja.goge.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractList<T> implements NavigableSet<T> {
    private final List<T> list;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        this.list = List.of();
        this.comparator = null;
    }

    public ArraySet(Collection<? extends T> collection) {
        this.list = new TreeSet<T>(collection).stream().toList();
        this.comparator = null;
    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> comparator) {
        TreeSet<T> set = new TreeSet<>(comparator);
        set.addAll(collection);
        this.list = set.stream().toList();
        this.comparator = comparator;
    }


    private ArraySet(List<T> list, Comparator<? super T> comparator) {
        this.list = list;
        this.comparator = comparator;
    }

    @Override
    public T lower(T element) {
        return getOrNull(lowerIdx(element));
    }

    @Override
    public T floor(T element) {
        return getOrNull(floorIdx(element));
    }

    @Override
    public T ceiling(T element) {
        return getOrNull(ceilingIdx(element));
    }

    @Override
    public T higher(T element) {
        return getOrNull(higherIdx(element));
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException("pollFirst is unsupported");
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException("pollLast is unsupported");
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException("add is unsupported");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("remove is unsupported");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object item : c) {
            if (!contains(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException("addAll is unsupported");
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException("addAll is unsupported");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("retainAll is unsupported");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("removeAll is unsupported");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("clear is unsupported");
    }

    @Override
    public T get(int index) {
        return list.get(index);
    }

    @Override
    public T set(int index, T element) {
        throw new UnsupportedOperationException("set is unsupported");
    }

    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException("add is unsupported");
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException("remove is unsupported");
    }

    @SuppressWarnings("unchecked")
    @Override
    public int indexOf(Object o) {
        int idx = Collections.binarySearch(list, (T) o, getComparator());
        return idx >= 0 ? idx : -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        return indexOf(o);
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return reversed();
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if (less(fromElement, toElement)) {
            throw new IllegalArgumentException("toElement is less then fromElement");
        }
        int from = fromInclusive ? ceilingIdx(fromElement) : higherIdx(fromElement);
        int to = toInclusive ? floorIdx(toElement) + 1 : lowerIdx(toElement) + 1;
        return new ArraySet<>(from < to ? list.subList(from, to) : List.of(), comparator);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        T left = isEmpty() || less(first(), toElement) ? toElement : first();
        return subSet(left, true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        T right = isEmpty() || less(fromElement, last()) ? fromElement : last();
        return subSet(fromElement, inclusive, right, true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T removeFirst() {
        return NavigableSet.super.removeFirst();
    }

    @Override
    public T removeLast() {
        return NavigableSet.super.removeLast();
    }

    @Override
    public ArraySet<T> reversed() {
        return new ArraySet<>(list.reversed(), Collections.reverseOrder(getComparator()));
    }

    @Override
    public T first() {
        return list.getFirst();
    }

    @Override
    public T last() {
        return list.getLast();
    }

    @Override
    public Spliterator<T> spliterator() {
        return super.spliterator();
    }

    @Override
    public void addFirst(T t) {
        super.addFirst(t);
    }

    @Override
    public void addLast(T t) {
        super.addLast(t);
    }

    @Override
    public T getFirst() {
        return super.getFirst();
    }

    @Override
    public T getLast() {
        return super.getLast();
    }

    @SuppressWarnings("unchecked")
    private Comparator<? super T> getComparator() {
        if (comparator != null) {
            return comparator;
        } else {
            return (Comparator<? super T>) Comparator.naturalOrder();
        }
    }

    private boolean less(T first, T second) {
        return getComparator().compare(first, second) > 0;
    }

    private T getOrNull(int idx) {
        return 0 <= idx && idx < size() ? list.get(idx) : null;
    }

    private int findElement(T element, int cIfFound, int cIfNotFound) {
        int idx = Collections.binarySearch(list, element, comparator);
        if (idx >= 0) {
            return idx + cIfFound;
        } else {
            return -idx + cIfNotFound;
        }
    }
    private int lowerIdx(T element) {
        return findElement(element, -1, -2);
    }

    private int floorIdx(T element) {
        return findElement(element, 0, -2);
    }

    private int ceilingIdx(T element) {
        return findElement(element, 0, -1);
    }

    private int higherIdx(T element) {
        return findElement(element, 1, -1);
    }
}
