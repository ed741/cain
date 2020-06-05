package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.ReverseSearch;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;

import java.util.List;

public interface ConfigGetter<T extends Config> {
    T getConfig(GoalBag goals, int depth);
    T getConfigForDirectSolve(List<Goal> goals, int depth);
}
