package uk.co.edstow.cain.goals.atomGoal.pairGen;

import uk.co.edstow.cain.goals.atomGoal.Atom;
import uk.co.edstow.cain.goals.atomGoal.AtomGoal;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;

public class V2PairGenFactory implements PairGenFactory<AtomGoal, SimpleTransformation> {

    private static final Comparator<Tuple<Distance, AtomGoal>> entryComparator = Comparator.comparingInt((Tuple<Distance, AtomGoal> t) -> t.getB().size()).thenComparingInt(t -> -t.getA().manhattanXY());


    @Override
    public Collection<Tuple<List<GoalPair<AtomGoal, SimpleTransformation>>, AtomGoal>> applyAllUnaryOpForwards(List<AtomGoal> initialGoals, Context<AtomGoal, SimpleTransformation> context, AtomGoal goal) {
        return SimplePairGenFactory.applyAllUnaryOps(initialGoals.get(0), goal);
    }

    @Override
    public PairGen<AtomGoal, SimpleTransformation> generatePairs(GoalBag<AtomGoal> goals, Context<AtomGoal, SimpleTransformation> context) {
        return new V2PairGen(goals);
    }

    @Override
    public SimpleTransformation getDummyTransformation(List<AtomGoal> upperGoals, List<AtomGoal> lowerGoals, Context<AtomGoal, SimpleTransformation> context) {
        return new SimpleTransformation.Null(lowerGoals.size(), upperGoals.size());
    }



    protected class V2PairGen implements PairGen<AtomGoal, SimpleTransformation> {
        final GoalBag<AtomGoal> goals;
        int ii;
        int jj;
        int dia = -1;
        final List<GoalPair<AtomGoal, SimpleTransformation>> currentList = new ArrayList<>();
        private int count;

        V2PairGen(GoalBag<AtomGoal> goals) {
            this.goals = goals;
            ii = 0;
            jj = 0;
        }

        int getI(){
            if (dia >=0){
                return dia;
            }
            return ii;
        }
        int getJ(){
            if (dia >=0){
                return dia;
            }
            return jj - ii;
        }
        void updateIJ(){
            if (dia < 0) {
                do{
                    if (ii < Math.min(jj, goals.size() - 1)) {
                        ii++;
                    } else {
                        jj++;
                        int d = jj - goals.size();
                        if (d < 0) {
                            ii = 0;
                        } else {
                            ii = d + 1;
                        }
                    }
                }
                while(jj-ii == ii);
                if (jj-ii >= goals.size() || ii >= goals.size()) {
                    dia++;
                }
            } else {
                dia++;
            }

        }

        @Override
        public GoalPair<AtomGoal, SimpleTransformation> next() {
            count++;
            while(currentList.isEmpty()) {
                updateIJ();
                if (getJ() >= goals.size() || getI() >= goals.size()) {
                    return null;
                }

                AtomGoal a = goals.get(getI());
                AtomGoal b = goals.get(getJ());
                boolean diagonal = getI()== getJ();

                List<Tuple<Distance, AtomGoal>> list = getAtomDistanceList(a, b, diagonal);
                list.sort(entryComparator);
                if (!diagonal) {
                    for (Tuple<Distance, AtomGoal> tuple : list) {
                        AtomGoal tmp = tuple.getA().inverse().translate(tuple.getB());
                        if (tmp.equals(a)) {
                            SimpleTransformation.Move mov = new SimpleTransformation.Move(1, tuple.getA().majorXYDirection().opposite(), a);
                            currentList.add(new GoalPair<>(a, mov.applyForwards(), mov));
                        } else {
                            AtomGoal split2 = a.without(tmp);
                            List<AtomGoal> lowers = Arrays.asList(tmp, split2);
                            currentList.add(new GoalPair<>(a, lowers, new SimpleTransformation.Add(tmp, split2)));
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
                        currentList.add(new GoalPair<>(a, Arrays.asList(split1, split2), new SimpleTransformation.Add(split1, split2)));
                    }


                }

            }
            return currentList.remove(currentList.size()-1);
        }

        @Override
        public int getNumber() {
            return count;
        }


    }

    static List<Tuple<Distance, AtomGoal>> getAtomDistanceList(AtomGoal a, AtomGoal b, boolean diagonal) {
        Map<Distance, AtomGoal.Factory> distanceMap = new HashMap<>();
        for (Iterator<Tuple<Atom, Integer>> ita = a.uniqueCountIterator(); ita.hasNext(); ) {
            Tuple<Atom, Integer> ta = ita.next();
            Atom atomA = ta.getA();
            for (Iterator<Tuple<Atom, Integer>> itb = b.uniqueCountIterator(); itb.hasNext(); ) {
                Tuple<Atom, Integer> tb = itb.next();
                Atom atomB = tb.getA();

                Distance d = new Distance(atomA, atomB);
                AtomGoal.Factory goalFactory = distanceMap.getOrDefault(d, new AtomGoal.Factory());
                int count = Math.min(ta.getB(), tb.getB());
                if (diagonal && d.isZero()){
                    count /= 2;
                }
                for (int i = 0; i < count; i++) {
                    goalFactory.add(atomB);
                }
                distanceMap.put(d, goalFactory);

            }
        }
        List<Tuple<Distance, AtomGoal>> list = new ArrayList<>(distanceMap.size());
        distanceMap.forEach((key, value) -> list.add(new Tuple<>(key, value.get())));
        if (!diagonal) {
            list.removeIf(t -> !(b.equals(t.getB())));
        }
        return list;
    }

}
