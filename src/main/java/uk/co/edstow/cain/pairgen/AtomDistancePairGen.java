package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.atomGoal.Atom;
import uk.co.edstow.cain.goals.atomGoal.pairGen.Distance;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;

public abstract class AtomDistancePairGen<G extends Kernel3DGoal<G>, T extends Transformation<R>, R extends Register> implements PairGen<G, T, R> {
    protected final Context<G, T, R> context;
    protected final GoalBag<G> goals;
    protected final Iterator<Tuple<Integer, Integer>> ijGetter;
    private int count;
    protected List<GoalPair<G,T,R>> currentList;


    public AtomDistancePairGen(GoalBag<G> goals, Context<G, T, R> context) {
        this.goals = goals;
        this.context = context;
        this.ijGetter = new SteppedCombinationIterator(goals.size());
        this.currentList = Collections.emptyList();
    }

    public AtomDistancePairGen(GoalBag<G> goals, Context<G, T, R> context, Iterator<Tuple<Integer, Integer>> ijGetter) {
        this.goals = goals;
        this.context = context;
        this.ijGetter = ijGetter;
        this.currentList = Collections.emptyList();
    }

    @SuppressWarnings("WeakerAccess")
    protected void fillCurrentList() {
        while (currentList.isEmpty()) {
            if (!ijGetter.hasNext()) {
                return;
            }
            Tuple<Integer, Integer> ij = ijGetter.next();
            G a = goals.get(ij.getA());
            G b = goals.get(ij.getB());

            boolean diagonal = ij.getA().equals(ij.getB());
            List<Item> inList = getAtomDistanceList(a, b, diagonal);
            List<Item> outList = new ArrayList<>();
            inList.sort(atomDistanceComparator);
            addPairs(a, diagonal, inList, outList);
            outList.forEach(item -> currentList.add(item.pair));

        }
    }

    protected abstract void addAtomDistancePairs(Item item, List<Item> outList);
    protected abstract void addAtomDistanceDiagonalPairs(Item item, List<Item> outList);
    protected abstract void addDirectTransformation(G a, List<Item> outList);

    @SuppressWarnings("WeakerAccess")
    protected final void addPairs(G a, boolean diagonal, List<Item> inList, List<Item> outList) {
        if (!diagonal) {
            for (Item item : inList) {
                addAtomDistancePairs(item, outList);
            }
        } else {
            addDirectTransformation(a, outList);

            for (Item item : inList) {
                addAtomDistanceDiagonalPairs(item, outList);
            }
        }
    }

    

    @Override
    public final GoalPair<G,T,R> next() {
        count++;
        fillCurrentList();
        return currentList.isEmpty() ? null : currentList.remove(currentList.size() - 1);
    }

    @Override
    public final int getNumber() {
        return count;
    }



    protected final List<Item> getAtomDistanceList(G a, G b, boolean diagonal) {
        Map<Tuple<Distance, Boolean>, Kernel3DGoal.Kernel3DGoalFactory<G>> distanceMap = new HashMap<>();
        for (Iterator<Tuple<Atom, Integer>> ita = a.uniqueCountIterator(); ita.hasNext(); ) {
            Tuple<Atom, Integer> ta = ita.next();
            Atom atomA = ta.getA();
            for (Iterator<Tuple<Atom, Integer>> itb = b.uniqueCountIterator(); itb.hasNext(); ) {
                Tuple<Atom, Integer> tb = itb.next();
                Atom atomB = tb.getA();

                Distance d = new Distance(atomA, atomB);
                boolean negate = atomA.positive ^ atomB.positive;
                Tuple<Distance, Boolean> key = new Tuple<>(d, negate);
                Kernel3DGoal.Kernel3DGoalFactory<G> goalFactory = distanceMap.getOrDefault(key, a.newFactory());
                int count = Math.min(ta.getB(), tb.getB());
                if (diagonal && d.isZero()){
                    count /= 2;
                }
                for (int i = 0; i < count; i++) {
                    goalFactory.add(atomB.x, atomB.y, atomB.z, atomB.positive?1:-1);
                }
                distanceMap.put(key, goalFactory);

            }
        }
        List<Item> list = new ArrayList<>(distanceMap.size());
        distanceMap.forEach((key, value) -> list.add(new Item(a,b, key.getA(), key.getB(), value.get())));
        if (!diagonal) {
            list.removeIf(t -> !(b.same(t.to)));
        }
        list.removeIf(t->t.to.totalI()==0);
        return list;
    }


