package uk.co.edstow.cain.structures;


import java.util.concurrent.ConcurrentHashMap;


public class GoalsCache<G extends Goal<G>> {
    private final ConcurrentHashMap<GoalBag<G>, Double> costMap;


    public GoalsCache() {
        this.costMap = new ConcurrentHashMap<>();
    }

    public boolean isBest(GoalBag<G> b, double d) {
        GoalBag<G> sortedB = new GoalBag<>(b, true);
        double c = costMap.compute(sortedB, (k, v) -> {
            if(v == null) {
                return d;
            } else if (v < d){
                return v;
            } else if(v == d) {
                return Math.nextDown(v);
            } else {
                return d;
            }
        });
        return c==d;
    }

    @Override
    public String toString() {

            return "GoalsCache: {Size: " + costMap.size() + "}";
    }

    public int size(){
        return costMap.size();
    }
}
