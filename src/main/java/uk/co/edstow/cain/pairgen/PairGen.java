package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.transformations.Transformation;

public interface PairGen<G extends Goal<G>, T extends Transformation<R>, R extends Register> {
    GoalPair<G, T, R> next();

    int getNumber();
}
