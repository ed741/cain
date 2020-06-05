package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.ReverseSearch;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface PairGenFactory {
    default List<GoalPair> applyAllUnaryOpForwards(List<Goal> initialGoals, int depth, List<Goal> goals){
        List<GoalPair> allPairs = new ArrayList<>();
        int found = 0;
        for(Goal goal: goals) {
            for (Tuple<List<GoalPair>, Goal> tuple : this.applyAllUnaryOpForwards(initialGoals, depth, goal)) {
                List<GoalPair> pairs = tuple.getA();
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
    Collection<Tuple<List<GoalPair>, Goal>> applyAllUnaryOpForwards(List<Goal> initialGoals, int depth, Goal goal);

    interface PairGen{
        GoalPair next();
        int getNumber();
    }

    void init(ReverseSearch rs);

    PairGen generatePairs(GoalBag goals, int depth);

}
