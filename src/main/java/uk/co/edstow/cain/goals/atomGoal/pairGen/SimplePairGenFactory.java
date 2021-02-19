package uk.co.edstow.cain.goals.atomGoal.pairGen;

import uk.co.edstow.cain.goals.atomGoal.Atom;
import uk.co.edstow.cain.goals.atomGoal.AtomGoal;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;
import uk.co.edstow.cain.Transformation;

import java.util.*;

public class SimplePairGenFactory implements PairGenFactory<AtomGoal, SimpleTransformation> {

    private final AtomGoal.AtomBounds bounds;

    public SimplePairGenFactory(AtomGoal.AtomBounds bounds) {
        this.bounds = bounds;
    }


    public static Collection<Tuple<List<GoalPair<AtomGoal, SimpleTransformation>>, AtomGoal>> applyAllUnaryOps(AtomGoal goal, AtomGoal upper){
        ArrayList<Tuple<List<GoalPair<AtomGoal, SimpleTransformation>>, AtomGoal>> list = new ArrayList<>();
        for (SimpleTransformation.Direction d: SimpleTransformation.Direction.values()){
            for (int i = 0; i < 4; i++){
                SimpleTransformation t = new SimpleTransformation.Move(i, d, goal);
                try {
                    AtomGoal go = t.applyForwards();
                    if(go.same(upper)) {
                        list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(upper, goal, t)), goal));
                    }
                } catch (Transformation.TransformationApplicationException ignored) {}
            }
        }
        for (int i = 0; i < 8; i++){
            SimpleTransformation t = new SimpleTransformation.Div(i, goal);
            try {
                AtomGoal go = t.applyForwards();
                if(go.same(upper)) {
                    list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(upper, goal, t)), goal));
                }
            } catch (Transformation.TransformationApplicationException ignored) {}
        }
        return list;
    }

    @Override
    public Collection<Tuple<List<GoalPair<AtomGoal, SimpleTransformation>>, AtomGoal>> applyAllUnaryOpForwards(List<AtomGoal> initialGoals, Context<AtomGoal, SimpleTransformation> context, AtomGoal goal) {
        return applyAllUnaryOps(initialGoals.get(0), goal);
    }

    @Override
    public PairGen<AtomGoal, SimpleTransformation> generatePairs(GoalBag<AtomGoal> goals, Context<AtomGoal, SimpleTransformation> context) {
        return new SimplePairGen(goals);
    }

    @Override
    public SimpleTransformation getDummyTransformation(List<AtomGoal> upperGoals, List<AtomGoal> lowerGoals, Context<AtomGoal, SimpleTransformation> context) {
        return null;
    }

    public class SimplePairGen implements PairGen<AtomGoal, SimpleTransformation> {

        private final GoalBag<AtomGoal> goalList;
        private final List<GoalPair<AtomGoal, SimpleTransformation>> currentList;
        private int count;


        public SimplePairGen(GoalBag<AtomGoal> goals) {
            goalList = new GoalBag<>(goals);
            goalList.setImmutable();
            currentList = new ArrayList<>();


        }

        @Override
        public final GoalPair<AtomGoal, SimpleTransformation> next() {
            count++;
            GoalPair<AtomGoal, SimpleTransformation> p = get_next();
            while(p != null && !check(p)){
                p = get_next();
            }
            return p;

        }

        @Override
        public int getNumber() {
            return count;
        }

        final void appendCurrentList(GoalPair<AtomGoal, SimpleTransformation> pair){
            currentList.add(pair);
        }

        final void appendCurrentList(Collection<GoalPair<AtomGoal, SimpleTransformation>> pairs){
            currentList.addAll(pairs);
        }


        boolean check(GoalPair<AtomGoal, SimpleTransformation> p){
            for (AtomGoal l: p.getLowers()) {
                for (Atom a : l) {
                    if (bounds.excludes(a)) {
                        return false;
                    }
                }
            }
            return true;

        }

        private GoalPair<AtomGoal, SimpleTransformation> get_next() {
            if (!currentList.isEmpty()){
                return currentList.remove(0);

            } else if (!goalList.isEmpty()){
                AtomGoal upper = goalList.remove(0);

                putTransformations(upper);

                return this.get_next();
            }
            return null;
        }

        void putTransformations(AtomGoal upper){
            // Add
            appendCurrentList(getAddTransformations(upper));

            // Mov and Div
            appendCurrentList(getUnaryTransformations(upper));
        }

        List<GoalPair<AtomGoal, SimpleTransformation>> getUnaryTransformations(AtomGoal upper) {
            List<GoalPair<AtomGoal, SimpleTransformation>> pairs = new ArrayList<>();
            Collection<Tuple<? extends SimpleTransformation, AtomGoal>> ts = SimpleTransformation.applyAllUnaryOpBackwards(upper);
            for (Tuple<? extends SimpleTransformation, AtomGoal> t: ts){
                pairs.add(new GoalPair<>(upper, t.getB(), t.getA()));
            }
            return pairs;
        }

        List<GoalPair<AtomGoal, SimpleTransformation>> getAddTransformations(AtomGoal upper) {
            List<GoalPair<AtomGoal, SimpleTransformation>> pairs = new ArrayList<>();
            // Addition
            Collection<AtomGoal> splits = upper.allSplitsRecursive();
            for (AtomGoal a : splits) {
                AtomGoal b = upper.without(a);
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

    }
}
