package uk.co.edstow.cain.traversal;

import uk.co.edstow.cain.structures.WorkState;

import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class BestFirstSearch implements TraversalSystem {
    public static Supplier<BestFirstSearch> BestFirstSearchFactory(Function<WorkState, Double> f){return () -> new BestFirstSearch(f);}

    private static class Entry implements Comparable<Entry> {
        final WorkState ws;
        final double f;

        private Entry(WorkState ws, double f) {
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
    private final PriorityBlockingQueue<Entry> workQueue = new PriorityBlockingQueue<>();
    private final Function<WorkState, Double> f;
    private WorkState next = null;

    BestFirstSearch(Function<WorkState, Double> f) {
        this.f = f;
    }

    @Override
    public void add(WorkState child, WorkState next) {
        if(child!=null)workQueue.add(new Entry(child, f.apply(child)));
        this.next = next;
        if(next!=null)workQueue.add(new Entry(next, f.apply(next)));
    }

    @Override
    public void add(WorkState child) {
        if(child!=null)workQueue.add(new Entry(child, f.apply(child)));
    }

    @Override
    public WorkState poll() {
        if(this.next!=null){
            WorkState out = next;
            this.next = null;
            return out;
        }
        Entry e = workQueue.poll();
        if(e == null){
            return null;
        }
        return e.ws;
    }

    @Override
    public WorkState steal(TraversalSystem system) throws InterruptedException {
        if(system instanceof BestFirstSearch) {
            Entry e = ((BestFirstSearch) system).workQueue.poll(100, TimeUnit.MILLISECONDS);
            if (e == null) {
                return null;
            }
            return e.ws;
        }
        return null;
    }
}
