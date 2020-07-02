package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.structures.Goal;

import java.util.List;

public class Config<G extends Goal<G>> {
    public final int availableRegisters;
    public final int depth;
    public final List<G> initialGoals;

    public Config(int availableRegisters, int depth, List<G> initialGoals) {
        this.availableRegisters = availableRegisters;
        this.depth = depth;
        this.initialGoals = initialGoals;
    }
}
