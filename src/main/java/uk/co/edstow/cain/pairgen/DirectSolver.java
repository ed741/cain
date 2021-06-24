package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class DirectSolver<G extends Goal<G>, T extends Transformation<R>, R extends Register> {

    public List<GoalPair<G, T, R>> solveDirectly(Context<G, T, R> context, GoalBag<G> goals){
        boolean[] initialGoalUsed = new boolean[context.initialGoals.size()];
        List<GoalPair<G, T, R>> allPairs = new ArrayList<>();
        int found = 0;
        for(G goal: goals) {
            for (Tuple<List<GoalPair<G, T, R>>, G> tuple : this.solveDirectly(context, goal)) {
                List<GoalPair<G, T, R>> pairs = tuple.getA();
                G g = tuple.getB();
                int idx = context.initialGoals.indexOf(g);
                if (idx >= 0 && !initialGoalUsed[idx]) {
                    initialGoalUsed[idx] = true;
                    allPairs.addAll(pairs);
                    found++;
                    break;
                }
            }
        }
        return found!=goals.size()?null:allPairs;
    }
    protected abstract Collection<Tuple<List<GoalPair<G, T, R>>, G>> solveDirectly(Context<G,T,R> context, G goal);
    public abstract T getDummyTransformation(List<G> upperGoals, List<G> lowerGoals, Context<G,T,R> context);

}
