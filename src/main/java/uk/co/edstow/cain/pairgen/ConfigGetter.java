package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;

import java.util.List;

public interface ConfigGetter<G extends Goal<G>, T extends Config> {
    T getConfig(GoalBag<G> goals, int depth);
    T getConfigForDirectSolve(GoalBag<G> goals, int depth);
}
