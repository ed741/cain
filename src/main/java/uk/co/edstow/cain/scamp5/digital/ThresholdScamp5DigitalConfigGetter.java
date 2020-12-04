package uk.co.edstow.cain.scamp5.digital;

import uk.co.edstow.cain.atom.AtomGoal;
import uk.co.edstow.cain.pairgen.Config;
import uk.co.edstow.cain.pairgen.CostHuristic;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.scamp5.PatternHuristic;
import uk.co.edstow.cain.scamp5.Scamp5ConfigGetter;
import uk.co.edstow.cain.scamp5.analogue.Scamp5AnaloguePairGenFactory;
import uk.co.edstow.cain.scamp5.analogue.Scamp5AnalougeConfig;
import uk.co.edstow.cain.structures.GoalBag;

import java.util.List;

public class ThresholdScamp5DigitalConfigGetter implements Scamp5ConfigGetter<AtomGoal, Scamp5DigitalConfig<AtomGoal>> {
    private final CostHuristic<AtomGoal> heuristic;
    private final Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig;
    private final Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfigMovOnly;
    private final int threshold;


    public ThresholdScamp5DigitalConfigGetter(List<AtomGoal> initialGoals, int threshold, Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig) {
        heuristic = new PatternHuristic(initialGoals);
        this.scamp5DigitalConfig = scamp5DigitalConfig;
        this.scamp5DigitalConfigMovOnly = new Scamp5DigitalConfig<>(scamp5DigitalConfig, true);
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
            stratergy = new Scamp5DigitalPairGenFactory.AtomDistanceSortedPairGen<>(goals, config, movOnly? scamp5DigitalConfigMovOnly : scamp5DigitalConfig, heuristic);
        } else {
            stratergy = new Scamp5DigitalPairGenFactory.Scamp5ExhaustivePairGen<>(goals, config, movOnly? scamp5DigitalConfigMovOnly : scamp5DigitalConfig, heuristic);
        }
        return stratergy;
    }

    @Override
    public Scamp5DigitalConfig<AtomGoal> getScamp5ConfigForDirectSolve(GoalBag<AtomGoal> goals, Config<AtomGoal> config) {
        return scamp5DigitalConfig;
    }
}
