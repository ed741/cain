package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.pairgen.Config;
import uk.co.edstow.cain.pairgen.CostHuristic;
import uk.co.edstow.cain.atom.AtomGoal;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.scamp5.analogue.Scamp5PairGenFactory;
import uk.co.edstow.cain.structures.GoalBag;

import java.util.List;

public class ThresholdScamp5ConfigGetter implements Scamp5ConfigGetter<AtomGoal, Scamp5Config<AtomGoal>> {
    private final CostHuristic<AtomGoal> heuristic;
    private final Scamp5Config<AtomGoal> scamp5Config;
    private final Scamp5Config<AtomGoal> scamp5ConfigMovOnly;
    private final int threshold;
    private final List<AtomGoal> initalGoals;


    public ThresholdScamp5ConfigGetter(List<AtomGoal> initialGoals, int threshold, Scamp5Config<AtomGoal> scamp5Config) {
        this.initalGoals = initialGoals;
        heuristic = new PatternHuristic(initialGoals);
        this.scamp5Config = scamp5Config;
        this.scamp5ConfigMovOnly = new Scamp5Config.Builder<>(scamp5Config).setOnlyMov(false).build();
        this.threshold = threshold ;
    }


    @Override
    public PairGenFactory.PairGen<AtomGoal> getScamp5Strategy(GoalBag<AtomGoal> goals, Config<AtomGoal> config, boolean movOnly) {
        int max = Integer.MIN_VALUE;
        for (AtomGoal goal : goals) {
            max = Math.max(max, goal.atomCount());
        }
        PairGenFactory.PairGen<AtomGoal> stratergy;
        if(max>threshold){
            stratergy = new Scamp5PairGenFactory.AtomDistanceSortedPairGen<>(goals, config, movOnly?scamp5ConfigMovOnly:scamp5Config, heuristic);
        } else {
            stratergy = new Scamp5PairGenFactory.Scamp5ExhaustivePairGen<>(goals, config, movOnly?scamp5ConfigMovOnly:scamp5Config, heuristic);
        }
        return stratergy;
    }

    @Override
    public Scamp5Config<AtomGoal> getScamp5ConfigForDirectSolve(GoalBag<AtomGoal> goals, Config<AtomGoal> config) {
        return scamp5Config;
    }
}
