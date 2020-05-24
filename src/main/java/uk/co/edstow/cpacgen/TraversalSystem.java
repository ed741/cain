package uk.co.edstow.cpacgen;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static uk.co.edstow.cpacgen.ReverseSearch.*;

public abstract class TraversalSystem {

    public abstract void add(WorkState child, WorkState next);
    public abstract void add(WorkState child);
    public abstract WorkState poll();
    public abstract WorkState steal(TraversalSystem system) throws InterruptedException;


    public static Supplier<DFS> DFSFactory(){return DFS::new;}
    private static class DFS extends TraversalSystem {
        private final LinkedBlockingDeque<WorkState> workQueue = new LinkedBlockingDeque<>();
        @Override
        public void add(WorkState child, WorkState next) {
            if(next!=null)workQueue.addFirst(next);
            if(child!=null)workQueue.addFirst(child);
        }

        @Override
        public void add(WorkState child) {
            if(child!=null)workQueue.addFirst(child);
        }

        @Override
        public WorkState poll() {
            return workQueue.pollFirst();
        }

        @Override
        public WorkState steal(TraversalSystem system) throws InterruptedException {
            if(system instanceof DFS) {
                return ((DFS) system).workQueue.pollLast(100, TimeUnit.MILLISECONDS);
            }
            return null;
        }
    }

    public static Supplier<BFS>BFSFactory(){return BFS::new;}
    private static class BFS extends TraversalSystem {
        private final LinkedBlockingDeque<WorkState> workQueue = new LinkedBlockingDeque<>();
        @Override
        public void add(WorkState child, WorkState next) {
            if(next!=null)workQueue.addFirst(next);
            if(child!=null)workQueue.addFirst(child);
        }

        @Override
        public void add(WorkState child) {
            if(child!=null)workQueue.addFirst(child);
        }

        @Override
        public WorkState poll() {
            return workQueue.pollFirst();
        }

        @Override
        public WorkState steal(TraversalSystem system) throws InterruptedException {
            if(system instanceof BFS) {
                return ((BFS) system).workQueue.pollLast(100, TimeUnit.MILLISECONDS);
            }
            return null;
        }
    }

    public static Supplier<SOT> SOTFactory(){return SOT::new;}
    private static class SOT extends TraversalSystem {
        private final LinkedBlockingDeque<WorkState> workQueue = new LinkedBlockingDeque<>();
        @Override
        public void add(WorkState child, WorkState next) {
            if(child!=null)workQueue.addFirst(child);
            if(next!=null)workQueue.addLast(next);
        }

        @Override
        public void add(WorkState child) {
            if(child!=null)workQueue.addFirst(child);
        }

        @Override
        public WorkState poll() {
            return workQueue.pollFirst();
        }

        @Override
        public WorkState steal(TraversalSystem system) throws InterruptedException {
            if(system instanceof SOT) {
                return ((SOT) system).workQueue.pollLast(100, TimeUnit.MILLISECONDS);
            }
            return null;
        }
    }


    public static Supplier<HOS> HOSFactory(){return HOS::new;}
    private static class HOS extends TraversalSystem {
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

    public static Supplier<AStar> AStarFactory(Function<WorkState, Double> f){return () -> new AStar(f);}
    public static class AStar extends TraversalSystem {
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

        AStar(Function<WorkState, Double> f) {
            this.f = f;
        }

        @Override
        public void add(WorkState child, WorkState next) {
            if(child!=null)workQueue.add(new Entry(child, f.apply(child)));
            if(next!=null)workQueue.add(new Entry(next, f.apply(next)));
        }

        @Override
        public void add(WorkState child) {
            if(child!=null)workQueue.add(new Entry(child, f.apply(child)));
        }

        @Override
        public WorkState poll() {
            Entry e = workQueue.poll();
            if(e == null){
                return null;
            }
            return e.ws;
        }

        @Override
        public WorkState steal(TraversalSystem system) throws InterruptedException {
            if(system instanceof AStar) {
                Entry e = ((AStar) system).workQueue.poll(100, TimeUnit.MILLISECONDS);
                if (e == null) {
                    return null;
                }
                return e.ws;
            }
            return null;
        }
    }
}
