package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;

import java.util.List;

public class ThresholdScamp5ConfigGetter<G extends Goal<G>, C extends Scamp5ConfigGetter.Scamp5Config<G, C>> implements Scamp5ConfigGetter<G, C> {
    private final CostHeuristic<G> heuristic;
    private final C scamp5Config;
    private final C scamp5ConfigMovOnly;
    private final int threshold;
    private final GenGetter<G,C> above;
    private final GenGetter<G,C> below;


    public ThresholdScamp5ConfigGetter(List<G> initialGoals, int threshold, CostHeuristic<G> heuristic, C scamp5Config, GenGetter<G, C> above, GenGetter<G, C> below) {
        this.heuristic = heuristic;
        this.scamp5Config = scamp5Config;
        this.scamp5ConfigMovOnly = scamp5Config.getMovOnlyVersion();
        this.threshold = threshold ;
        this.above = above;
        this.below = below;
    }


    @Override
    public PairGenFactory.PairGen<G> getScamp5Strategy(GoalBag<G> goals, Context<G> context, boolean movOnly) {
        int max = Integer.MIN_VALUE;
        for (G goal : goals) {
            max = (int) Math.max(max, goal.total());
        }
        PairGenFactory.PairGen<G> stratergy;
        if(max>threshold){
            stratergy = above.get(goals, context, movOnly? scamp5ConfigMovOnly : scamp5Config, heuristic);
        } else {
            stratergy = below.get(goals, context, movOnly? scamp5ConfigMovOnly : scamp5Config, heuristic);
        }
        return stratergy;
    }

    @Override
    public C getScamp5ConfigForDirectSolve(GoalBag<G> goals, Context<G> context) {
        return scamp5Config;
    }

    @FunctionalInterface
    public interface GenGetter<G extends Goal<G>, C extends Scamp5ConfigGetter.Scamp5Config<G, C>>{
        PairGenFactory.PairGen<G> get(GoalBag<G> goals, Context<G> conf, C scamp5Config, CostHeuristic<G> heuristic);
    }
}