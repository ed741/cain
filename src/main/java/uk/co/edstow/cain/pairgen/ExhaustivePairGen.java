package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Stream;

public abstract class ExhaustivePairGen<G extends Goal<G>, T extends Transformation<R>, R extends Register> implements PairGenFactory.PairGen<G,T,R> {
    protected final Iterator<GoalPair<G, T, R>> it;
    private int count = 0;
    protected final Context<G,T,R> context;
    protected final GoalBag<G> goals;

    public ExhaustivePairGen(GoalBag<G> goals, Context<G,T,R> context, CostHeuristic<G,T,R> heuristic) {
        this.goals = goals;
        this.context = context;
        Comparator<Tuple<GoalPair<G, T, R>, Double>> comparator = Comparator.comparingDouble(Tuple::getB);
        Stream<GoalPair<G, T, R>> stream = goals.asList().parallelStream()
                .flatMap((G upper) ->
                        Stream.concat(
                                getNaryOpStream(upper),
                                getUnaryOpStream(upper)
                        )
                );
        if (heuristic != null) {
            this.it = stream.map(pair -> new Tuple<>(pair, heuristic.getCost(pair, goals, context))).filter(t -> t.getB() >= 0)
                    .sorted(comparator)
                    .map(Tuple::getA)
                    .iterator();
        } else {
            this.it = stream.iterator();
        }
    }


    @Override
    public GoalPair<G, T, R> next() {
        count++;
        return it.hasNext()?it.next():null;
    }

    @Override
    public int getNumber() {
        return count;
    }


    protected abstract Stream<GoalPair<G,T,R>> getUnaryOpStream(G upper);

    protected abstract Stream<GoalPair<G,T,R>> getNaryOpStream(G upper);
}