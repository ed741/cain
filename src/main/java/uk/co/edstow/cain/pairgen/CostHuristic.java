package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;

public interface CostHuristic<G extends Goal<G>, T extends Config> {
    double getCost(GoalPair<G> pair, GoalBag<G> goals, T config);


}
