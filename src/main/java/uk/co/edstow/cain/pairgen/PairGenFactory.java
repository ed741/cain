package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@FunctionalInterface
public interface PairGenFactory<G extends Goal<G>, T extends Transformation<R>, R extends Register> {

    PairGen<G,T,R> generatePairs(GoalBag<G> goals, Context<G,T,R> context);

}
