package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;

public class BasicScamp5ConfigGetter<G extends Goal<G>, C extends Scamp5ConfigGetter.Scamp5Config<G, C>> implements Scamp5ConfigGetter<G, C> {
    private final C scamp5Config;
    private final C scamp5ConfigMovOnly;
    private final GenGetter<G,C> genGetter;


    public BasicScamp5ConfigGetter(C scamp5Config, GenGetter<G, C> genGetter) {
        this.scamp5Config = scamp5Config;
        this.scamp5ConfigMovOnly = scamp5Config.getMovOnlyVersion();
        this.genGetter = genGetter;
    }


    @Override
    public PairGenFactory.PairGen<G> getScamp5Strategy(GoalBag<G> goals, Context<G> context, boolean movOnly) {
        return genGetter.get(goals, context, movOnly? scamp5ConfigMovOnly : scamp5Config);
    }

    @Override
    public C getScamp5ConfigForDirectSolve(GoalBag<G> goals, Context<G> context) {
        return scamp5Config;
    }

    @FunctionalInterface
    public interface GenGetter<G extends Goal<G>, C extends Scamp5Config<G, C>>{
        PairGenFactory.PairGen<G> get(GoalBag<G> goals, Context<G> conf, C scamp5Config);
    }
}