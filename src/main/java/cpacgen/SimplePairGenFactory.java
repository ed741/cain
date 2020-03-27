package cpacgen;

import cpacgen.util.Bounds;
import cpacgen.util.Tuple;

import java.util.*;

public class SimplePairGenFactory implements PairGenFactory{

    private Bounds bounds;

    @Override
    public void init(ReverseSplit rs) {
        bounds = new Bounds(rs.finalGoals);
    }

    @Override
    public PairGen generatePairs(Goal.Bag goals) {
        return new SimplePairGen(goals);
    }

    public class SimplePairGen implements PairGen {

        private Goal.Bag goalList;
        private List<Goal.Pair> currentList;


        public SimplePairGen(Goal.Bag goals) {
            goalList = new Goal.Bag(goals);
            goalList.setImmutable();
            currentList = new ArrayList<>();


        }

        @Override
        public final Goal.Pair next() {
            Goal.Pair p = get_next();
            while(p != null && !check(p)){
                p = get_next();
            }
            return p;

        }

        final void appendCurrentList(Goal.Pair pair){
            currentList.add(pair);
        }

        final void appendCurrentList(Collection<Goal.Pair> pairs){
            currentList.addAll(pairs);
        }


        boolean check(Goal.Pair p){
            for (Goal l: p.lowers) {
                for (Atom a : l) {
                    if (!bounds.includes(a)) {
                        return false;
                    }
                }
            }
            return true;

        }

        private Goal.Pair get_next() {
            if (!currentList.isEmpty()){
                Goal.Pair p = currentList.remove(0);
                return p;

            } else if (!goalList.isEmpty()){
                Goal upper = goalList.remove(0);

                putTransformations(upper);

                return this.get_next();
            }
            return null;
        }

        void putTransformations(Goal upper){
            // Add
            appendCurrentList(getAddTransformations(upper));

            // Mov and Div
            appendCurrentList(getUnaryTransformations(upper));
        }

        List<Goal.Pair> getUnaryTransformations(Goal upper) {
            List<Goal.Pair> pairs = new ArrayList<>();
            Collection<Tuple<? extends Transformation, Goal>> ts = Transformation.applyAllUnaryOpBackwards(upper);
            for (Tuple<? extends Transformation, Goal> t: ts){
                pairs.add(new Goal.Pair(upper, t.getB(), t.getA()));
            }
            return pairs;
        }

        List<Goal.Pair> getAddTransformations(Goal upper) {
            List<Goal.Pair> pairs = new ArrayList<>();
            // Addition
            Collection<Goal> splits = upper.allSplits();
            for (Goal a : splits) {
                Goal b = upper.without(a);
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

    }
}
