package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.Goal;
import uk.co.edstow.cain.ReverseSearch;
import uk.co.edstow.cain.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface PairGenFactory {
    default List<Goal.Pair> applyAllUnaryOpForwards(List<Goal> initialGoals, int depth, List<Goal> goals){
        List<Goal.Pair> allPairs = new ArrayList<>();
        int found = 0;
        for(Goal goal: goals) {
            for (Tuple<List<Goal.Pair>, Goal> tuple : this.applyAllUnaryOpForwards(initialGoals, depth, goal)) {
                List<Goal.Pair> pairs = tuple.getA();
                Goal g = tuple.getB();
                if (initialGoals.contains(g)) {
                    allPairs.addAll(pairs);
                    found++;
                    break;
                }
            }
        }
        return found!=goals.size()?null:allPairs;
    }
    Collection<Tuple<List<Goal.Pair>, Goal>> applyAllUnaryOpForwards(List<Goal> initialGoals, int depth, Goal goal);

    interface PairGen{
        Goal.Pair next();
    }

    void init(ReverseSearch rs);

    PairGen generatePairs(Goal.Bag goals, int depth);

}
