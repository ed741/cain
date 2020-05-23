package uk.co.edstow.cpacgen.pairgen;

import uk.co.edstow.cpacgen.Goal;
import uk.co.edstow.cpacgen.ReverseSearch;
import uk.co.edstow.cpacgen.util.Tuple;

import java.util.Collection;
import java.util.List;

public interface PairGenFactory {
    Collection<Tuple<List<Goal.Pair>, Goal>> applyAllUnaryOpForwards(List<Goal> initialGoals, int depth, Goal goal);

    interface PairGen{
        Goal.Pair next();
    }

    void init(ReverseSearch rs);

    PairGen generatePairs(Goal.Bag goals, int depth);

}
