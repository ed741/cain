package uk.co.edstow.cain.traversal;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class RT<T> implements TraversalSystem<T> {
    public static <E> Supplier<RT<E>> RTFactory(){return RT::new;}

    private final List<T> workQueue = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Random random = new Random();
    @Override
    public void add(T child, T next) {
        try {
            lock.lock();
            if (next != null) workQueue.add(next);
            if (child != null) workQueue.add(child);
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void add(T child) {
        try {
            lock.lock();
            if (child != null) workQueue.add(child);
        }finally {
            lock.unlock();
        }
    }

    @Override
    public T poll() {
        try {
            lock.lock();
            int s = workQueue.size();
            if(s==0){
                return null;
            }
            return workQueue.get(random.nextInt(s));
        }finally {
            lock.unlock();
        }
    }

    @Override
    public T steal(TraversalSystem<T> system) throws InterruptedException {
        if(system instanceof RT) {
            RT<T> other = (RT<T>) system;
            try {
                if(other.lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                    int s = other.workQueue.size();
                    if(s==0){
                        return null;
                    }
                    return other.workQueue.get(random.nextInt(s));
                } else {
                    return null;
                }
            }finally {
                if(other.lock.isHeldByCurrentThread()) other.lock.unlock();
            }
        }
        return null;
    }
}
