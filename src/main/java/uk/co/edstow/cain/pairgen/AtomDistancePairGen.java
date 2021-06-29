package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.structures.Bounds;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AtomDistancePairGen<G extends Kernel3DGoal<G>, T extends Transformation<R>, R extends Register> implements PairGen<G, T, R> {
    protected final Context<G, T, R> context;
    protected final GoalBag<G> goals;
    protected final Iterator<Tuple<Integer, Integer>> ijGetter;
    private int count;
    protected List<GoalPair<G,T,R>> currentList;
    private boolean twoD;

    public AtomDistancePairGen(GoalBag<G> goals, Context<G, T, R> context, Iterator<Tuple<Integer, Integer>> ijGetter, boolean twoD) {
        this.goals = goals;
        this.context = context;
        this.ijGetter = ijGetter;
        this.currentList = Collections.emptyList();
        this.twoD = twoD;
    }
    public AtomDistancePairGen(GoalBag<G> goals, Context<G, T, R> context, Iterator<Tuple<Integer, Integer>> ijGetter) {
        this(goals, context, ijGetter, false);
    }

    public AtomDistancePairGen(GoalBag<G> goals, Context<G, T, R> context) {
        this(goals, context, new SteppedCombinationIterator(goals.size()), false);
    }

    public AtomDistancePairGen(GoalBag<G> goals, Context<G, T, R> context, boolean twoD) {
        this(goals, context, new SteppedCombinationIterator(goals.size()), twoD);
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
            List<Item> inList;
            if(twoD) {
                inList = getAtomDistanceList2D(a, b, diagonal);
            } else {
                inList = getAtomDistanceList3D(a, b, diagonal);
            }
            List<Item> outList = new ArrayList<>();
            inList.sort(atomDistanceComparator);
            addPairs(a, diagonal, inList, outList);
            currentList = outList.stream().map((Item i) -> i.pair).collect(Collectors.toList());

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



    protected final List<Item> getAtomDistanceList3D(G a, G b, boolean diagonal) {

        Bounds aBounds = a.bounds();
        Bounds bBounds = b.bounds();
        int maxdx = bBounds.getXMax()-aBounds.getXMin();
        int mindx = bBounds.getXMin()-aBounds.getXMax();
        int xSize = 1+ maxdx - mindx;
        int maxdy = bBounds.getYMax()-aBounds.getYMin();
        int mindy = bBounds.getYMin()-aBounds.getYMax();
        int ySize = 1+ maxdy - mindy;
        int maxdz = bBounds.getZMax()-aBounds.getZMin();
        int mindz = bBounds.getZMin()-aBounds.getZMax();
        int zSize = 1+ maxdz - mindz;
        Kernel3DGoal.Kernel3DGoalFactory<G>[][][][] distanceArray = new Kernel3DGoal.Kernel3DGoalFactory[2][xSize][ySize][zSize];
        int fillCount = 0;
        for (Iterator<Tuple<Kernel3DGoal.Coord, Integer>> ita = a.uniqueCountIterator(); ita.hasNext(); ) {
            Tuple<Kernel3DGoal.Coord, Integer> ta = ita.next();
            Kernel3DGoal.Coord coordA = ta.getA();
            for (Iterator<Tuple<Kernel3DGoal.Coord, Integer>> itb = b.uniqueCountIterator(); itb.hasNext(); ) {
                Tuple<Kernel3DGoal.Coord, Integer> tb = itb.next();
                Kernel3DGoal.Coord coordB = tb.getA();

                Distance d = new Distance(coordA, coordB);
                int negate = ((ta.getB()>0 && tb.getB()<0) || (ta.getB()<0 && tb.getB()>0))?1:0;
                int xPos = d.x - mindx;
                int yPos = d.y - mindy;
                int zPos = d.z - mindz;
                Kernel3DGoal.Kernel3DGoalFactory<G> goalFactory = distanceArray[negate][xPos][yPos][zPos];
                if(goalFactory == null){
                    goalFactory = a.newFactory();
                    distanceArray[negate][xPos][yPos][zPos] = goalFactory;
                    fillCount++;
                }
                int count = Math.min(Math.abs(ta.getB()), Math.abs(tb.getB()));
                if (diagonal && d.isZero()){
                    count /= 2;
                }
                goalFactory.add(coordB.x, coordB.y, coordB.z, ta.getB()>0?count:-count);
            }
        }
        List<Item> list = new ArrayList<>(fillCount);
        for (int n = 0; n < 2; n++) {
            Kernel3DGoal.Kernel3DGoalFactory<G>[][][] factories3 = distanceArray[n];
            for (int x = 0; x < xSize; x++) {
                Kernel3DGoal.Kernel3DGoalFactory<G>[][] factories2 = factories3[x];
                for (int y = 0; y < ySize; y++) {
                    Kernel3DGoal.Kernel3DGoalFactory<G>[] factories1 = factories2[y];
                    for (int z = 0; z < zSize; z++) {
                        Kernel3DGoal.Kernel3DGoalFactory<G> factory = factories1[z];
                        if (factory != null) {
                            list.add(new Item(a, b, new Distance(x+mindx, y+mindy, z+mindz), n==1, factory.get()));
                        }
                    }
                }
            }
        }
        if (!diagonal) {
            list.removeIf(t -> !(b.same(t.to)));
        }
        list.removeIf(t->t.to.totalI()==0);
        return list;
    }

    protected final List<Item> getAtomDistanceList2D(G a, G b, boolean diagonal) {

        Bounds aBounds = a.bounds();
        Bounds bBounds = b.bounds();
        int maxdx = bBounds.getXMax()-aBounds.getXMin();
        int mindx = bBounds.getXMin()-aBounds.getXMax();
        int xSize = 1+ maxdx - mindx;
        int maxdy = bBounds.getYMax()-aBounds.getYMin();
        int mindy = bBounds.getYMin()-aBounds.getYMax();
        int ySize = 1+ maxdy - mindy;
        Kernel3DGoal.Kernel3DGoalFactory<G>[][][] distanceArray = new Kernel3DGoal.Kernel3DGoalFactory[2][xSize][ySize];
        int fillCount = 0;
        for (Iterator<Tuple<Kernel3DGoal.Coord, Integer>> ita = a.uniqueCountIterator(); ita.hasNext(); ) {
            Tuple<Kernel3DGoal.Coord, Integer> ta = ita.next();
            Kernel3DGoal.Coord coordA = ta.getA();
            for (Iterator<Tuple<Kernel3DGoal.Coord, Integer>> itb = b.uniqueCountIterator(); itb.hasNext(); ) {
                Tuple<Kernel3DGoal.Coord, Integer> tb = itb.next();
                Kernel3DGoal.Coord coordB = tb.getA();

                Distance d = new Distance(coordA, coordB);
                if (d.z == 0) {
                    int negate = ((ta.getB()>0 && tb.getB()<0) || (ta.getB()<0 && tb.getB()>0))?1:0;
                    int xPos = d.x - mindx;
                    int yPos = d.y - mindy;
                    Kernel3DGoal.Kernel3DGoalFactory<G> goalFactory = distanceArray[negate][xPos][yPos];
                    if (goalFactory == null) {
                        goalFactory = a.newFactory();
                        distanceArray[negate][xPos][yPos] = goalFactory;
                        fillCount++;
                    }
                    int count = Math.min(Math.abs(ta.getB()), Math.abs(tb.getB()));
                    if (diagonal && d.isZero()) {
                        count /= 2;
                    }
                    goalFactory.add(coordB.x, coordB.y, coordB.z, tb.getB()>0? count : -count);
                }
            }
        }
        List<Item> list = new ArrayList<>(fillCount);
        for (int n = 0; n < 2; n++) {
            Kernel3DGoal.Kernel3DGoalFactory<G>[][] factories2 = distanceArray[n];
            for (int x = 0; x < xSize; x++) {
                Kernel3DGoal.Kernel3DGoalFactory<G>[] factories1 = factories2[x];
                for (int y = 0; y < ySize; y++) {
                    Kernel3DGoal.Kernel3DGoalFactory<G> factory = factories1[y];
                    if (factory != null) {
                        list.add(new Item(a, b, new Distance(x+mindx, y+mindy, 0), n==1, factory.get()));
                    }
                }
            }
        }
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
