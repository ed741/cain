package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;
import uk.co.edstow.cain.structures.Atom;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.ReverseSearch;
import uk.co.edstow.cain.Transformation;

import java.util.*;

public class SimplePairGenFactory implements PairGenFactory {

    private Goal.Bounds bounds;


    public static Collection<Tuple<List<GoalPair>, Goal>> applyAllUnaryOps(Goal goal, Goal upper){
        ArrayList<Tuple<List<GoalPair>, Goal>> list = new ArrayList<>();
        for (SimpleTransformation.Direction d: SimpleTransformation.Direction.values()){
            for (int i = 0; i < 4; i++){
                SimpleTransformation t = new SimpleTransformation.Move(i, d, goal);
                try {
                    Goal go = t.applyForwards();
                    if(go.same(upper)) {
                        list.add(new Tuple<>(Collections.singletonList(new GoalPair(upper, goal, t)), goal));
                    }
                } catch (Transformation.TransformationApplicationException ignored) {}
            }
        }
        for (int i = 0; i < 8; i++){
            SimpleTransformation t = new SimpleTransformation.Div(i, goal);
            try {
                Goal go = t.applyForwards();
                if(go.same(upper)) {
                    list.add(new Tuple<>(Collections.singletonList(new GoalPair(upper, goal, t)), goal));
                }
            } catch (Transformation.TransformationApplicationException ignored) {}
        }
        return list;
    }

    @Override
    public Collection<Tuple<List<GoalPair>, Goal>> applyAllUnaryOpForwards(List<Goal> initialGoals, int depth, Goal goal) {
        return applyAllUnaryOps(initialGoals.get(0), goal);
    }

    @Override
    public void init(ReverseSearch rs) {
        bounds = new Goal.Bounds(rs.getFinalGoals());
    }

    @Override
    public PairGen generatePairs(GoalBag goals, int depth) {
        return generatePairs(goals);
    }
    @SuppressWarnings("WeakerAccess")
    public PairGen generatePairs(GoalBag goals) {
        return new SimplePairGen(goals);
    }

    public class SimplePairGen implements PairGen {

        private final GoalBag goalList;
        private final List<GoalPair> currentList;
        private int count;


        public SimplePairGen(GoalBag goals) {
            goalList = new GoalBag(goals);
            goalList.setImmutable();
            currentList = new ArrayList<>();


        }

        @Override
        public final GoalPair next() {
            count++;
            GoalPair p = get_next();
            while(p != null && !check(p)){
                p = get_next();
            }
            return p;

        }

        @Override
        public int getNumber() {
            return count;
        }

        final void appendCurrentList(GoalPair pair){
            currentList.add(pair);
        }

        final void appendCurrentList(Collection<GoalPair> pairs){
            currentList.addAll(pairs);
        }


        boolean check(GoalPair p){
            for (Goal l: p.getLowers()) {
                for (Atom a : l) {
                    if (bounds.excludes(a)) {
                        return false;
                    }
                }
            }
            return true;

        }

        private GoalPair get_next() {
            if (!currentList.isEmpty()){
                return currentList.remove(0);

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

        List<GoalPair> getUnaryTransformations(Goal upper) {
            List<GoalPair> pairs = new ArrayList<>();
            Collection<Tuple<? extends Transformation, Goal>> ts = SimpleTransformation.applyAllUnaryOpBackwards(upper);
            for (Tuple<? extends Transformation, Goal> t: ts){
                pairs.add(new GoalPair(upper, t.getB(), t.getA()));
            }
            return pairs;
        }

        List<GoalPair> getAddTransformations(Goal upper) {
            List<GoalPair> pairs = new ArrayList<>();
            // Addition
            Collection<Goal> splits = upper.allSplitsRecursive();
            for (Goal a : splits) {
                Goal b = upper.without(a);
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

    }
}
