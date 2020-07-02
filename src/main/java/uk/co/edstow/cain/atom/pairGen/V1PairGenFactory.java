package uk.co.edstow.cain.atom.pairGen;


import uk.co.edstow.cain.atom.Atom;
import uk.co.edstow.cain.atom.AtomGoal;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;
import uk.co.edstow.cain.ReverseSearch;
import uk.co.edstow.cain.Transformation;

import java.util.*;
import java.util.stream.Collectors;

public class V1PairGenFactory implements PairGenFactory<AtomGoal> {


    @Override
    public Collection<Tuple<List<GoalPair<AtomGoal>>, AtomGoal>> applyAllUnaryOpForwards(List<AtomGoal> initialGoals, int depth, AtomGoal goal) {
        return SimplePairGenFactory.applyAllUnaryOps(initialGoals.get(0), goal);
    }

    private AtomGoal.AtomBounds bounds;
    @Override
    public void init(ReverseSearch<AtomGoal> rs) {
        bounds = new AtomGoal.AtomBounds(rs.getFinalGoals());

    }

    @Override
    public PairGen<AtomGoal> generatePairs(GoalBag<AtomGoal> goals, int depth) {
        List<GoalPair<AtomGoal>> pairList = new ArrayList<>();
        for(AtomGoal upper: goals) {
            pairList.addAll(getAddTransformations(upper));
            pairList.addAll(getUnaryTransformations(upper));
        }

        List<Tuple<GoalPair<AtomGoal>, Double>> list = new ArrayList<>(pairList.size());
        for (GoalPair<AtomGoal> pair : pairList) {
            if (check(pair)) {
                double v = getValue(goals, pair, bounds);
                list.add(new Tuple<>(pair, v));
            }
        }
        list.sort(Comparator.comparingDouble(Tuple::getB));
        List<GoalPair<AtomGoal>> out = list.stream().map(Tuple::getA).collect(Collectors.toList());
        return new V1PairGen(out);
    }

    public static double getValue(GoalBag<AtomGoal> goals, GoalPair<AtomGoal> pair, AtomGoal.AtomBounds bounds) {
        HashSet<AtomGoal> goalSet = new HashSet<>(goals.asList());
        goalSet.removeAll(pair.getUppers());

        goalSet.addAll(pair.getLowers());
        double v = 0;
        for (AtomGoal g : goalSet) {
            int i = 0;
            for (Atom a : g) {
                i += Math.abs(a.x) + Math.abs(a.y) + Math.abs(a.z);
            }
            v += i;
        }
        List<AtomGoal> goalList = new ArrayList<>(goalSet);
        for (int i = 0; i < goalList.size(); i++) {
            AtomGoal g = goalList.get(i);
            List<AtomGoal> toRemove = patternRepeated(bounds, goalList, g);
            goalList.removeAll(toRemove);
            goalList.add(i, g);
        }
        for (AtomGoal g: goalList){
            v += Math.pow(g.atomCount(), 2);
        }
        return v;
    }


    private List<GoalPair<AtomGoal>> getUnaryTransformations(AtomGoal upper) {
        List<GoalPair<AtomGoal>> pairs = new ArrayList<>();
        Collection<Tuple<? extends Transformation, AtomGoal>> ts = SimpleTransformation.applyAllUnaryOpBackwards(upper);
        for (Tuple<? extends Transformation, AtomGoal> t : ts) {
            pairs.add(new GoalPair<>(upper, t.getB(), t.getA()));
        }
        return pairs;
    }

    private List<GoalPair<AtomGoal>> getAddTransformations(AtomGoal upper) {
        List<GoalPair<AtomGoal>> pairs = new ArrayList<>();
        // Addition
        Collection<AtomGoal> splits = upper.allSplitsRecursive();
        Set<AtomGoal> seen = new HashSet<>();
        for (AtomGoal a : splits) {
            if (seen.contains(a)){
                continue;
            }
            AtomGoal b = upper.without(a);
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
            List<AtomGoal> lowers = new ArrayList<>();
            lowers.add(a);
            lowers.add(b);
            pairs.add(new GoalPair<>(upper, lowers, add));
        }

        return pairs;
    }

    private static List<AtomGoal> patternRepeated(AtomGoal.AtomBounds bounds, Collection<AtomGoal> goals, AtomGoal pattern){
        List<AtomGoal> matches = new ArrayList<>();
        int bx = bounds.xMax-bounds.xMin;
        int by = bounds.yMax-bounds.yMin;
        for (int i = -bx; i <= bx; i++) {
            for (int j = -by; j <= by; j++) {
                AtomGoal tmp = moveGoal(pattern, i, j, 0);
                if (goals.contains(tmp)){
                    matches.add(tmp);
                }

            }
        }
        return matches;
    }

    @SuppressWarnings("SameParameterValue")
    private static AtomGoal moveGoal(AtomGoal goal, int x, int y, int z){
        AtomGoal.Factory factory = new AtomGoal.Factory();
        for (Atom a: goal){
            factory.add(a.moved(x, y, z));
        }
        return factory.get();
    }

    private boolean check(GoalPair<AtomGoal> p){
        for (AtomGoal l: p.getLowers()) {
            for (Atom a : l) {
                if (bounds.excludes(a)) {
                    return false;
                }
            }
        }
        return true;

    }

    private class V1PairGen implements PairGen<AtomGoal> {
        final List<GoalPair<AtomGoal>> pairs;
        int i;

        V1PairGen(List<GoalPair<AtomGoal>> pairs) {
            this.pairs = pairs;
            i = 0;
        }

        @Override
        public GoalPair<AtomGoal> next() {
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
