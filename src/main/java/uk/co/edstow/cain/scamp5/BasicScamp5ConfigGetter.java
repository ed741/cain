package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.regAlloc.RegisterAllocator;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;

public class BasicScamp5ConfigGetter<G extends Goal<G>, T extends Transformation<R>, R extends RegisterAllocator.Register, SELF extends Scamp5ConfigGetter.Scamp5Config<G, SELF>> implements Scamp5ConfigGetter<G, T, R, SELF> {
    private final SELF scamp5Config;
    private final SELF scamp5ConfigMovOnly;
    private final GenGetter<G, T, R, SELF> genGetter;


    public BasicScamp5ConfigGetter(SELF scamp5Config, GenGetter<G, T, R, SELF> genGetter) {
        this.scamp5Config = scamp5Config;
        this.scamp5ConfigMovOnly = scamp5Config.getMovOnlyVersion();
        this.genGetter = genGetter;
    }


    @Override
    public PairGenFactory.PairGen<G, T> getScamp5Strategy(GoalBag<G> goals, Context<G, T> context, boolean movOnly) {
        return genGetter.get(goals, context, movOnly? scamp5ConfigMovOnly : scamp5Config);
    }

    @Override
    public SELF getScamp5ConfigForDirectSolve(GoalBag<G> goals, Context<G, T> context) {
        return scamp5Config;
    }

    @FunctionalInterface
    public interface GenGetter<G extends Goal<G>, T extends Transformation<R>, R extends RegisterAllocator.Register, C extends Scamp5Config<G, C>>{
        PairGenFactory.PairGen<G, T> get(GoalBag<G> goals, Context<G, T> conf, C scamp5Config);
    }
}