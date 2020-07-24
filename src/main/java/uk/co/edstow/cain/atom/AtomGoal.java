package uk.co.edstow.cain.atom;

import uk.co.edstow.cain.structures.Bounds;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;


public class AtomGoal implements List<Atom>, Goal<AtomGoal> {

    private final List<Atom> list;

    @SuppressWarnings("WeakerAccess")
    public AtomGoal(List<Atom> list) {
        ArrayList<Atom> l = new ArrayList<>(list);
        l.sort(Atom.comparator);
        this.list = Collections.unmodifiableList(simplifySorted(l));
    }

    public AtomGoal(AtomGoal goal) { // No need to sort as AtomGoal is already correctly sorted
        ArrayList<Atom> l = new ArrayList<>(goal);
        this.list = Collections.unmodifiableList(l);
    }

    public AtomGoal(Atom... atoms) {
        List<Atom> l = new ArrayList<>(Arrays.asList(atoms));
        l.sort(Atom.comparator);
        this.list = Collections.unmodifiableList(simplifySorted(l));
    }

    @SuppressWarnings("SameParameterValue")
    private AtomGoal(ArrayList<Atom> list, boolean check){
        if(check){
            ArrayList<Atom> l = new ArrayList<>(list);
            l.sort(Atom.comparator);
            this.list = Collections.unmodifiableList(simplifySorted(l));
        } else {
            this.list = Collections.unmodifiableList(list);
        }
    }

    private static List<Atom> simplifySorted(List<Atom> list){
        for (int i = list.size()-2; i >= 0; i--) {
            if (i+1 < list.size()) {
                Atom last = list.get(i + 1);
                Atom c = list.get(i);
                if (last != null && last.samePos(c) && last.positive ^ c.positive) {
                    list.remove(i + 1);
                    list.remove(i);
                }
            }
        }
        return list;
    }
    static String goalStringN(AtomGoal g){
        return g!=null? g.toStringN() : "null";
    }

