package cpacgen.pairgen;

import cpacgen.Atom;
import cpacgen.Goal;
import cpacgen.ReverseSplit;
import cpacgen.Transformation;
import cpacgen.util.Bounds;
import cpacgen.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;

public class V1PairGenFactory implements PairGenFactory{

    private Bounds bounds;
    @Override
    public void init(ReverseSplit rs) {
        bounds = new Bounds(rs.getFinalGoals());

    }

    @Override
    public PairGen generatePairs(Goal.Bag goals) {
        List<Goal.Pair> pairList = new ArrayList<>();
        for(Goal upper: goals) {
            pairList.addAll(getAddTransformations(upper));
            pairList.addAll(getUnaryTransformations(upper));
        }

        List<Tuple<Goal.Pair, Double>> list = new ArrayList<>(pairList.size());
        for (Goal.Pair pair : pairList) {
            if (check(pair)) {
                HashSet<Goal> goalSet = new HashSet<>(goals);
                goalSet.remove(pair.getUpper());

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
                    List<Goal> toRemove = patternRepeated(goalList, g);
                    goalList.removeAll(toRemove);
                    goalList.add(i, g);
                }
                for (Goal g: goalList){
                    v += Math.pow(g.atomCount(), 2);
                }
                list.add(new Tuple<>(pair, v));
            }
        }
        list.sort(Comparator.comparingDouble(Tuple::getB));
        List<Goal.Pair> out = list.stream().map(Tuple::getA).collect(Collectors.toList());
        return new V1PairGen(out);
    }


    List<Goal.Pair> getUnaryTransformations(Goal upper) {
        List<Goal.Pair> pairs = new ArrayList<>();
        Collection<Tuple<? extends Transformation, Goal>> ts = Transformation.applyAllUnaryOpBackwards(upper);
        for (Tuple<? extends Transformation, Goal> t : ts) {
            pairs.add(new Goal.Pair(upper, t.getB(), t.getA()));
        }
        return pairs;
    }

    List<Goal.Pair> getAddTransformations(Goal upper) {
        List<Goal.Pair> pairs = new ArrayList<>();
        // Addition
        Collection<Goal> splits = upper.allSplits();
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

            Transformation.Add add = new Transformation.Add(a, b);
            try {
                if (!add.applyForwards().same(upper)) throw new AssertionError();
            } catch (Transformation.TransformationApplicationException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            List<Goal> lowers = new ArrayList<>();
            lowers.add(a);
            lowers.add(b);
            pairs.add(new Goal.Pair(upper, lowers, add));
        }

        return pairs;
    }

    private List<Goal> patternRepeated(Collection<Goal> goals, Goal pattern){
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

    Goal moveGoal(Goal goal, int x, int y, int z){
        Goal.Factory factory = new Goal.Factory();
        for (Atom a: goal){
            factory.add(a.moved(x, y, z));
        }
        return factory.get();
    }

    boolean check(Goal.Pair p){
        for (Goal l: p.getLowers()) {
            for (Atom a : l) {
                if (!bounds.includes(a)) {
                    return false;
                }
            }
        }
        return true;

    }

    private class V1PairGen implements PairGen {
        final List<Goal.Pair> pairs;
        int i;

        public V1PairGen(List<Goal.Pair> pairs) {
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
