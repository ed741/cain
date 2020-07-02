package uk.co.edstow.cain.traversal;

import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class HOS<T> implements TraversalSystem<T> {
    public static <E> Supplier<HOS<E>> HOSFactory(){return HOS::new;}

    private static class Entry<T> implements Comparable<Entry<T>> {
        final T ws;
        final int f;
        private Entry(T ws, int f) {
            this.ws = ws;
            this.f = f;
        }
        @Override
        public int compareTo(Entry o) {
            return Integer.compare(f, o.f);
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return f == entry.f &&
                    Objects.equals(ws, entry.ws);
        }
        @Override
        public int hashCode() {
            return Objects.hash(ws, f);
        }
    }

    private final PriorityBlockingQueue<Entry<T>> workQueue = new PriorityBlockingQueue<>();
    private int last = 0;
    @Override
    public void add(T child, T next) {
        if(child!=null)workQueue.add(new Entry<>(child, 0));
        if(next!=null)workQueue.add(new Entry<>(next, last+1));
    }

    @Override
    public void add(T child) {
        if(child!=null)workQueue.add(new Entry<>(child, 0));
    }

    @Override
    public T poll() {
        Entry<T> e = workQueue.poll();
        if(e != null) {
            last = e.f;
            return e.ws;
        } else {
            return null;
        }
    }

    @Override
    public T steal(TraversalSystem<T> system) throws InterruptedException {
        if(system instanceof HOS){
            HOS<T> s = (HOS<T>) system;
            Entry<T> e = s.workQueue.poll(100, TimeUnit.MILLISECONDS);
            if(e == null){
                return null;
            }
            last = e.f;
            return e.ws;
        }
        return null;
    }
}
