package uk.co.edstow.cain.traversal;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CGDS<T> implements TraversalSystem<T> {
    public static <E> Supplier<CGDS<E>> CGDSFactory(){return CGDS::new;}

    private final LinkedBlockingDeque<T> workQueue = new LinkedBlockingDeque<>();
    @Override
    public void add(T child, T next) {
        if(child!=null)workQueue.addFirst(child);
        if(next!=null)workQueue.addLast(next);
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
        if(system instanceof CGDS) {
            return ((CGDS<T>) system).workQueue.pollLast(100, TimeUnit.MILLISECONDS);
        }
        return null;
    }
}
