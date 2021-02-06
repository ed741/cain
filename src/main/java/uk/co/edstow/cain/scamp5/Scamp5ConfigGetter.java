package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;

public interface Scamp5ConfigGetter<G extends Goal<G>, T extends Scamp5ConfigGetter.Scamp5Config<G,T>> {
    PairGenFactory.PairGen<G> getScamp5Strategy(GoalBag<G> goals, Context<G> context, boolean movOnly);
    default PairGenFactory.PairGen<G> getScamp5Strategy(GoalBag<G> goals, Context<G> context){
        return getScamp5Strategy(goals, context, false);
    }
    T getScamp5ConfigForDirectSolve(GoalBag<G> goals, Context<G> context);

    interface Scamp5Config<G extends Goal<G>, SELF extends Scamp5Config<G, SELF>> {
        boolean onlyMov();
        SELF getMovOnlyVersion();
    }
}
