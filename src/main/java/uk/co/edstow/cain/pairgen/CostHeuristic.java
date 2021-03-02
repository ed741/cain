package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;

public interface CostHeuristic<G extends Goal<G>, T extends Transformation<R>, R extends Register> {
    double getCost(GoalPair<G, T, R> pair, GoalBag<G> goals, Context<G,T,R> context);


}
