package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.structures.Atom;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.ReverseSearch;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;

public class V3PairGenFactory extends V2PairGenFactory{

    private static Comparator<Tuple<Distance, Goal>> entryComparator = Comparator.comparingInt((Tuple<Distance, Goal> t) -> t.getB().size()).thenComparingInt(t -> -t.getA().manhattanXY());

    @Override
    public Collection<Tuple<List<GoalPair>, Goal>> applyAllUnaryOpForwards(List<Goal> initialGoals, int depth, Goal goal) {
        return SimplePairGenFactory.applyAllUnaryOps(initialGoals.get(0), goal);
    }


    private Atom.Bounds bounds;
    @Override
    public void init(ReverseSearch rs) {
        bounds = new Atom.Bounds(rs.getFinalGoals());

    }
    @Override
    public PairGen generatePairs(GoalBag goals, int depth) {
        return new V3PairGen(goals);
    }

    private class V3PairGen extends V2PairGen {

        public V3PairGen(GoalBag goals) {
            super(goals);
        }


        @Override
        public GoalPair next() {
            if(!currentList.isEmpty()){
                return currentList.remove(0);
            }
            if(getJ() >= goals.size() || getI() >= goals.size()){
                return null;
            }

            List<Tuple<GoalPair, Tuple<Double, Integer>>> rankList = new ArrayList<>();
            updateIJ();
            while(getJ() < goals.size() && getI() < goals.size()) {

                Goal a = goals.get(getI());
                Goal b = goals.get(getJ());
                boolean diagonal = getI()== getJ();
                List<Tuple<Distance, Goal>> list = getAtomDistanceList(a, b, diagonal);

                if (!diagonal) {
                    for (Tuple<Distance, Goal> tuple : list) {
                        Goal tmp = tuple.getA().inverse().translate(tuple.getB());
                        if (tmp.equals(a)) {
                            Goal lower = new Distance(tuple.getA().majorXYDirection(), 1).inverse().translate(tuple.getB());
                            SimpleTransformation.Move mov = new SimpleTransformation.Move(1, tuple.getA().majorXYDirection(), lower);
                            GoalPair pair = new GoalPair(b, lower, mov);
                            double v = V1PairGenFactory.getValue(goals, pair, bounds);
                            rankList.add(Tuple.triple(pair, v, tuple.getB().size()));
                        } else {
                            int div = a.divide(tmp);
                            if (div > 1 && (((div - 1) & div) == 0)) {
                                Goal lower = new Goal.Factory(b).addAll(b).get();
                                SimpleTransformation.Div divide = new SimpleTransformation.Div(1, lower);
                                GoalPair pair = new GoalPair(b, lower, divide);
                                double v = V1PairGenFactory.getValue(goals, pair, bounds);
                                rankList.add(Tuple.triple(pair, v, tuple.getB().size()));
                            } else {
                                Goal split2 = a.without(tmp);
                                List<Goal> lowers = Arrays.asList(tmp, split2);
                                GoalPair pair = new GoalPair(a, lowers, new SimpleTransformation.Add(tmp, split2));
                                double v = V1PairGenFactory.getValue(goals, pair, bounds);
                                rankList.add(Tuple.triple(pair, v, tuple.getB().size()));
                            }
                        }

                    }
                } else {
//                    System.out.println(a.getCharTableString(false));
                    for (Tuple<Distance, Goal> tuple : list) {
                        Goal split1 = a.without(tuple.getB());
                        Goal split2 = tuple.getB();

//                        System.out.println(tuple.getA());
//                        System.out.println(split1.getCharTableString(true));
//                        System.out.println(split2.getCharTableString(true));
                        GoalPair pair = new GoalPair(a, Arrays.asList(split1, split2), new SimpleTransformation.Add(split1, split2));
                        double v = V1PairGenFactory.getValue(goals, pair, bounds);
                        rankList.add(Tuple.triple(pair, v, tuple.getB().size()));
                    }


                }
                updateIJ();
            }
            Comparator<Tuple<GoalPair, Tuple<Double, Integer>>> c = Comparator.comparingInt((Tuple<GoalPair, Tuple<Double, Integer>> v) -> v.getB().getB()).reversed().thenComparingDouble((Tuple<GoalPair, Tuple<Double, Integer>> v) -> v.getB().getA());
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
