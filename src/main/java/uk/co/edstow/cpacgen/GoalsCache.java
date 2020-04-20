package uk.co.edstow.cpacgen;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.StampedLock;

public class GoalsCache {
    private Map<Goal.Bag, Double> costMap;
    private ReentrantLock mapLock;
    private StampedLock statsLock;
    private long hits = 0;
    private long checks = 0;

    public GoalsCache() {
        this.costMap = new HashMap<>();
        this.mapLock = new ReentrantLock();
    }

    public synchronized boolean isBest(Goal.Bag b, double d) {
        Goal.Bag sortedB = new Goal.Bag(b, true);
        try {
            mapLock.lock();
            checks++;
            Double cost = costMap.get(sortedB);
            boolean best = cost == null || d < cost;
            if (best) {
                costMap.put(sortedB, d);
            } else {
                hits++;
            }
            return best;
        } finally {
            mapLock.unlock();
        }
    }

    @Override
    public String toString() {
        try {
            mapLock.lock();
            return "GoalsCache: {Size: " + costMap.size() + ", Hits: " + hits + ", Checks: " + checks + "}";
        } finally {
            mapLock.unlock();
        }
    }
}
