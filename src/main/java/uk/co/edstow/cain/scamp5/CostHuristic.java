package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.ReverseSearch;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;

public interface CostHuristic<T extends Config> {
    double getCost(GoalPair pair, GoalBag goals, T config);


}
