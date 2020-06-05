package uk.co.edstow.cain.traversal;

import uk.co.edstow.cain.structures.WorkState;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class DFS implements TraversalSystem {
    public static Supplier<DFS> DFSFactory(){return DFS::new;}

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
