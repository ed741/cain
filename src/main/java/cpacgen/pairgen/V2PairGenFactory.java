package cpacgen.pairgen;

import cpacgen.Atom;
import cpacgen.Goal;
import cpacgen.ReverseSplit;
import cpacgen.Transformation;
import cpacgen.util.Bounds;
import cpacgen.util.Tuple;

import java.util.*;

public class V2PairGenFactory implements PairGenFactory{

    private static Comparator<Tuple<Distance, Goal>> entryComparator = Comparator.comparingInt((Tuple<Distance, Goal> t) -> t.getB().size()).thenComparingInt(t -> -t.getA().manhattan());

    private Bounds bounds;
    @Override
    public void init(ReverseSplit rs) {
        bounds = new Bounds(rs.getFinalGoals());

    }

    @Override
    public PairGen generatePairs(Goal.Bag goals) {
        return new V2PairGen(goals);
    }

    private class V2PairGen implements PairGen {
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

        private int geti(){
            if (dia >=0){
                return dia;
            }
            return ii;
        }
        private int getj(){
            if (dia >=0){
                return dia;
            }
            return jj - ii;
        }
        private void updateIJ(){
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
                //System.out.println("ii: " + ii + " jj: " + jj + " i: " + geti() + " j: " + getj());

                Map<Distance, Goal.Factory> distanceMap = new HashMap<>();
                for (Iterator<Atom> ita = a.uniqueIterator(); ita.hasNext(); ) {
                    Atom atomA = ita.next();
                    for (Iterator<Atom> itb = b.uniqueIterator(); itb.hasNext(); ) {
                        Atom atomB = itb.next();

                        Distance d = new Distance(atomA, atomB);
                        if (geti() == getj() && d.isZero()){
                            continue;
                        }
                        Goal.Factory goalFactory = distanceMap.getOrDefault(d, new Goal.Factory());
                        int count = Math.min(a.count(atomA), b.count(atomB));
                        for (int i = 0; i < count; i++) {
                            goalFactory.add(atomB);
                        }
                        distanceMap.put(d, goalFactory);

                    }
                }
                List<Tuple<Distance, Goal>> list = new ArrayList<>(distanceMap.size());
                distanceMap.forEach((key, value) -> list.add(new Tuple<>(key, value.get())));
                if (geti() != getj()) {
                    list.removeIf(t -> !(b.equals(t.getB())));
                }
                list.sort(entryComparator);
                if (geti() != getj()) {
                    for (Tuple<Distance, Goal> tuple : list) {
                        Goal tmp = tuple.getA().inverse().translate(tuple.getB());
                        if (tmp.equals(a)) {
                            Transformation.Move mov = new Transformation.Move(1, tuple.getA().majorDirection(), a);
                            currentList.add(new Goal.Pair(a, mov.applyForwards(), mov));
                        } else {
                            Goal split2 = a.without(tmp);
                            List<Goal> lowers = Arrays.asList(tmp, split2);
                            currentList.add(new Goal.Pair(b, lowers, new Transformation.Add(tmp, split2)));
                        }

                    }
                } else {
                    List<Tuple<Distance, Tuple<Goal, Goal>>> splits = new ArrayList<>();
//                    System.out.println(a.getCharTableString(false));
                    for (Tuple<Distance, Goal> tuple : list) {
                        Goal split1 = a.without(tuple.getB());
                        Goal split2 = tuple.getB();
                        splits.add(Tuple.triple(tuple.getA(), split1, split2));

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

    private class Distance {
        final int x, y, z;

        Distance(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Distance(Atom a, Atom b){
            this.x = b.x-a.x;
            this.y = b.y-a.y;
            this.z = b.z-a.z;
        }

        boolean isZero(){
            return x==0 && y==0 && z==0;
        }

        int manhattan() {
            return Math.abs(x)+Math.abs(y)+Math.abs(z);
        }

        @Override
        public String toString() {
            return "D:("+x+","+y+","+z+")";
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(x) ^ Integer.hashCode(y) ^ Integer.hashCode(z);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Distance && ((Distance) o).x == x && ((Distance) o).y == y && ((Distance) o).z == z;
        }

        Distance inverse(){
            return new Distance(-x, -y, -z);
        }

        Goal translate(Goal goal){
            Goal.Factory factory = new Goal.Factory();
            for (Atom a: goal) {
                factory.add(a.moved(x, y, z));
            }
            return factory.get();
        }

        public Transformation.Direction majorDirection() {
            int absX = Math.abs(x);
            int absY = Math.abs(y);
            int absZ = Math.abs(z);

            if (isZero()){
                return null;
            }
            if(absX >= absY && absX >= absZ){
                return x>0? Transformation.Direction.E : Transformation.Direction.W;
            }
            if(absY >= absX && absY >= absZ){
                return y>0? Transformation.Direction.N : Transformation.Direction.S;
            }
            if(absZ >= absX && absZ >= absY){
                throw new UnsupportedOperationException("Movement in Z not supported");
            }
            return null;
        }
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

        PairGen pg = g.generatePairs(goals);
        Goal.Pair p = pg.next();
        while(p != null){
            p = pg.next();
        }
    }

}
