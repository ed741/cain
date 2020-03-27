package cpacgen;

import java.util.*;

public class GoalsCache {
    private Map<Goal.Bag, Double> costMap;
    private long hits = 0;
    private long checks = 0;

    public GoalsCache() {
        this.costMap = new HashMap<>();
    }

    public synchronized boolean isBest(Goal.Bag b, double d) {
        b.setImmutable();
        checks++;
        Double cost = costMap.get(b);
        boolean best = cost == null || d < cost;
        if (best){
            costMap.put(b, d);
        }
        return best;
    }

    public void put(Goal.Bag b, double cost){
        b.setImmutable();
        costMap.put(b, cost);
    }

    public synchronized void hit(){
        hits++;
    }

    @Override
    public String toString() {
        return "GoalsCache: {Size: "+ costMap.size() + ", Hits: "+ hits + ", Checks: "+ checks + "}";
    }
}
