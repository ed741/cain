package uk.co.edstow.cain.nonlinear;

import uk.co.edstow.cain.Transformation;
import uk.co.edstow.cain.pairgen.Config;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.util.Tuple;

import java.util.Collection;
import java.util.List;

public interface LinearPairGenFactory<G extends Goal<G>, C extends Config> extends PairGenFactory<G, C>{
    Collection<Tuple<List<G>, Transformation>> generateValueConstantOps(List<G> goal, C config);
}
