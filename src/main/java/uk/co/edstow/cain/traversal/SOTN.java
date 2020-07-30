package uk.co.edstow.cain.traversal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SOTN<T> implements TraversalSystem<T> {
    public SOTN(int n) {
        this.n = n;
        this.cn = n;
    }

    public static <E> Supplier<SOTN<E>> SOTNFactory(int n){return ()->new SOTN<>(n);}

    private final LinkedBlockingDeque<T> workQueue = new LinkedBlockingDeque<>();
    private final List<T> wait = new ArrayList<>();
    private T parent;
    private final int n;
    private int cn;
    @Override
    public void add(T child, T next) {
        cn--;
        if(child!=null)wait.add(child);
        if(cn<=0){
            cn = n;
            if(next!=null)workQueue.addLast(next);
            for (int i = wait.size() - 1; i >= 0; i--) {
                workQueue.addFirst(wait.get(i));
            }
            wait.clear();
        } else {
            parent = next;
        }
    }

    @Override
    public void add(T child) {
        if(child!=null)workQueue.addFirst(child);
    }

    @Override
    public T poll() {
        if(parent != null){
            T out = parent;
            parent = null;
            return out;
        }
        cn=n;
        for (int i = wait.size() - 1; i >= 0; i--) {
            workQueue.addFirst(wait.get(i));
        }
        wait.clear();
        return workQueue.pollFirst();
    }

    @Override
    public T steal(TraversalSystem<T> system) throws InterruptedException {
        if(system instanceof SOTN) {
            return ((SOTN<T>) system).workQueue.pollLast(100, TimeUnit.MILLISECONDS);
        }
        return null;
    }
}
