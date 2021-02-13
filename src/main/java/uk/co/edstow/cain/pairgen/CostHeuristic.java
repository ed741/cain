package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;

public interface CostHeuristic<G extends Goal<G>> {
    double getCost(GoalPair<G> pair, GoalBag<G> goals, Context<G> context);


}
