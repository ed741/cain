package uk.co.edstow.cpacgen.pairgen;

import uk.co.edstow.cpacgen.Atom;
import uk.co.edstow.cpacgen.Goal;
import uk.co.edstow.cpacgen.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;

public class SortPairGenFactory extends SimplePairGenFactory {

    @Override
    public PairGen generatePairs(Goal.Bag goals) {
        return new AddSortPairGen(goals);
    }

    private class AddSortPairGen extends SimplePairGen {
        private HashSet<Goal> goals;
        public AddSortPairGen(Goal.Bag goals) {
            super(goals);
            this.goals = new HashSet<>(goals);
        }

        @Override
        void putTransformations(Goal upper) {
            List<Goal.Pair> pairs = getAddTransformations(upper);
            pairs.addAll(getUnaryTransformations(upper));

            List<Tuple<Goal.Pair, Double>> list = new ArrayList<>(pairs.size());
            for (Goal.Pair pair: pairs){
                HashSet<Goal> goalSet = new HashSet<>(this.goals);
                goalSet.remove(pair.getUpper());
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
