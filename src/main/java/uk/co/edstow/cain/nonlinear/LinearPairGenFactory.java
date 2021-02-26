package uk.co.edstow.cain.nonlinear;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.util.Tuple;

import java.util.Collection;
import java.util.List;

public interface LinearPairGenFactory<G extends Goal<G>, T extends Transformation<R>, R extends Register> extends PairGenFactory<G, T, R>{
    Collection<Tuple<List<G>, T>> generateValueConstantOps(List<G> goal, Context<G,T,R> context);
}
