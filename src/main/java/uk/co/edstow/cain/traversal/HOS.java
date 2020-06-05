package uk.co.edstow.cain.traversal;

import uk.co.edstow.cain.structures.WorkState;

import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class HOS implements TraversalSystem {
    public static Supplier<HOS> HOSFactory(){return HOS::new;}

    private static class Entry implements Comparable<Entry> {
        final WorkState ws;
        final int f;
        private Entry(WorkState ws, int f) {
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
    private final PriorityBlockingQueue<Entry> workQueue = new PriorityBlockingQueue<>();
    int last = 0;
    @Override
    public void add(WorkState child, WorkState next) {
        if(child!=null)workQueue.add(new Entry(child, 0));
        if(next!=null)workQueue.add(new Entry(next, last+1));
    }

    @Override
    public void add(WorkState child) {
        if(child!=null)workQueue.add(new Entry(child, 0));
    }

    @Override
    public WorkState poll() {
        Entry e = workQueue.poll();
        if(e != null) {
            last = e.f;
            return e.ws;
        } else {
            return null;
        }
    }

    @Override
    public WorkState steal(TraversalSystem system) throws InterruptedException {
        if(system instanceof HOS){
            HOS s = (HOS) system;
            Entry e = s.workQueue.poll(100, TimeUnit.MILLISECONDS);
            if(e == null){
                return null;
            }
            last = e.f;
            return e.ws;
        }
        return null;
    }
}
