package uk.co.edstow.cain.traversal;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class BFS<T> implements TraversalSystem<T> {
    public static <E> Supplier<BFS<E>> BFSFactory(){return BFS::new;}

    private final LinkedBlockingDeque<T> workQueue = new LinkedBlockingDeque<>();
    @Override
    public void add(T child, T next) {
        if(next!=null)workQueue.addFirst(next);
        if(child!=null)workQueue.addFirst(child);
    }

    @Override
    public void add(T child) {
        if(child!=null)workQueue.addFirst(child);
    }

    @Override
    public T poll() {
        return workQueue.pollFirst();
    }

    @Override
    public T steal(TraversalSystem<T> system) throws InterruptedException {
        if(system instanceof BFS) {
            return ((BFS<T>) system).workQueue.pollLast(100, TimeUnit.MILLISECONDS);
        }
        return null;
    }
}
