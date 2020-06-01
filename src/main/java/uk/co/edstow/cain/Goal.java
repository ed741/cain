package uk.co.edstow.cain;

import uk.co.edstow.cain.util.Bounds;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;


public class Goal implements List<Atom>, Comparable<Goal>{

    private final List<Atom> list;

    @SuppressWarnings("WeakerAccess")
    public Goal(List<Atom> list) {
        ArrayList<Atom> l = new ArrayList<>(list);
        l.sort(Atom.comparator);
        this.list = Collections.unmodifiableList(simplifySorted(l));
    }

    public Goal(Goal goal) { // No need to sort as Goal is already correctly sorted
        ArrayList<Atom> l = new ArrayList<>(goal);
        this.list = Collections.unmodifiableList(l);
    }

    public Goal(Atom... atoms) {
        List<Atom> l = new ArrayList<>(Arrays.asList(atoms));
        l.sort(Atom.comparator);
        this.list = Collections.unmodifiableList(simplifySorted(l));
    }

    @SuppressWarnings("SameParameterValue")
    private Goal(ArrayList<Atom> list, boolean check){
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
    private static String goalStringN(Goal g){
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

    public Goal without(Goal goal) {
        Goal.Factory factory = new Goal.Factory();
        List<Atom> tmp = new ArrayList<>(goal);
        for (Atom a: this){
            if (!tmp.remove(a)){
                factory.add(a);
            }
        }
        return factory.get();
    }

    public boolean same(Goal g){
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

    public boolean equivalent(Goal g){
        return this == g;
    }

    public Goal negative(){
        ArrayList<Atom> list = new ArrayList<>();
        this.forEach(a->list.add(a.negate()));
        return new Goal(list, false);
    }

    public Goal translated(int x, int y, int z){
        ArrayList<Atom> list = new ArrayList<>();
        this.forEach(atom -> list.add(atom.moved(x, y, z)));
        return new Goal(list, false);
    }


    /**
     * @return all possible sub Goals (subsets) of this goal, excluding the empty Goal.
     */
    public List<Goal> allSplitsRecursive(){
        List<Goal> out = r_AllSplits();
        out.remove(0);
        return out;
    }

    private List<Goal> r_AllSplits(){
        if (isEmpty()){
            List<Goal> out = new ArrayList<>();
            out.add(new Goal());
            return out;
        }
        ArrayList<Atom> sub = new ArrayList<>(this);
        Atom a = sub.remove(0);
        Collection<Goal> subSplits = new Goal(sub).r_AllSplits();
        List<Goal> out = new ArrayList<>();
        for (Goal g: subSplits){
            if (out.stream().noneMatch(g::same)){
                out.add(g);
            }
            Goal.Factory factory = new Goal.Factory(g);
            factory.add(a);
            Goal gp = factory.get();
            if (out.stream().noneMatch(gp::same)){
                out.add(gp);
            }
        }
        return out;
    }

    public List<Goal> allSplits(){

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
        ArrayList<Goal> out = new ArrayList<>();
        for (int i = 1; i < lists.size(); i++) {
            List<Atom> atoms = lists.get(i);
            out.add(new Goal(atoms));
        }
        return out;
    }

    public int divide(Goal goal){
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

    public Goal subtract(Goal a) {
        return new Goal.Factory(list).subAll(a).get();
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

    public String getCharTableString(boolean topBorder, boolean bottomBorder, boolean centreDot, boolean colourNeg){
        return Goal.Bag.toGoalsString(Collections.singletonList(this), topBorder, bottomBorder, centreDot, colourNeg);
    }

    public String[][] getCharTable(boolean topBorder, boolean bottomBorder, boolean centreDot, boolean colourNeg){
        Bounds b = new Bounds(Bounds.BoundsFromGoal(this), new Atom(0,0,0, true));
        return getCharTable(b, topBorder, bottomBorder, centreDot, colourNeg);
    }


    public String[][] getCharTable(Bounds b, boolean topBorder, boolean bottomBorder, boolean centreDot, boolean colourNeg) {

        int width = 1+b.xMax - b.xMin;
        int height = 1+b.yMax - b.yMin;
        int depth = 1+b.zMax - b.zMin;
        int[][][] intArray = new int[width][height][depth];
        int ixZero = -b.xMin;
        int iyZero = -b.yMin;
        int izZero = -b.zMin;
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
            int xZero = 1 - b.xMin;
            int yZero = 1 - b.yMin;
            for (int x = b.xMin - 1; x <= b.xMax + 1; x++) {
                for (int y = b.yMin - 1; y <= b.yMax + 1; y++) {
                    if (b.xMin <= x && x <= b.xMax && b.yMin <= y && y <= b.yMax) {

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
    public int compareTo(Goal goal) {
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

    public boolean hasSubGoal(Goal goal) {
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


    public static class Pair {
        private final List<Goal> uppers;
        private final List<Goal> lowers;
        private final Transformation transformation;

        public List<Goal> getUppers() {
            return uppers;
        }

        public List<Goal> getLowers() {
            return lowers;
        }

        public Transformation getTransformation() {
            return transformation;
        }

        public Pair(Goal u, Goal l, Transformation t) {
            uppers = Collections.singletonList(u);
            lowers = Collections.singletonList(l);
            transformation = t;
        }

        public Pair(Goal u, List<Goal> ls, Transformation t) {
            uppers = Collections.singletonList(u);
            ArrayList<Goal> list = new ArrayList<>(ls);
            lowers = Collections.unmodifiableList(list);
            transformation = t;
        }

        public Pair(List<Goal> us, List<Goal> ls, Transformation t) {
            ArrayList<Goal> ulist = new ArrayList<>(us);
            uppers = Collections.unmodifiableList(ulist);
            ArrayList<Goal> llist = new ArrayList<>(ls);
            lowers = Collections.unmodifiableList(llist);
            transformation = t;
        }


        @Override
        public String toString() {
            return "{T:" + transformation + ", U: " + uppers.toString() + ", Ls:" + lowers.toString() + "}";
        }
        public String toStringN() {
            StringBuilder sLowers  = new StringBuilder("[");
            for (Goal g: lowers){
                sLowers.append(goalStringN(g));
                if (g != lowers.get(lowers.size()-1)) {
                    sLowers.append(",");
                }
            }
            sLowers.append("]");

            StringBuilder sUppers  = new StringBuilder("[");
            for (Goal g: uppers){
                sUppers.append(goalStringN(g));
                if (g != uppers.get(uppers.size()-1)) {
                    sUppers.append(",");
                }
            }
            sUppers.append("]");

            return "{T:" + transformation + ", U: " + sUppers.toString() + ", Ls:" + sLowers.toString() + "}";
        }

    }

    public static class Bag extends ArrayList<Goal>{
        private boolean immutable = false;
        private int atomCount = -1;
        private static final Comparator<Goal> fullComp = (a, b) -> {
            if(a==b){
                return 0;
            }
            if(a.size() < b.size()){
                return 1;
            }
            if(a.size() > b.size()){
                return -1;
            }
            for (int i = 0; i < a.size(); i++) {
                int c = a.get(i).compareTo(b.get(i));
                if(c != 0){
                    return c;
                }
            }
            return 0;
        };
        private static final Comparator<Goal> halfComp = (a, b) -> {
            if(a==b){
                return 0;
            }
            return Integer.compare(b.size(), a.size());
        };

        public Bag(Bag b) {
            super(b);
        }

        public Bag() {
            super();
        }

        public Bag(Goal goal) {
            super(goal.size());
            add(goal);
        }

        public Bag(Bag b, boolean fullSort) {
            super(b);
            if(fullSort) {
                sort(fullComp);
            }
        }

        public Bag(Collection<Goal> goals) {
            super(goals.size());
            this.addAll(goals);
        }

        public void setImmutable(){
            immutable = true;
        }

        @SuppressWarnings("WeakerAccess")
        public boolean isImmutable(){
            return immutable;
        }

        public int atomCount(){
            if (isImmutable() && atomCount >=0){
                return atomCount;
            }
            int a = 0;
            for(Goal g: this){
                a += g.size();
            }
            atomCount = a;
            return a;
        }


        @Override
        public boolean add(Goal goal) {
            assert !immutable;
            for (int i = 0; i < size(); i++) {
                if(halfComp.compare(get(i), goal) >= 0){
                    super.add(i, goal);
                    return true;
                }
            }
            super.add(goal);
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends Goal> collection) {
            assert !immutable;
            boolean c = false;
            for (Goal g:collection) {
                add(g);
                c = true;
            }
            return c;
        }

        public boolean addIfUnique(Goal goal){
            assert !immutable;
            for (int i = 0; i < size(); i++) {
                int c = halfComp.compare(get(i),goal);
                if(c > 0){
                    super.add(i, goal);
                    return true;
                } else if(c==0){
                    if(get(i).same(goal)){
                        return false;
                    }
                }
            }
            super.add(goal);
            return true;
        }

        @Override
        public boolean remove(Object g){
            assert !immutable;
            if(!(g instanceof Goal)){
                return false;
            }
            Goal goal = (Goal) g;
            for (int i = 0; i < this.size(); i++) {
                Goal goalI = this.get(i);
                if(goalI.same(goal)){
                    super.remove(i);
                    return true;
                }
                if(halfComp.compare(goalI,goal) > 0){
                    return false;
                }
            }
            return false;
        }

        public boolean removeEquivalent(Goal g){
            assert !immutable;
            for (int i = 0; i < this.size(); i++) {
                Goal goalI = this.get(i);
                if(goalI.equivalent(g)){
                    super.remove(i);
                    return true;
                }
                if(halfComp.compare(goalI,g) > 0){
                    return false;
                }
            }
            return false;
        }

        @Override
        public boolean addAll(int i, Collection<? extends Goal> collection) {
            throw new UnsupportedOperationException("Cannot add Goals at index");
        }

        @Override
        public Goal set(int i, Goal goal) {
            throw new UnsupportedOperationException("Cannot set Goals at index");
        }

        @Override
        public void add(int i, Goal goal) {
            throw new UnsupportedOperationException("Cannot add Goals at index");
        }


        public static String toGoalsString(List<Goal> goals) {
            return toGoalsString(goals, false, false, true, true);
        }

        public static String toGoalsString(List<Goal> goals, boolean topBorder, boolean bottomBorder, boolean centreDot, boolean colourNeg) {
            boolean[] tops = new boolean[goals.size()];
            boolean[] bottoms = new boolean[goals.size()];
            Arrays.fill(tops, topBorder);
            Arrays.fill(bottoms, bottomBorder);
            return toGoalsString(goals, tops, bottoms, centreDot, colourNeg);
        }


        @SuppressWarnings("ForLoopReplaceableByForEach")
        public static String toGoalsString(List<Goal> goals, boolean[] topBorder, boolean[] bottomBorder, boolean centreDot, boolean colourNeg) {

            Bounds b = new Bounds(new Bounds(goals), new Atom(0,0,0, true));
            int height = 1 + b.yMax - b.yMin;
            int width = 1 + b.xMax - b.xMin;
            List<String[][]> arrays = new ArrayList<>();
            List<int[]> widthArrays = new ArrayList<>();

            for (int i = 0; i < goals.size(); i++) {
                Goal goal = goals.get(i);
                String[][] tableArray = goal.getCharTable(b, topBorder[i], bottomBorder[i], centreDot, colourNeg);
                arrays.add(tableArray);
            }
            for (String[][] array : arrays) {
                int[] widths = new int[array[0].length];
                for (int j = 0; j < array.length; j++) {
                    for (int i = 0; i < array[j].length; i++) {
                        int len = array[j][i].replaceAll("\u001B\\[[0-9]+m", "").length();
                        if(len > widths[i]){
                            widths[i] = len;
                        }
                    }
                }
                widthArrays.add(widths);

            }


            StringBuilder sb = new StringBuilder();
            for (int j = height+1; j >= 0; j--) {
                for (int i1 = 0; i1 < arrays.size(); i1++) {
                    String[][] array = arrays.get(i1);
                    int[] widths = widthArrays.get(i1);
                    for (int i = 0; i < array[j].length; i++) {
                        int len = array[j][i].replaceAll("\u001B\\[[0-9]+m", "").length();
                        sb.append(array[j][i]);
                        for(int l = len; l < widths[i]; l++){
                            if(j == height+1 || j == 0 || i == 0 || i == array[j].length-1){
                                sb.append(array[j][i].charAt(0));
                            } else {
                                sb.append("_");
                            }
                        }
                    }
                    sb.append(' ');
                }
                sb.append("\n");
            }
            return sb.toString();
        }

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

        public Goal.Factory add(int z, int[][] matrix, int multiples){
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

        public Goal get(){
            Goal g = new Goal(list);
            list = null;
            return g;
        }

        public Goal.Factory add(Atom a){
            list.add(a); return this;
        }

        public Goal.Factory addAll(Collection<Atom> a){
            list.addAll(a); return this;
        }

        public Goal.Factory sub(Atom a){
            list.add(a.negate()); return this;
        }

        public Goal.Factory subAll(Collection<Atom> b) {
            b.forEach(atom -> list.add(atom.negate())); return this;
        }

        public Goal.Factory add(Atom a, int i) {
            for (int j = 0; j < i; j++) {
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
        return o instanceof Goal && same((Goal) o);
    }
}