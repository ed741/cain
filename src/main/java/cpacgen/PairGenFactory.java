package cpacgen;

import java.util.Iterator;

public interface PairGenFactory {
    static interface PairGen{
        Goal.Pair next();
    }

    void init(ReverseSplit rs);
    PairGen generatePairs(Goal.Bag goals);

}
