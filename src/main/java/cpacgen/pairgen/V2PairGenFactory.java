package cpacgen.pairgen;

import cpacgen.Atom;
import cpacgen.Goal;
import cpacgen.ReverseSplit;
import cpacgen.Transformation;
import cpacgen.util.Tuple;

import java.util.*;

public class V2PairGenFactory implements PairGenFactory{

    static Comparator<Tuple<Distance, Goal>> entryComparator = Comparator.comparingInt((Tuple<Distance, Goal> t) -> t.getB().size()).thenComparingInt(t -> -t.getA().manhattan());


    @Override
    public Collection<Tuple<? extends Transformation, Goal>> applyAllUnaryOpForwards(Goal initialGoal, int depth) {
        return SimplePairGenFactory.applyAllUnaryOps(initialGoal);
    }

    @Override
    public void init(ReverseSplit rs) {
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
        List<Goal.Pair> currentList = new ArrayList<>();

        public V2PairGen(Goal.Bag goals) {
            this.goals = goals;
            ii = 0;
            jj = 0;
        }

        int geti(){
            if (dia >=0){
                return dia;
            }
            return ii;
        }
        int getj(){
            if (dia >=0){
                return dia;
            }
            return jj - ii;
        }
        protected void updateIJ(){
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
                if (getj() >= goals.size() || geti() >= goals.size()) {
                    return null;
                }
                //System.out.println("ii: " + ii + " jj: " + jj + " i: " + geti() + " j: " + getj());



                Goal a = goals.get(geti());
                Goal b = goals.get(getj());
                boolean diaganal = geti()==getj();
                //System.out.println("ii: " + ii + " jj: " + jj + " i: " + geti() + " j: " + getj());

                List<Tuple<Distance, Goal>> list = getAtomDistanceList(a, b, diaganal);
                list.sort(entryComparator);
                if (!diaganal) {
                    for (Tuple<Distance, Goal> tuple : list) {
                        Goal tmp = tuple.getA().inverse().translate(tuple.getB());
                        if (tmp.equals(a)) {
                            Transformation.Move mov = new Transformation.Move(1, tuple.getA().majorDirection().opposite(), a);
                            currentList.add(new Goal.Pair(a, mov.applyForwards(), mov));
                        } else {
                            Goal split2 = a.without(tmp);
                            List<Goal> lowers = Arrays.asList(tmp, split2);
                            currentList.add(new Goal.Pair(a, lowers, new Transformation.Add(tmp, split2)));
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
                        currentList.add(new Goal.Pair(a, Arrays.asList(split1, split2), new Transformation.Add(split1, split2)));
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


    public static void main(String[] args) {
        PairGenFactory g = new V2PairGenFactory();
        Goal.Bag goals = new Goal.Bag();
        for (int i = 0; i < 4; i++) {
            goals.add(new Goal.Factory(new Atom(i,0,0, true)).get());
        }
        //goals.add(new Goal.Factory(new Atom(-1,1,0), new Atom(-1,0,0), new Atom(-1,-1,0), new Atom(-1,-2,0)).get());//, new Atom(-1,0,0), new Atom(1,1,0), new Atom(1,0,0), new Atom(1,0,0), new Atom(1,-1,0)).get());
        int[][] multi = new int[][]{
                { 0, 1, 2, 1, 0},
                { 1, 4, 6, 4, 1},
                { 2, 6, 10, 6, 2},
                { 1, 4, 6, 4, 1},
                { 0, 1, 2, 1, 0}
        };
        //goals.add(new Goal.Factory(multi).get());
        //goals.add(new Goal.Factory(new Atom(0,0,0), new Atom(0,0,0)).get());

        PairGen pg = g.generatePairs(goals, 0);
        Goal.Pair p = pg.next();
        while(p != null){
            p = pg.next();
        }
    }

}