    protected class Item {
        public final GoalPair<G,T,R> pair; // the goal-pair produced to best make use of the sub-goal (set by concrete implementation)

        public double cost; // the cost calculated for this item to select bigger, longer moves first

        final public G a;// a goal from goals
        final public G b;// another goal from goals (possibly a==b) (null if we want a direct Transformation)
        // distance the sub-goal of A is translated such that it is a sub-goal of b,
        // if this is a directTransformation this should be the distance from 'to' to a
        final public Distance distance;

        // Weather or not the sub-goal of a is negated as-well as translated to be a sub-goal of b (or if this is a
        // direct Transformation, weather 'to' is a negated form of 'a'
        final public boolean negate;
        final public G to; // the sub-goal of b, or if this is a direct Transformation then this is the lower of 'pair'

        public Item(Item item, GoalPair<G,T,R> pair) {
            assert item.pair==null;
            this.pair = pair;
            this.cost = 0;
            this.a = item.a;
            this.b = item.b;
            this.distance = item.distance;
            this.negate = item.negate;
            this.to = item.to;
        }

        private Item(G a, G b, Distance distance, boolean negate, G to) {
            this.pair=null;
            this.a = a;
            this.b = b;
            this.distance = distance;
            this.negate = negate;
            this.to = to;
        }

        public Item(G upper, G lower, T transformation) {
            this.pair = new GoalPair<>(upper, lower, transformation);
            this.a = upper;
            this.b = upper;
            this.to = lower;
            this.distance = new Distance(0,0,0);
            this.negate = false;
        }

        public Item(G upper, G lower, T transformation, Distance distance, boolean negate) {
            this.pair = new GoalPair<>(upper, lower, transformation);
            this.a = upper;
            this.b = upper;
            this.to = lower;
            this.distance = distance;
            this.negate = negate;
        }
    }

    private static final Comparator<AtomDistancePairGen<?,?,?>.Item> atomDistanceComparator = Comparator.comparingInt((AtomDistancePairGen<?,?,?>.Item i) -> i.to.totalI()).thenComparingInt(i -> -i.distance.manhattanXY());


    public static class SteppedCombinationIterator implements Iterator<Tuple<Integer, Integer>> {
        int ii = 0;
        int jj = 0;
        int dia = -1;
        final int maxSize;
        public SteppedCombinationIterator(int maxSize) {
            this.maxSize = maxSize;
            updateIJ();
        }
        private int getI(){
            if (dia >=0){
                return dia;
            }
            return ii;
        }
        private int getJ(){
            if (dia >=0){
                return dia;
            }
            return jj - ii;
        }
        private void updateIJ(){
            if (dia < 0) {
                do{
                    if (ii < Math.min(jj, maxSize - 1)) {
                        ii++;
                    } else {
                        jj++;
                        int d = jj - maxSize;
                        if (d < 0) {
                            ii = 0;
                        } else {
                            ii = d + 1;
                        }
                    }
                }
                while(jj-ii == ii);
                if (jj-ii >= maxSize || ii >= maxSize) {
                    dia++;
                }
            } else {
                dia++;
            }

        }

        @Override
        public boolean hasNext() {
            return getJ() < maxSize && getI() < maxSize;
        }

        @Override
        public Tuple<Integer, Integer> next() {
            Tuple<Integer, Integer> t = new Tuple<>(getI(), getJ());
            updateIJ();
            return t;
        }
    }

    public static class PlainCombinationIterator implements Iterator<Tuple<Integer, Integer>> {
        int ii = 0;
        int jj = 0;

        final int maxSize;
        public PlainCombinationIterator(int maxSize) {
            this.maxSize = maxSize;
        }
        private void updateIJ(){
            ii++;
            if(ii >= maxSize){
                jj++;
                ii=0;
            }
        }

        @Override
        public boolean hasNext() {
            return jj < maxSize;
        }

        @Override
        public Tuple<Integer, Integer> next() {
            Tuple<Integer, Integer> t = new Tuple<>(ii, jj);
            updateIJ();
            return t;
        }
    }
}
