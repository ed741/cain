package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.RegisterAllocator;
import uk.co.edstow.cain.ReverseSearch;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;

import java.util.List;
import java.util.function.Consumer;

public class ThresholdConfigGetter implements ConfigGetter<Config> {
    private final ReverseSearch rs;
    private final RegisterAllocator.Register[] availableRegisters;
    private final CostHuristic<Config> heuristic;
    private final Consumer<Config> configConsumer;
    private final int threshold;

    public ThresholdConfigGetter(ReverseSearch rs, RegisterAllocator.Register[] availableRegisters) {
        this(rs, availableRegisters, 10, c -> c.useAll().useSubPowerOf2());
    }

    public ThresholdConfigGetter(ReverseSearch rs, RegisterAllocator.Register[] availableRegisters, int threshold) {
        this(rs, availableRegisters, threshold, c -> c.useAll().useSubPowerOf2());
    }

    public ThresholdConfigGetter(ReverseSearch rs, RegisterAllocator.Register[] availableRegisters, int threshold, Consumer<Config> configConsumer) {
        this.rs = rs;
        this.availableRegisters = availableRegisters;
        heuristic = new PatternHuristic(rs);
        this.configConsumer = configConsumer;
        this.threshold = threshold ;
    }


    @Override
    public Config getConfig(GoalBag goals, int depth) {
        int max = Integer.MIN_VALUE;
        for (Goal goal : goals) {
            max = Math.max(max, goal.atomCount());
        }
        Config conf = new Config(availableRegisters.length, depth, rs.getInitialGoals());
        configConsumer.accept(conf);
        conf.setStrategy(max>threshold? new Scamp5PairGenFactory.AtomDistanceSortedPairGen<>(goals, conf, heuristic): new Scamp5PairGenFactory.ExhaustivePairGen<>(goals, conf, heuristic));
        return conf;
    }

    @Override
    public Config getConfigForDirectSolve(List<Goal> goals, int depth) {
        return new Config(availableRegisters.length, depth, rs.getInitialGoals()).useAll();
    }
}
