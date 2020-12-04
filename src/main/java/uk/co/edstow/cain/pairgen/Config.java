package uk.co.edstow.cain.pairgen;


import uk.co.edstow.cain.structures.Goal;

import java.util.List;

public class Config<G extends Goal<G>> {


    public final int searchDepth;
    public final int totalAvailableRegisters;
    public final List<G> initialGoals;

    public Config(int searchDepth, int totalAvailableRegisters, List<G> initialGoals) {
        this.searchDepth = searchDepth;
        this.totalAvailableRegisters = totalAvailableRegisters;
        this.initialGoals = initialGoals;
    }
}
