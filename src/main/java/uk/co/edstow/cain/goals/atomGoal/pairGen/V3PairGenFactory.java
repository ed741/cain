package uk.co.edstow.cain.goals.atomGoal.pairGen;

import uk.co.edstow.cain.goals.atomGoal.AtomGoal;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;

public class V3PairGenFactory extends V2PairGenFactory{

    private static Comparator<Tuple<Distance, AtomGoal>> entryComparator = Comparator.comparingInt((Tuple<Distance, AtomGoal> t) -> t.getB().size()).thenComparingInt(t -> -t.getA().manhattanXY());

    public V3PairGenFactory(AtomGoal.AtomBounds bounds) {
        this.bounds = bounds;
    }

    @Override
    public Collection<Tuple<List<GoalPair<AtomGoal>>, AtomGoal>> applyAllUnaryOpForwards(List<AtomGoal> initialGoals, Context<AtomGoal> context, AtomGoal goal) {
        return SimplePairGenFactory.applyAllUnaryOps(initialGoals.get(0), goal);
    }


    private AtomGoal.AtomBounds bounds;

    @Override
    public PairGen<AtomGoal> generatePairs(GoalBag<AtomGoal> goals, Context<AtomGoal> context) {
        return new V3PairGen(goals);
    }

    private class V3PairGen extends V2PairGen {

        public V3PairGen(GoalBag<AtomGoal> goals) {
            super(goals);
        }


        @Override
        public GoalPair<AtomGoal> next() {
            if(!currentList.isEmpty()){
                return currentList.remove(0);
            }
            if(getJ() >= goals.size() || getI() >= goals.size()){
                return null;
            }

            List<Tuple<GoalPair<AtomGoal>, Tuple<Double, Integer>>> rankList = new ArrayList<>();
            updateIJ();
            while(getJ() < goals.size() && getI() < goals.size()) {

                AtomGoal a = goals.get(getI());
                AtomGoal b = goals.get(getJ());
                boolean diagonal = getI()== getJ();
                List<Tuple<Distance, AtomGoal>> list = getAtomDistanceList(a, b, diagonal);

                if (!diagonal) {
                    for (Tuple<Distance, AtomGoal> tuple : list) {
                        AtomGoal tmp = tuple.getA().inverse().translate(tuple.getB());
                        if (tmp.equals(a)) {
                            AtomGoal lower = new Distance(tuple.getA().majorXYDirection(), 1).inverse().translate(tuple.getB());
                            SimpleTransformation.Move mov = new SimpleTransformation.Move(1, tuple.getA().majorXYDirection(), lower);
                            GoalPair<AtomGoal> pair = new GoalPair<>(b, lower, mov);
                            double v = V1PairGenFactory.getValue(goals, pair, bounds);
                            rankList.add(Tuple.triple(pair, v, tuple.getB().size()));
                        } else {
                            int div = a.divide(tmp);
                            if (div > 1 && (((div - 1) & div) == 0)) {
                                AtomGoal lower = b.added(b);
                                SimpleTransformation.Div divide = new SimpleTransformation.Div(1, lower);
                                GoalPair<AtomGoal> pair = new GoalPair<>(b, lower, divide);
                                double v = V1PairGenFactory.getValue(goals, pair, bounds);
                                rankList.add(Tuple.triple(pair, v, tuple.getB().size()));
                            } else {
                                AtomGoal split2 = a.without(tmp);
                                List<AtomGoal> lowers = Arrays.asList(tmp, split2);
                                GoalPair<AtomGoal> pair = new GoalPair<>(a, lowers, new SimpleTransformation.Add(tmp, split2));
                                double v = V1PairGenFactory.getValue(goals, pair, bounds);
                                rankList.add(Tuple.triple(pair, v, tuple.getB().size()));
                            }
                        }

                    }
                } else {
//                    System.out.println(a.getTableString(false));
                    for (Tuple<Distance, AtomGoal> tuple : list) {
                        AtomGoal split1 = a.without(tuple.getB());
                        AtomGoal split2 = tuple.getB();

//                        System.out.println(tuple.getA());
//                        System.out.println(split1.getTableString(true));
//                        System.out.println(split2.getTableString(true));
                        GoalPair<AtomGoal> pair = new GoalPair<>(a, Arrays.asList(split1, split2), new SimpleTransformation.Add(split1, split2));
                        double v = V1PairGenFactory.getValue(goals, pair, bounds);
                        rankList.add(Tuple.triple(pair, v, tuple.getB().size()));
                    }


                }
                updateIJ();
            }
            Comparator<Tuple<GoalPair<AtomGoal>, Tuple<Double, Integer>>> c = Comparator.comparingInt((Tuple<GoalPair<AtomGoal>, Tuple<Double, Integer>> v) -> v.getB().getB()).reversed().thenComparingDouble((Tuple<GoalPair<AtomGoal>, Tuple<Double, Integer>> v) -> v.getB().getA());
            rankList.sort(c);
            rankList.forEach(t -> currentList.add(t.getA()));
            if (currentList.isEmpty()){
                return null;
            } else {
                return currentList.remove(0);
            }
        }
    }
}
