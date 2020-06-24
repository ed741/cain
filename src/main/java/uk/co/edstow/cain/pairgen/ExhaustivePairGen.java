package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Stream;

public abstract class ExhaustivePairGen<T extends Config> implements PairGenFactory.PairGen {
    private final Iterator<GoalPair> it;
    private int count;
    protected final T conf;
    protected final GoalBag goals;

    public ExhaustivePairGen(GoalBag goals, T conf, CostHuristic<T> huristic) {
        this.goals = goals;
        this.conf = conf;
        Comparator<Tuple<GoalPair, Double>> comparator = Comparator.comparingDouble(Tuple::getB);
        this.it = goals.parallelStream()
                .flatMap((Goal upper) ->
                        Stream.concat(
                                getNaryOpStream(upper),
                                getUnaryOpStream(upper)
                        )
                ).map(pair -> new Tuple<>(pair, huristic.getCost(pair, goals, conf))).filter(t -> t.getB()>=0)
                .sorted(comparator)
                .map(Tuple::getA)
                .iterator();
    }

    @Override
    public GoalPair next() {
        count++;
        return it.hasNext()?it.next():null;
    }

    @Override
    public int getNumber() {
        return count;
    }


    protected abstract Stream<GoalPair> getUnaryOpStream(Goal upper);

    protected abstract Stream<GoalPair> getNaryOpStream(Goal upper);
}