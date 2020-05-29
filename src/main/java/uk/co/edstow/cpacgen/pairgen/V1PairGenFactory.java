package uk.co.edstow.cpacgen.pairgen;


import uk.co.edstow.cpacgen.util.Bounds;
import uk.co.edstow.cpacgen.util.Tuple;
import uk.co.edstow.cpacgen.Atom;
import uk.co.edstow.cpacgen.Goal;
import uk.co.edstow.cpacgen.ReverseSearch;
import uk.co.edstow.cpacgen.Transformation;

import java.util.*;
import java.util.stream.Collectors;

public class V1PairGenFactory implements PairGenFactory{


    @Override
    public Collection<Tuple<List<Goal.Pair>, Goal>> applyAllUnaryOpForwards(List<Goal> initialGoals, int depth, Goal goal) {
        return SimplePairGenFactory.applyAllUnaryOps(initialGoals.get(0), goal);
    }

    private Bounds bounds;
    @Override
    public void init(ReverseSearch rs) {
        bounds = new Bounds(rs.getFinalGoals());

    }

    @Override
    public PairGen generatePairs(Goal.Bag goals, int depth) {
        List<Goal.Pair> pairList = new ArrayList<>();
        for(Goal upper: goals) {
            pairList.addAll(getAddTransformations(upper));
            pairList.addAll(getUnaryTransformations(upper));
        }

        List<Tuple<Goal.Pair, Double>> list = new ArrayList<>(pairList.size());
        for (Goal.Pair pair : pairList) {
            if (check(pair)) {
                double v = getValue(goals, pair, bounds);
                list.add(new Tuple<>(pair, v));
            }
        }
        list.sort(Comparator.comparingDouble(Tuple::getB));
        List<Goal.Pair> out = list.stream().map(Tuple::getA).collect(Collectors.toList());
        return new V1PairGen(out);
    }

    public static double getValue(Goal.Bag goals, Goal.Pair pair, Bounds bounds) {
        HashSet<Goal> goalSet = new HashSet<>(goals);
        goalSet.removeAll(pair.getUppers());

        goalSet.addAll(pair.getLowers());
        double v = 0;
        for (Goal g : goalSet) {
            int i = 0;
            for (Atom a : g) {
                i += Math.abs(a.x) + Math.abs(a.y) + Math.abs(a.z);
            }
            v += i;
        }
        List<Goal> goalList = new ArrayList<>(goalSet);
        for (int i = 0; i < goalList.size(); i++) {
            Goal g = goalList.get(i);
            List<Goal> toRemove = patternRepeated(bounds, goalList, g);
            goalList.removeAll(toRemove);
            goalList.add(i, g);
        }
        for (Goal g: goalList){
            v += Math.pow(g.atomCount(), 2);
        }
        return v;
    }


    private List<Goal.Pair> getUnaryTransformations(Goal upper) {
        List<Goal.Pair> pairs = new ArrayList<>();
        Collection<Tuple<? extends Transformation, Goal>> ts = SimpleTransformation.applyAllUnaryOpBackwards(upper);
        for (Tuple<? extends Transformation, Goal> t : ts) {
            pairs.add(new Goal.Pair(upper, t.getB(), t.getA()));
        }
        return pairs;
    }

    private List<Goal.Pair> getAddTransformations(Goal upper) {
        List<Goal.Pair> pairs = new ArrayList<>();
        // Addition
        Collection<Goal> splits = upper.allSplitsRecursive();
        Set<Goal> seen = new HashSet<>();
        for (Goal a : splits) {
            if (seen.contains(a)){
                continue;
            }
            Goal b = upper.without(a);
            if (seen.contains(b)){
                continue;
            }
            seen.add(a);
            seen.add(b);
            if (b.isEmpty()) {
                continue;
            }

            SimpleTransformation.Add add = new SimpleTransformation.Add(a, b);
            if (!add.applyForwards().same(upper)) throw new AssertionError();
            List<Goal> lowers = new ArrayList<>();
            lowers.add(a);
            lowers.add(b);
            pairs.add(new Goal.Pair(upper, lowers, add));
        }

        return pairs;
    }

    private static List<Goal> patternRepeated(Bounds bounds, Collection<Goal> goals, Goal pattern){
        List<Goal> matches = new ArrayList<>();
        int bx = bounds.xMax-bounds.xMin;
        int by = bounds.yMax-bounds.yMin;
        for (int i = -bx; i <= bx; i++) {
            for (int j = -by; j <= by; j++) {
                Goal tmp = moveGoal(pattern, i, j, 0);
                if (goals.contains(tmp)){
                    matches.add(tmp);
                }

            }
        }
        return matches;
    }

    @SuppressWarnings("SameParameterValue")
    private static Goal moveGoal(Goal goal, int x, int y, int z){
        Goal.Factory factory = new Goal.Factory();
        for (Atom a: goal){
            factory.add(a.moved(x, y, z));
        }
        return factory.get();
    }

    private boolean check(Goal.Pair p){
        for (Goal l: p.getLowers()) {
            for (Atom a : l) {
                if (bounds.excludes(a)) {
                    return false;
                }
            }
        }
        return true;

    }

    private class V1PairGen implements PairGen {
        final List<Goal.Pair> pairs;
        int i;

        V1PairGen(List<Goal.Pair> pairs) {
            this.pairs = pairs;
            i = 0;
        }

        @Override
        public Goal.Pair next() {
            if (i < pairs.size()){
                return pairs.get(i++);
            }
            return null;
        }
    }
}
