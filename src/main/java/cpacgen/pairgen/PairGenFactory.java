package cpacgen.pairgen;

import cpacgen.Goal;
import cpacgen.ReverseSplit;

import java.util.Iterator;

public interface PairGenFactory {
    interface PairGen{
        Goal.Pair next();
    }

    void init(ReverseSplit rs);
    PairGen generatePairs(Goal.Bag goals);

}
