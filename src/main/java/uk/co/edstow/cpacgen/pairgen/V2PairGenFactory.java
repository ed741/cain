package uk.co.edstow.cpacgen.pairgen;

import uk.co.edstow.cpacgen.Atom;
import uk.co.edstow.cpacgen.Goal;
import uk.co.edstow.cpacgen.ReverseSearch;
import uk.co.edstow.cpacgen.util.Tuple;

import java.util.*;

public class V2PairGenFactory implements PairGenFactory{

    private static final Comparator<Tuple<Distance, Goal>> entryComparator = Comparator.comparingInt((Tuple<Distance, Goal> t) -> t.getB().size()).thenComparingInt(t -> -t.getA().manhattanXY());


    @Override
    public Collection<Tuple<List<Goal.Pair>, Goal>> applyAllUnaryOpForwards(List<Goal> initialGoals, int depth, Goal goal) {
        return SimplePairGenFactory.applyAllUnaryOps(initialGoals.get(0), goal);
    }

    @Override
    public void init(ReverseSearch rs) {
    }

    @Override
    public PairGen generatePairs(Goal.Bag goals, int depth) {
        return new V2PairGen(goals);
    }

    protected class V2PairGen implements PairGen {
        final Goal.Bag goals;
        int ii;
        int jj;
        int dia = -1;
        final List<Goal.Pair> currentList = new ArrayList<>();

        V2PairGen(Goal.Bag goals) {
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
        public Goal.Pair next() {
            while(currentList.isEmpty()) {
                updateIJ();
                if (getJ() >= goals.size() || getI() >= goals.size()) {
                    return null;
                }

                Goal a = goals.get(getI());
                Goal b = goals.get(getJ());
                boolean diagonal = getI()== getJ();

                List<Tuple<Distance, Goal>> list = getAtomDistanceList(a, b, diagonal);
                list.sort(entryComparator);
                if (!diagonal) {
                    for (Tuple<Distance, Goal> tuple : list) {
                        Goal tmp = tuple.getA().inverse().translate(tuple.getB());
                        if (tmp.equals(a)) {
                            SimpleTransformation.Move mov = new SimpleTransformation.Move(1, tuple.getA().majorXYDirection().opposite(), a);
                            currentList.add(new Goal.Pair(a, mov.applyForwards(), mov));
                        } else {
                            Goal split2 = a.without(tmp);
                            List<Goal> lowers = Arrays.asList(tmp, split2);
                            currentList.add(new Goal.Pair(a, lowers, new SimpleTransformation.Add(tmp, split2)));
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
                        currentList.add(new Goal.Pair(a, Arrays.asList(split1, split2), new SimpleTransformation.Add(split1, split2)));
                    }


                }

            }
            return currentList.remove(currentList.size()-1);
        }


    }

    static List<Tuple<Distance, Goal>> getAtomDistanceList(Goal a, Goal b, boolean diagonal) {
        Map<Distance, Goal.Factory> distanceMap = new HashMap<>();
        for (Iterator<Tuple<Atom, Integer>> ita = a.uniqueCountIterator(); ita.hasNext(); ) {
            Tuple<Atom, Integer> ta = ita.next();
            Atom atomA = ta.getA();
            for (Iterator<Tuple<Atom, Integer>> itb = b.uniqueCountIterator(); itb.hasNext(); ) {
                Tuple<Atom, Integer> tb = itb.next();
                Atom atomB = tb.getA();

                Distance d = new Distance(atomA, atomB);
                Goal.Factory goalFactory = distanceMap.getOrDefault(d, new Goal.Factory());
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
        List<Tuple<Distance, Goal>> list = new ArrayList<>(distanceMap.size());
        distanceMap.forEach((key, value) -> list.add(new Tuple<>(key, value.get())));
        if (!diagonal) {
            list.removeIf(t -> !(b.equals(t.getB())));
        }
        return list;
    }

}
