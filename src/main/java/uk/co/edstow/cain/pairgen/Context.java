package uk.co.edstow.cain.pairgen;


import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.regAlloc.RegisterAllocator;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.structures.Goal;

import java.util.List;

public class Context<G extends Goal<G>, T extends Transformation<R>, R extends Register> {


    public final int searchDepth;
    public final RegisterAllocator<G,T,R> registerAllocator;
    public final List<G> initialGoals;

    public Context(int searchDepth, RegisterAllocator<G,T,R> registerAllocator, List<G> initialGoals) {
        this.searchDepth = searchDepth;
        this.registerAllocator = registerAllocator;
        this.initialGoals = initialGoals;
    }
}
