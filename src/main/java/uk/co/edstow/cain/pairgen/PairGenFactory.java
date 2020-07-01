package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.ReverseSearch;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface PairGenFactory<G extends Goal<G>> {
    default List<GoalPair<G>> applyAllUnaryOpForwards(List<G> initialGoals, int depth, GoalBag<G> goals){
        List<GoalPair<G>> allPairs = new ArrayList<>();
        int found = 0;
        for(G goal: goals) {
            for (Tuple<List<GoalPair<G>>, G> tuple : this.applyAllUnaryOpForwards(initialGoals, depth, goal)) {
                List<GoalPair<G>> pairs = tuple.getA();
                G g = tuple.getB();
                if (initialGoals.contains(g)) {
                    allPairs.addAll(pairs);
                    found++;
                    break;
                }
            }
        }
        return found!=goals.size()?null:allPairs;
    }
    Collection<Tuple<List<GoalPair<G>>, G>> applyAllUnaryOpForwards(List<G> initialGoals, int depth, G goal);

    interface PairGen<G extends Goal<G>>{
        GoalPair<G> next();
        int getNumber();
    }

    void init(ReverseSearch<G> rs);

    PairGen<G> generatePairs(GoalBag<G> goals, int depth);

}
