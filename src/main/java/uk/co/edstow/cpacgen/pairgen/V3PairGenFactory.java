package uk.co.edstow.cpacgen.pairgen;

import uk.co.edstow.cpacgen.Goal;
import uk.co.edstow.cpacgen.ReverseSearch;
import uk.co.edstow.cpacgen.Transformation;
import uk.co.edstow.cpacgen.util.Bounds;
import uk.co.edstow.cpacgen.util.Tuple;

import java.util.*;

public class V3PairGenFactory extends V2PairGenFactory{

    private static Comparator<Tuple<Distance, Goal>> entryComparator = Comparator.comparingInt((Tuple<Distance, Goal> t) -> t.getB().size()).thenComparingInt(t -> -t.getA().manhattan());

    @Override
    public Collection<Tuple<List<Goal.Pair>, Goal>> applyAllUnaryOpForwards(Goal initialGoal, int depth, Goal goal) {
        return SimplePairGenFactory.applyAllUnaryOps(initialGoal, goal);
    }


    private Bounds bounds;
    @Override
    public void init(ReverseSearch rs) {
        bounds = new Bounds(rs.getFinalGoals());

    }
    @Override
    public PairGen generatePairs(Goal.Bag goals, int depth) {
        return new V3PairGen(goals);
    }

    private class V3PairGen extends V2PairGen {

        public V3PairGen(Goal.Bag goals) {
            super(goals);
        }


        @Override
        public Goal.Pair next() {
            if(!currentList.isEmpty()){
                return currentList.remove(0);
            }
            if(getj() >= goals.size() || geti() >= goals.size()){
                return null;
            }

            List<Tuple<Goal.Pair, Tuple<Double, Integer>>> rankList = new ArrayList<>();
            updateIJ();
            while(getj() < goals.size() && geti() < goals.size()) {

                //System.out.println("ii: " + ii + " jj: " + jj + " i: " + geti() + " j: " + getj());



                Goal a = goals.get(geti());
                Goal b = goals.get(getj());
                boolean diaganal = geti()==getj();
                //System.out.println("ii: " + ii + " jj: " + jj + " i: " + geti() + " j: " +
                List<Tuple<Distance, Goal>> list = getAtomDistanceList(a, b, diaganal);

                if (!diaganal) {
                    for (Tuple<Distance, Goal> tuple : list) {
                        Goal tmp = tuple.getA().inverse().translate(tuple.getB());
                        if (tmp.equals(a)) {
                            Goal lower = new Distance(tuple.getA().majorDirection(), 1).inverse().translate(tuple.getB());
                            Transformation.Move mov = new Transformation.Move(1, tuple.getA().majorDirection(), lower);
                            Goal.Pair pair = new Goal.Pair(b, lower, mov);
                            double v = V1PairGenFactory.getValue(goals, pair, bounds);
                            rankList.add(Tuple.triple(pair, v, tuple.getB().size()));
                        } else {
                            int div = a.divide(tmp);
                            if (div > 1 && (((div - 1) & div) == 0)) {
                                Goal lower = new Goal.Factory(b).addAll(b).get();
                                Transformation.Div divide = new Transformation.Div(1, lower);
                                Goal.Pair pair = null;
                                pair = new Goal.Pair(b, lower, divide);
                                double v = V1PairGenFactory.getValue(goals, pair, bounds);
                                rankList.add(Tuple.triple(pair, v, tuple.getB().size()));
                            } else {
                                Goal split2 = a.without(tmp);
                                List<Goal> lowers = Arrays.asList(tmp, split2);
                                Goal.Pair pair = new Goal.Pair(a, lowers, new Transformation.Add(tmp, split2));
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
                        Goal.Pair pair = new Goal.Pair(a, Arrays.asList(split1, split2), new Transformation.Add(split1, split2));
                        double v = V1PairGenFactory.getValue(goals, pair, bounds);
                        rankList.add(Tuple.triple(pair, v, tuple.getB().size()));
                    }


                }
                updateIJ();
            }
            Comparator<Tuple<Goal.Pair, Tuple<Double, Integer>>> c = Comparator.comparingInt((Tuple<Goal.Pair, Tuple<Double, Integer>> v) -> v.getB().getB()).reversed().thenComparingDouble((Tuple<Goal.Pair, Tuple<Double, Integer>> v) -> v.getB().getA());
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
