package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.structures.Atom;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;

public class SortPairGenFactory extends SimplePairGenFactory {

    @Override
    public PairGen generatePairs(GoalBag goals) {
        return new AddSortPairGen(goals);
    }

    private class AddSortPairGen extends SimplePairGen {
        private final HashSet<Goal> goals;
        AddSortPairGen(GoalBag goals) {
            super(goals);
            this.goals = new HashSet<>(goals);
        }

        @Override
        void putTransformations(Goal upper) {
            List<GoalPair> pairs = getAddTransformations(upper);
            pairs.addAll(getUnaryTransformations(upper));

            List<Tuple<GoalPair, Double>> list = new ArrayList<>(pairs.size());
            for (GoalPair pair: pairs){
                HashSet<Goal> goalSet = new HashSet<>(this.goals);
                goalSet.removeAll(pair.getUppers());
                goalSet.addAll(pair.getLowers());
                double v = 0;
                for (Goal g: goalSet){
                    int i = 0;
                    for (Atom a: g){
                        i += a.x + a.y + a.z;
                    }
                    v += i;
                    v += Math.pow(g.atomCount(), 2);
                }
                list.add(new Tuple<>(pair, v));
            }
            list.sort(Comparator.comparingDouble(Tuple::getB));
            appendCurrentList(list.stream().map(Tuple::getA).collect(Collectors.toList()));

        }
    }
}
