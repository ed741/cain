package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.RegisterAllocator;
import uk.co.edstow.cain.ReverseSearch;
import uk.co.edstow.cain.pairgen.ConfigGetter;
import uk.co.edstow.cain.pairgen.CostHuristic;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;

import java.util.List;
import java.util.function.Consumer;

public class ThresholdConfigGetter implements ConfigGetter<Scamp5Config> {
    private final ReverseSearch rs;
    private final RegisterAllocator.Register[] availableRegisters;
    private final CostHuristic<Scamp5Config> heuristic;
    private final Consumer<Scamp5Config> configConsumer;
    private final int threshold;

    public ThresholdConfigGetter(ReverseSearch rs, RegisterAllocator.Register[] availableRegisters) {
        this(rs, availableRegisters, 10, c -> c.useAll().useSubPowerOf2());
    }

    public ThresholdConfigGetter(ReverseSearch rs, RegisterAllocator.Register[] availableRegisters, int threshold) {
        this(rs, availableRegisters, threshold, c -> c.useAll().useSubPowerOf2());
    }

    public ThresholdConfigGetter(ReverseSearch rs, RegisterAllocator.Register[] availableRegisters, int threshold, Consumer<Scamp5Config> configConsumer) {
        this.rs = rs;
        this.availableRegisters = availableRegisters;
        heuristic = new PatternHuristic<>(rs);
        this.configConsumer = configConsumer;
        this.threshold = threshold ;
    }


    @Override
    public Scamp5Config getConfig(GoalBag goals, int depth) {
        int max = Integer.MIN_VALUE;
        for (Goal goal : goals) {
            max = Math.max(max, goal.atomCount());
        }
        Scamp5Config conf = new Scamp5Config(availableRegisters.length, depth, rs.getInitialGoals());
        configConsumer.accept(conf);
        conf.setStrategy(max>threshold? new Scamp5PairGenFactory.AtomDistanceSortedPairGen<>(goals, conf, heuristic): new Scamp5PairGenFactory.Scamp5ExhaustivePairGen<>(goals, conf, heuristic));
        return conf;
    }

    @Override
    public Scamp5Config getConfigForDirectSolve(List<Goal> goals, int depth) {
        Scamp5Config conf = new Scamp5Config(availableRegisters.length, depth, rs.getInitialGoals()).useAll();
        configConsumer.accept(conf);
        return conf;
    }
}
