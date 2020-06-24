package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.structures.Goal;

import java.util.List;

public class Config {
    public final int availableRegisters;
    public final int depth;
    public final List<Goal> initialGoals;

    public Config(int availableRegisters, int depth, List<Goal> initialGoals) {
        this.availableRegisters = availableRegisters;
        this.depth = depth;
        this.initialGoals = initialGoals;
    }
}
