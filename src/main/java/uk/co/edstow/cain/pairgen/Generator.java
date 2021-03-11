package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.transformations.Transformation;

public class Generator<G extends Goal<G>, T extends Transformation<R>, R extends Register> {
    private final DirectSolver<G,T,R> directSolver;
    private final PairGenFactory<G,T,R> pairGenFactory;

    public Generator(DirectSolver<G, T, R> directSolver, PairGenFactory<G, T, R> pairGenFactory) {
        this.directSolver = directSolver;
        this.pairGenFactory = pairGenFactory;
    }
    public DirectSolver<G, T, R> getDirectSolver() {
        return directSolver;
    }
    public PairGenFactory<G, T, R> getPairGenFactory() {
        return pairGenFactory;
    }
}
