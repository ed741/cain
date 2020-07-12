package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Stream;

public abstract class ExhaustivePairGen<G extends Goal<G>, T extends Config> implements PairGenFactory.PairGen<G> {
    private final Iterator<GoalPair<G>> it;
    private int count = 0;
    protected final T conf;
    protected final GoalBag<G> goals;

    public ExhaustivePairGen(GoalBag<G> goals, T conf, CostHuristic<G, T> huristic) {
        this.goals = goals;
        this.conf = conf;
        Comparator<Tuple<GoalPair<G>, Double>> comparator = Comparator.comparingDouble(Tuple::getB);
        Stream<GoalPair<G>> stream = goals.asList().parallelStream()
                .flatMap((G upper) ->
                        Stream.concat(
                                getNaryOpStream(upper),
                                getUnaryOpStream(upper)
                        )
                );
        if (huristic != null) {
            this.it = stream.map(pair -> new Tuple<>(pair, huristic.getCost(pair, goals, conf))).filter(t -> t.getB() >= 0)
                    .sorted(comparator)
                    .map(Tuple::getA)
                    .iterator();
        } else {
            this.it = stream.iterator();
        }
    }

    @Override
    public GoalPair<G> next() {
        count++;
        return it.hasNext()?it.next():null;
    }

    @Override
    public int getNumber() {
        return count;
    }


    protected abstract Stream<GoalPair<G>> getUnaryOpStream(G upper);

    protected abstract Stream<GoalPair<G>> getNaryOpStream(G upper);
}