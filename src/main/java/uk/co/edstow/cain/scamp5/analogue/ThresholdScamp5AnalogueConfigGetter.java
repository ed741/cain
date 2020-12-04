package uk.co.edstow.cain.scamp5.analogue;

import uk.co.edstow.cain.pairgen.Config;
import uk.co.edstow.cain.pairgen.CostHuristic;
import uk.co.edstow.cain.atom.AtomGoal;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.scamp5.PatternHuristic;
import uk.co.edstow.cain.scamp5.Scamp5ConfigGetter;
import uk.co.edstow.cain.scamp5.analogue.Scamp5AnaloguePairGenFactory;
import uk.co.edstow.cain.scamp5.analogue.Scamp5AnalougeConfig;
import uk.co.edstow.cain.structures.GoalBag;

import java.util.List;

public class ThresholdScamp5AnalogueConfigGetter implements Scamp5ConfigGetter<AtomGoal, Scamp5AnalougeConfig<AtomGoal>> {
    private final CostHuristic<AtomGoal> heuristic;
    private final Scamp5AnalougeConfig<AtomGoal> scamp5AnalougeConfig;
    private final Scamp5AnalougeConfig<AtomGoal> scamp5AnalougeConfigMovOnly;
    private final int threshold;


    public ThresholdScamp5AnalogueConfigGetter(List<AtomGoal> initialGoals, int threshold, Scamp5AnalougeConfig<AtomGoal> scamp5AnalougeConfig) {
        heuristic = new PatternHuristic(initialGoals);
        this.scamp5AnalougeConfig = scamp5AnalougeConfig;
        this.scamp5AnalougeConfigMovOnly = new Scamp5AnalougeConfig.Builder<>(scamp5AnalougeConfig).setOnlyMov().build();
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
            stratergy = new Scamp5AnaloguePairGenFactory.AtomDistanceSortedPairGen<>(goals, config, movOnly? scamp5AnalougeConfigMovOnly : scamp5AnalougeConfig, heuristic);
        } else {
            stratergy = new Scamp5AnaloguePairGenFactory.Scamp5ExhaustivePairGen<>(goals, config, movOnly? scamp5AnalougeConfigMovOnly : scamp5AnalougeConfig, heuristic);
        }
        return stratergy;
    }

    @Override
    public Scamp5AnalougeConfig<AtomGoal> getScamp5ConfigForDirectSolve(GoalBag<AtomGoal> goals, Config<AtomGoal> config) {
        return scamp5AnalougeConfig;
    }
}
