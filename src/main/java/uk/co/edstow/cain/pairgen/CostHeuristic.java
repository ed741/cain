package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.Transformation;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;

public interface CostHeuristic<G extends Goal<G>, T extends Transformation> {
    double getCost(GoalPair<G, T> pair, GoalBag<G> goals, Context<G, T> context);


}
