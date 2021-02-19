package uk.co.edstow.cain.pairgen;


import uk.co.edstow.cain.regAlloc.RegisterAllocator;
import uk.co.edstow.cain.Transformation;
import uk.co.edstow.cain.structures.Goal;

import java.util.List;

public class Context<G extends Goal<G>, T extends Transformation> {


    public final int searchDepth;
    public final RegisterAllocator<G,T> registerAllocator;
    public final List<G> initialGoals;

    public Context(int searchDepth, RegisterAllocator<G, T> registerAllocator, List<G> initialGoals) {
        this.searchDepth = searchDepth;
        this.registerAllocator = registerAllocator;
        this.initialGoals = initialGoals;
    }
}
