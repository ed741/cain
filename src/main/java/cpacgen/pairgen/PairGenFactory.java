package cpacgen.pairgen;

import cpacgen.Goal;
import cpacgen.ReverseSplit;
import cpacgen.Transformation;
import cpacgen.util.Tuple;

import java.util.Collection;
import java.util.Iterator;

public interface PairGenFactory {
    Collection<Tuple<? extends Transformation, Goal>> applyAllUnaryOpForwards(Goal initialGoal, int depth);

    interface PairGen{
        Goal.Pair next();
    }

    void init(ReverseSplit rs);

    PairGen generatePairs(Goal.Bag goals, int depth);

}
