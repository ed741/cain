package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.Transformation;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.structures.Plan;
import uk.co.edstow.cain.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface PairGenFactory<G extends Goal<G>, T extends Transformation> {
    default List<GoalPair<G, T>> applyAllUnaryOpForwards(List<G> initialGoals, Context<G, T> context, GoalBag<G> goals){
        List<GoalPair<G, T>> allPairs = new ArrayList<>();
        int found = 0;
        for(G goal: goals) {
            for (Tuple<List<GoalPair<G, T>>, G> tuple : this.applyAllUnaryOpForwards(initialGoals, context, goal)) {
                List<GoalPair<G, T>> pairs = tuple.getA();
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
    Collection<Tuple<List<GoalPair<G, T>>, G>> applyAllUnaryOpForwards(List<G> initialGoals, Context<G, T> context, G goal);

    interface PairGen<G extends Goal<G>, T extends Transformation>{
        GoalPair<G, T> next();
        int getNumber();
    }

    PairGen<G, T> generatePairs(GoalBag<G> goals, Context<G, T> context);

    T getDummyTransformation(List<G> upperGoals, List<G> lowerGoals, Context<G, T> context);

}
