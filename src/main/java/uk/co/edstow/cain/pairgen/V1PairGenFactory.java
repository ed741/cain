package uk.co.edstow.cain.pairgen;


import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;
import uk.co.edstow.cain.structures.Atom;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.ReverseSearch;
import uk.co.edstow.cain.Transformation;

import java.util.*;
import java.util.stream.Collectors;

public class V1PairGenFactory implements PairGenFactory{


    @Override
    public Collection<Tuple<List<GoalPair>, Goal>> applyAllUnaryOpForwards(List<Goal> initialGoals, int depth, Goal goal) {
        return SimplePairGenFactory.applyAllUnaryOps(initialGoals.get(0), goal);
    }

    private Goal.Bounds bounds;
    @Override
    public void init(ReverseSearch rs) {
        bounds = new Goal.Bounds(rs.getFinalGoals());

    }

    @Override
    public PairGen generatePairs(GoalBag goals, int depth) {
        List<GoalPair> pairList = new ArrayList<>();
        for(Goal upper: goals) {
            pairList.addAll(getAddTransformations(upper));
            pairList.addAll(getUnaryTransformations(upper));
        }

        List<Tuple<GoalPair, Double>> list = new ArrayList<>(pairList.size());
        for (GoalPair pair : pairList) {
            if (check(pair)) {
                double v = getValue(goals, pair, bounds);
                list.add(new Tuple<>(pair, v));
            }
        }
        list.sort(Comparator.comparingDouble(Tuple::getB));
        List<GoalPair> out = list.stream().map(Tuple::getA).collect(Collectors.toList());
        return new V1PairGen(out);
    }

    public static double getValue(GoalBag goals, GoalPair pair, Goal.Bounds bounds) {
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


    private List<GoalPair> getUnaryTransformations(Goal upper) {
        List<GoalPair> pairs = new ArrayList<>();
        Collection<Tuple<? extends Transformation, Goal>> ts = SimpleTransformation.applyAllUnaryOpBackwards(upper);
        for (Tuple<? extends Transformation, Goal> t : ts) {
            pairs.add(new GoalPair(upper, t.getB(), t.getA()));
        }
        return pairs;
    }

    private List<GoalPair> getAddTransformations(Goal upper) {
        List<GoalPair> pairs = new ArrayList<>();
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
            pairs.add(new GoalPair(upper, lowers, add));
        }

        return pairs;
    }

    private static List<Goal> patternRepeated(Goal.Bounds bounds, Collection<Goal> goals, Goal pattern){
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

    private boolean check(GoalPair p){
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
        final List<GoalPair> pairs;
        int i;

        V1PairGen(List<GoalPair> pairs) {
            this.pairs = pairs;
            i = 0;
        }

        @Override
        public GoalPair next() {
            if (i < pairs.size()){
                return pairs.get(i++);
            }
            return null;
        }

        @Override
        public int getNumber() {
            return i;
        }


    }
}
