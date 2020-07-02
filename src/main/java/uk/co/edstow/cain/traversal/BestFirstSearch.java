package uk.co.edstow.cain.traversal;

import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class BestFirstSearch<T> implements TraversalSystem<T> {
    public static <E> Supplier<BestFirstSearch<E>> BestFirstSearchFactory(Function<E, Double> f){return () -> new BestFirstSearch<E>(f);}

    private static class Entry<T> implements Comparable<Entry<T>> {
        final T ws;
        final double f;

        private Entry(T ws, double f) {
            this.ws = ws;
            this.f = f;
        }

        @Override
        public int compareTo(Entry o) {
            return Double.compare(f, o.f);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return Double.compare(entry.f, f) == 0 &&
                    Objects.equals(ws, entry.ws);
        }

        @Override
        public int hashCode() {

            return Objects.hash(ws, f);
        }
    }
    private final PriorityBlockingQueue<Entry<T>> workQueue = new PriorityBlockingQueue<>();
    private final Function<T, Double> f;
    private T next = null;

    private BestFirstSearch(Function<T, Double> f) {
        this.f = f;
    }

    @Override
    public void add(T child, T next) {
        if(child!=null)workQueue.add(new Entry<>(child, f.apply(child)));
        this.next = next;
        if(next!=null)workQueue.add(new Entry<>(next, f.apply(next)));
    }

    @Override
    public void add(T child) {
        if(child!=null)workQueue.add(new Entry<>(child, f.apply(child)));
    }

    @Override
    public T poll() {
        if(this.next!=null){
            T out = next;
            this.next = null;
            return out;
        }
        Entry<T> e = workQueue.poll();
        if(e == null){
            return null;
        }
        return e.ws;
    }

    @Override
    public T steal(TraversalSystem<T> system) throws InterruptedException {
        if(system instanceof BestFirstSearch) {
            Entry<T> e = ((BestFirstSearch<T>) system).workQueue.poll(100, TimeUnit.MILLISECONDS);
            if (e == null) {
                return null;
            }
            return e.ws;
        }
        return null;
    }
}
