package uk.co.edstow.cain.traversal;

public interface TraversalSystem<T> {

    void add(T child, T next);
    void add(T child);
    T poll();
    T steal(TraversalSystem<T> system) throws InterruptedException;

}
