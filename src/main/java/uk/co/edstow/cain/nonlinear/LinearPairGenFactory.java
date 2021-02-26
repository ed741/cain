package uk.co.edstow.cain.nonlinear;

import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.util.Tuple;

import java.util.Collection;
import java.util.List;

public interface LinearPairGenFactory<G extends Goal<G>, T extends Transformation> extends PairGenFactory<G, T>{
    Collection<Tuple<List<G>, T>> generateValueConstantOps(List<G> goal, Context<G, T> context);
}