    public String toStringN() {
        StringBuilder sb = new StringBuilder("[");
        for (Atom a: this){
            sb.append(a.toStringN());
            if (a != get(size()-1)) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String toString() {
        return toStringN();
    }

    public AtomGoal without(AtomGoal goal) {
        AtomGoal.Factory factory = new AtomGoal.Factory();
        List<Atom> tmp = new ArrayList<>(goal);
        for (Atom a: this){
            if (!tmp.remove(a)){
                factory.add(a);
            }
        }
        return factory.get();
    }

    @Override
    public boolean same(AtomGoal g){
        if(g == this){return true;}
        if(g.list.size() != list.size()){
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            if(!g.list.get(i).equals(list.get(i))){
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equivalent(AtomGoal g){
        return this == g;
    }

    @Override
    public double total() {
        return atomCount();
    }

    public AtomGoal negative(){
        ArrayList<Atom> list = new ArrayList<>();
        this.forEach(a->list.add(a.negate()));
        return new AtomGoal(list, false);
    }

    public AtomGoal translated(int x, int y, int z){
        ArrayList<Atom> list = new ArrayList<>();
        this.forEach(atom -> list.add(atom.moved(x, y, z)));
        return new AtomGoal(list, false);
    }


    /**
     * @return all possible sub Goals (subsets) of this goal, excluding the empty AtomGoal.
     */
    public List<AtomGoal> allSplitsRecursive(){
        List<AtomGoal> out = r_AllSplits();
        out.remove(0);
        return out;
    }

    private List<AtomGoal> r_AllSplits(){
        if (isEmpty()){
            List<AtomGoal> out = new ArrayList<>();
            out.add(new AtomGoal());
            return out;
        }
        ArrayList<Atom> sub = new ArrayList<>(this);
        Atom a = sub.remove(0);
        Collection<AtomGoal> subSplits = new AtomGoal(sub).r_AllSplits();
        List<AtomGoal> out = new ArrayList<>();
        for (AtomGoal g: subSplits){
            if (out.stream().noneMatch(g::same)){
                out.add(g);
            }
            AtomGoal.Factory factory = new AtomGoal.Factory(g);
            factory.add(a);
            AtomGoal gp = factory.get();
            if (out.stream().noneMatch(gp::same)){
                out.add(gp);
            }
        }
        return out;
    }

    public List<AtomGoal> allSplits(){
        try {
            List<List<Atom>> lists = new ArrayList<>();
            lists.add(new ArrayList<>());
            for (Iterator<Tuple<Atom, Integer>> iterator = uniqueCountIterator(); iterator.hasNext(); ) {
                Tuple<Atom, Integer> tuple = iterator.next();
                Atom atom = tuple.getA();
                int count = tuple.getB();

                int length = lists.size();
                for (int i = 0; i < length; i++) {
                    List<Atom> currentList = lists.get(i);
                    for (int j = 1; j <= count; j++) {
                        List<Atom> l = new ArrayList<>(currentList);
                        l.add(atom);
                        currentList = l;
                        lists.add(l);
                    }
                }
            }
            ArrayList<AtomGoal> out = new ArrayList<>();
            for (int i = 1; i < lists.size(); i++) {
                List<Atom> atoms = lists.get(i);
                out.add(new AtomGoal(atoms));
            }
            return out;
        } catch (OutOfMemoryError memoryError){
            System.out.println("OUT OF MEM size:" + this.list.size());
            System.exit(-1);
            return null;
        }
    }

    public int divide(AtomGoal goal){
        if(list.isEmpty() || goal.isEmpty()){
            return 0;
        }
        int div = -1;
        Iterator<Tuple<Atom, Integer>> thisIt = this.uniqueCountIterator();
        Iterator<Tuple<Atom, Integer>> goalIt = goal.uniqueCountIterator();
        while(thisIt.hasNext() && goalIt.hasNext()){
            Tuple<Atom, Integer> ta = thisIt.next();
            Tuple<Atom, Integer> tb = goalIt.next();

            if (!ta.getA().equals(tb.getA())){
                return 0;
            }
            if (tb.getB() > ta.getB()){
                return 0;
            }
            int mod = ta.getB() % tb.getB();
            if (mod != 0){
                return 0;
            }
            if(div < 0){
                div = ta.getB()/tb.getB();
            }
            if(ta.getB()/tb.getB() != div){
                return 0;
            }
        }
        if (thisIt.hasNext() || goalIt.hasNext()){
            return 0;
        }
        return div;
    }


    // returns the smallest number of atoms for the same location
    public int minimumCount() {
        if(list.isEmpty()){
            return 0;
        }
        int cursor = 0;
        int minCount = Integer.MAX_VALUE;
        Atom a = list.get(cursor++);
        int count = 1;
        while(cursor < list.size()){
            if (minCount == 1){
                return minCount;
            }

            if(a.equals(list.get(cursor))) {
                count++;
            } else {
                minCount = Math.min(minCount, count);
                count = 1;
                a = list.get(cursor);
            }
            cursor++;
        }
        minCount = Math.min(minCount, count);
        return minCount;

    }


    public int maximumCount() {
        if(list.isEmpty()){
            return 0;
        }
        int cursor = 0;
        int maxCount = Integer.MIN_VALUE;
        Atom a = list.get(cursor++);
        int count = 1;
        while(cursor < list.size()){
            if(a.equals(list.get(cursor))) {
                count++;
            } else {
                maxCount = Math.max(maxCount, count);
                count = 1;
                a = list.get(cursor);
            }
            cursor++;
        }
        maxCount = Math.max(maxCount, count);
        return maxCount;
    }

    public AveragePosition getAveragePos(){
        double x=0, y=0, z=0;
        for (Atom a : list) {
            x += a.x;
            y += a.y;
            z += a.z;
        }
        return new AveragePosition(x/list.size(), y/list.size(), z/list.size());
    }

    public boolean allSame() {
        if(isEmpty()){
            return true;
        }
        Atom a = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            if(!a.equals(list.get(i))){
                return false;
            }
        }
        return true;
    }

    public AtomGoal subtract(AtomGoal a) {
        return new AtomGoal.Factory(list).subAll(a).get();
    }



    public static class AveragePosition {
        public final double x, y, z;

        @SuppressWarnings("WeakerAccess")
        public AveragePosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public String[][] getCharTable(boolean topBorder, boolean bottomBorder, boolean centreDot, boolean colourNeg){
        Bounds b = new AtomBounds(AtomBounds.BoundsFromGoal(this), new Atom(0,0,0, true));
        return getCharTable(b, topBorder, bottomBorder, centreDot, colourNeg);
    }


    public String[][] getCharTable(Bounds b, boolean topBorder, boolean bottomBorder, boolean centreDot, boolean colourNeg) {

        int width = 1+b.getXMax() - b.getXMin();
        int height = 1+b.getYMax() - b.getYMin();
        int depth = 1+b.getZMax() - b.getZMin();
        int[][][] intArray = new int[width][height][depth];
        int ixZero = -b.getXMin();
        int iyZero = -b.getYMin();
        int izZero = -b.getZMin();
        {


            Iterator<Tuple<Atom, Integer>> it = this.uniqueCountIterator();
            while (it.hasNext()) {
                Tuple<Atom, Integer> t = it.next();
                int x = ixZero + t.getA().x;
                int y = iyZero + t.getA().y;
                int z = izZero + t.getA().z;

                intArray[x][y][z] = t.getA().positive? t.getB(): -t.getB();
            }
        }
        String[][] tableArray = new String[height + 2][width + 2];
        {
            int xZero = 1 - b.getXMin();
            int yZero = 1 - b.getYMin();
            for (int x = b.getXMin() - 1; x <= b.getXMax() + 1; x++) {
                for (int y = b.getYMin() - 1; y <= b.getYMax() + 1; y++) {
                    if (b.getXMin() <= x && x <= b.getXMax() && b.getYMin() <= y && y <= b.getYMax()) {

                        StringBuilder sb = new StringBuilder();
                        boolean zeros = true;
                        if(depth>1){sb.append('[');}
                        for (int z = 0; z < intArray[ixZero + x][iyZero + y].length; z++) {
                            int count = intArray[ixZero + x][iyZero + y][z];
                            zeros &= count == 0;
                            boolean neg = intArray[ixZero + x][iyZero + y][z] < 0;
                            if (neg) {
                                sb.append(colourNeg ? "\u001B[34m" : "-");//blue
                            }
                            sb.append(Math.abs(count));
                            if (neg && colourNeg) {
                                sb.append("\u001B[0m");//reset

                            }
                            if(z<intArray[ixZero + x][iyZero + y].length-1){
                                sb.append(",");
                            }
                        }
                        if(depth>1){sb.append(']');}
                        if(centreDot && zeros && x==0 && y==0){
                            if(depth == 1) {
                                tableArray[yZero + y][xZero + x] = ".";
                            } else {
                                StringBuilder dot = new StringBuilder("[");
                                for(int k = 0; k < depth-1;k++){
                                    dot.append(" ");
                                }
                                dot.append(".");
                                for(int k = 0; k < depth-1;k++){
                                    dot.append(" ");
                                }
                                dot.append("]");
                                tableArray[yZero + y][xZero + x] = dot.toString();
                            }
                        } else {
                            tableArray[yZero + y][xZero + x] = sb.toString();
                        }
                    } else if (x == 0) {
                        tableArray[yZero + y][xZero + x] = "|";
                    } else if (y == 0) {
                        tableArray[yZero + y][xZero + x] = "-";
                    } else if (yZero + y == 0 && bottomBorder) {
                        tableArray[yZero + y][xZero + x] = "v";
                    } else if (yZero + y == height + 1 && topBorder) {
                        tableArray[yZero + y][xZero + x] = "v";
                    } else {
                        tableArray[yZero + y][xZero + x] = "+";
                    }
                }
            }
        }

        return tableArray;
    }

    @Override
    public Bounds bounds() {
        return AtomBounds.BoundsFromGoal(this);
    }

    @Override
    public int compareTo(AtomGoal goal) {
        return Integer.compare(goal.size(), this.size());
    }

    public int atomCount() {
        return size();
    }

    public int countUnique() {
        if(list.isEmpty()){
            return 0;
        }
        int cursor = 0;
        Atom a = list.get(cursor++);
        int count = 1;
        while(cursor < list.size()){
            Atom b = list.get(cursor);
            if(!a.equals(b)) {
                count++;
                a = b;
            }
            cursor++;
        }
        return count;
    }

    public boolean hasSubGoal(AtomGoal goal) {
        if(goal.size() > list.size()){
            return false;
        }
        int i = 0;
        int j = 0;
        while(i < list.size() && j < goal.size()){
            switch(list.get(i).compareTo(goal.get(j))){
                case -1: i++; break;
                case  0: i++; j++; break;
                case  1: return false;
            }
        }
        return j == goal.size();
    }


    public static class Factory {
        private ArrayList<Atom> list;

        public Factory(List<Atom> list) {
            this.list = new ArrayList<>(list);
        }

        public Factory() {
            this.list = new ArrayList<>();
        }

        public Factory(Atom... atoms) {
            this.list = new ArrayList<>(Arrays.asList(atoms));
        }

        public Factory(AtomGoal... goals) {
            this.list = new ArrayList<>();
            for (AtomGoal goal : goals) {
                this.list.addAll(goal);
            }
        }


        public Factory(int z, int[][] matrix) {
            this(z, matrix, 1);
        }

        public Factory(int[][] matrix) {
            this(0, matrix);
        }
        public Factory(int[][] matrix, int multiples) {
            this(0, matrix, multiples);
        }
        public Factory(int z, int[][] matrix, int multiples) {
            this();
            add(z, matrix, multiples);
        }


        public AtomGoal.Factory add(int z, int[][] matrix, int multiples){
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    int count = Math.abs(matrix[i][j]);
                    boolean positive = matrix[i][j]>0;
                    for (int k = 0; k < count*multiples; k++) {
                        list.add(new Atom(j-(matrix[i].length/2), (matrix.length/2)-i, z, positive));
                    }
                }
            }
            return this;
        }

        public AtomGoal get(){
            AtomGoal g = new AtomGoal(list);
            list = null;
            return g;
        }

        public AtomGoal.Factory add(Atom a){
            list.add(a); return this;
        }

        public AtomGoal.Factory addAll(Collection<Atom> a){
            list.addAll(a); return this;
        }

        public AtomGoal.Factory sub(Atom a){
            list.add(a.negate()); return this;
        }

        public AtomGoal.Factory subAll(Collection<Atom> b) {
            b.forEach(atom -> list.add(atom.negate())); return this;
        }

        public AtomGoal.Factory add(Atom a, int i) {
            for (int j = 0; j < i; j++) {
                list.add(a);
            }
            return this;
        }

        public AtomGoal.Factory add(int[] position, int i) {
            assert(position.length==3);
            int abs = Math.abs(i);
            Atom a = new Atom(position[0], position[1], position[2], i>=0);
            for (int j = 0; j < abs; j++) {
                list.add(a);
            }
            return this;
        }
    }


    public Iterator<Atom> uniqueIterator() {
        return new Iterator<Atom>() {
            int cursor = 0;
            @Override
            public boolean hasNext() {
                return cursor < list.size();
            }

            @Override
            public Atom next() {
                Atom a = list.get(cursor++);
                while(cursor < list.size() && a.equals(list.get(cursor))){
                    cursor++;
                }
                return a;
            }
        };
    }

    public Iterator<Tuple<Atom, Integer>> uniqueCountIterator() {
        return new Iterator<Tuple<Atom, Integer>>() {
            int cursor = 0;
            @Override
            public boolean hasNext() {
                return cursor < list.size();
            }

            @Override
            public Tuple<Atom, Integer> next() {
                Atom a = list.get(cursor++);
                int count = 1;
                while(cursor < list.size() && a.equals(list.get(cursor))){
                    cursor++;
                    count++;
                }
                return new Tuple<>(a, count);
            }
        };
    }

    public int count(Atom a){
        int low = 0;
        int high = list.size()-1;
        int index = Integer.MAX_VALUE;

        while (low <= high) {
            int mid = (low + high) / 2;
            int c = list.get(mid).compareTo(a);
            if (c<0) {
                low = mid + 1;
            } else if (c>0) {
                high = mid - 1;
            } else {
                index = mid;
                break;
            }
        }
        if (index >= list.size()){
            return 0;
        }
        int count = 1;
        int idxUp = index +1;
        while(idxUp < list.size() && list.get(idxUp).equals(a)){
            count++;
            idxUp++;
        }
        int idxDown = index-1;
        while(idxDown >= 0 && list.get(idxDown).equals(a)){
            count++;
            idxDown--;
        }
        return count;

    }



    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public Iterator<Atom> iterator() {
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @Override
    public <T> T[] toArray(T[] ts) {
        return list.toArray(ts);
    }

    @Override
    public boolean add(Atom atom) {
        throw new UnsupportedOperationException("Goals are immutable");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Goals are immutable");
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return list.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends Atom> collection) {
        throw new UnsupportedOperationException("Goals are immutable");
    }

    @Override
    public boolean addAll(int i, Collection<? extends Atom> collection) {
        throw new UnsupportedOperationException("Goals are immutable");
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Goals are immutable");
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Goals are immutable");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Goals are immutable");
    }

    @Override
    public Atom get(int i) {
        return list.get(i);
    }

    @Override
    public Atom set(int i, Atom atom) {
        throw new UnsupportedOperationException("Goals are immutable");
    }

    @Override
    public void add(int i, Atom atom) {
        throw new UnsupportedOperationException("Goals are immutable");
    }

    @Override
    public Atom remove(int i) {
        throw new UnsupportedOperationException("Goals are immutable");
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<Atom> listIterator() {
        return list.listIterator();
    }

    @Override
    public ListIterator<Atom> listIterator(int i) {
        return list.listIterator(i);
    }

    @Override
    public List<Atom> subList(int i, int i1) {
        return list.subList(i, i1);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AtomGoal && same((AtomGoal) o);
    }

    @SuppressWarnings("WeakerAccess")
    public static class AtomBounds implements Bounds {
        public final int xMax;
        public final int xMin;
        public final int yMax;
        public final int yMin;
        public final int zMax;
        public final int zMin;

        @Override
        public int getXMax() {
            return xMax;
        }
        @Override
        public int getXMin() {
            return xMin;
        }
        @Override
        public int getYMax() {
            return yMax;
        }
        @Override
        public int getYMin() {
            return yMin;
        }
        @Override
        public int getZMax() {
            return zMax;
        }
        @Override
        public int getZMin() {
            return zMin;
        }

        public AtomBounds(int xMax, int xMin, int yMax, int yMin, int zMax, int zMin) {
            this.xMax = xMax;
            this.xMin = xMin;
            this.yMax = yMax;
            this.yMin = yMin;
            this.zMax = zMax;
            this.zMin = zMin;
        }

        public AtomBounds(Collection<? extends Collection<Atom>> cs) {
            int xMax = Integer.MIN_VALUE;
            int xMin = Integer.MAX_VALUE;
            int yMax = Integer.MIN_VALUE;
            int yMin = Integer.MAX_VALUE;
            int zMax = Integer.MIN_VALUE;
            int zMin = Integer.MAX_VALUE;

            boolean found = false;
            for (Collection<? extends Atom> c: cs){
                for (Atom a: c){
                    found = true;
                    xMax = max(xMax, a.x);
                    xMin = min(xMin, a.x);
                    yMax = max(yMax, a.y);
                    yMin = min(yMin, a.y);
                    zMax = max(zMax, a.z);
                    zMin = min(zMin, a.z);
                }
            }
            if(found) {
                this.xMax = xMax;
                this.xMin = xMin;
                this.yMax = yMax;
                this.yMin = yMin;
                this.zMax = zMax;
                this.zMin = zMin;
            } else {
                this.xMax = 0;
                this.xMin = 0;
                this.yMax = 0;
                this.yMin = 0;
                this.zMax = 0;
                this.zMin = 0;
            }
        }

        public static AtomBounds BoundsFromGoal(Collection<Atom> c) {
            int xMax = Integer.MIN_VALUE;
            int xMin = Integer.MAX_VALUE;
            int yMax = Integer.MIN_VALUE;
            int yMin = Integer.MAX_VALUE;
            int zMax = Integer.MIN_VALUE;
            int zMin = Integer.MAX_VALUE;

            for (Atom a: c){
                xMax = max(xMax, a.x);
                xMin = min(xMin, a.x);
                yMax = max(yMax, a.y);
                yMin = min(yMin, a.y);
                zMax = max(zMax, a.z);
                zMin = min(zMin, a.z);
            }

            return new AtomBounds(xMax, xMin, yMax, yMin, zMax, zMin);
        }

        @Override
        public Bounds includeCentre() {
            return new AtomBounds(this, new Atom(0, 0, 0, true));
        }

        public AtomBounds(Bounds b, Atom a){
                xMax = max(b.getXMax(), a.x);
                xMin = min(b.getXMin(), a.x);
                yMax = max(b.getYMax(), a.y);
                yMin = min(b.getYMin(), a.y);
                zMax = max(b.getZMax(), a.z);
                zMin = min(b.getZMin(), a.z);
        }

        private static int max(int a, int b){
            return a>b ? a : b;
        }

        private static int min(int a, int b){
            return a<b ? a : b;
        }


        public boolean includes(Atom a){
            return getXMin() <= a.x && a.x <= getXMax() &&
                    getYMin() <= a.y && a.y <= getYMax() &&
                    getZMin() <= a.z && a.z <= getZMax();
        }

        public boolean excludes(Atom a){
            return getXMin() > a.x || a.x > getXMax() ||
                    getYMin() > a.y || a.y > getYMax() ||
                    getZMin() > a.z || a.z > getZMax();
        }

        @Override
        public String toString() {
            return "AtomBounds{" +
                    "xMax=" + xMax +
                    ", xMin=" + xMin +
                    ", yMax=" + yMax +
                    ", yMin=" + yMin +
                    ", zMax=" + zMax +
                    ", zMin=" + zMin +
                    '}';
        }

        public static AtomBounds combine(Collection<Bounds> bounds) {
            int xMax = Integer.MIN_VALUE;
            int xMin = Integer.MAX_VALUE;
            int yMax = Integer.MIN_VALUE;
            int yMin = Integer.MAX_VALUE;
            int zMax = Integer.MIN_VALUE;
            int zMin = Integer.MAX_VALUE;

            boolean found = false;
            for (Bounds b: bounds){
                found = true;
                xMax = max(xMax, b.getXMax());
                xMin = min(xMin, b.getXMin());
                yMax = max(yMax, b.getYMax());
                yMin = min(yMin, b.getYMin());
                zMax = max(zMax, b.getZMax());
                zMin = min(zMin, b.getZMin());
            }
            if(found) {
                return new AtomBounds(xMax, xMin, yMax, yMin, zMax, zMin);
            } else {
                return new AtomBounds(0,0,0,0,0,0);
            }
        }

    }
}
