package uk.co.edstow.cpacgen;

import uk.co.edstow.cpacgen.util.Bounds;
import uk.co.edstow.cpacgen.util.Tuple;

import java.util.*;


public class Goal implements List<Atom>, Comparable<Goal>{

    private final List<Atom> list;

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
        if(g.list.size() != list.size()){
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            if(!g.list.get(i).equals(list.get(i))){
                return false;
            }
        }
        return true;
        //return list.equals(g.list);
        //TODO optimise!
//        List<Atom> tmp = new ArrayList<>(g);
//        for (Atom a: this){
//            if (!tmp.remove(a)){
//                return false;
//            }
//        }
//        return tmp.isEmpty();
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
     * @return all possible sub Goals (subsets) of this goal, excluding the emptpy Goal.
     */
    public List<Goal> allSplits(){
        List<Goal> out = rallSplits();
        out.remove(0);
        return out;
    }

    private List<Goal> rallSplits(){
        if (isEmpty()){
            List<Goal> out = new ArrayList<>();
            out.add(new Goal());
            return out;
        }
        ArrayList<Atom> sub = new ArrayList<>(this);
        Atom a = sub.remove(0);
        Collection<Goal> subSplits = new Goal(sub).rallSplits();
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

        public AveragePosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public String getCharTableString(boolean topBorder, boolean bottomBorder, boolean centreDot, boolean colourNeg){
        Bounds b = new Bounds(Bounds.BoundsFromGoal(this), new Atom(0,0,0, true));
        int height = 1 + b.yMax - b.yMin;
        int width = 1 + b.xMax - b.xMin;
        String[][] table = getCharTable(b, width, height, topBorder, bottomBorder, centreDot, colourNeg);
        StringBuilder sb = new StringBuilder();
        for (int j = height+1; j >= 0; j--) {
            for (int i = 0; i < table[j].length; i++) {
                sb.append(table[j][i]);
            }
            sb.append(' ');
            sb.append("\n");
        }
        return sb.toString();
    }

    public String[][] getCharTable(boolean topBorder, boolean bottomBorder, boolean centreDot, boolean colourNeg){
        Bounds b = new Bounds(Bounds.BoundsFromGoal(this), new Atom(0,0,0, true));
        int height = 1 + b.yMax - b.yMin;
        int width = 1 + b.xMax - b.xMin;
        return getCharTable(b, width, height, topBorder, bottomBorder, centreDot, colourNeg);
    }


    public String[][] getCharTable(Bounds b, int width, int height, boolean topBorder, boolean bottomBorder, boolean centreDot, boolean colourNeg) {
        String [][] tableArray = new String[height+2][width+2];
        int xZero = 1-b.xMin;
        int yZero = 1-b.yMin;
        for (int x = b.xMin-1; x <= b.xMax+1; x++) {
            for (int y = b.yMin-1; y <= b.yMax+1; y++) {
                if (b.xMin <= x && x <= b.xMax && b.yMin <= y && y <= b.yMax) {
                    tableArray[yZero + y][xZero + x] = "0";
                } else if(x==0){
                    tableArray[yZero + y][xZero + x] = "|";
                } else if(y==0){
                    tableArray[yZero + y][xZero + x] = "-";
                } else if(yZero + y == 0 && bottomBorder){
                    tableArray[yZero + y][xZero + x] = "v";
                } else if(yZero + y == height+1 && topBorder) {
                    tableArray[yZero + y][xZero + x] = "v";
                } else {
                    tableArray[yZero + y][xZero + x] = "+";
                }
            }
        }
        Iterator<Tuple<Atom, Integer>> it = this.uniqueCountIterator();
        while(it.hasNext()) {
            Tuple<Atom, Integer> t = it.next();
            int count = Math.abs(t.getB());
            boolean neg = !t.getA().positive;
            StringBuilder sb = new StringBuilder();
            if(neg){
                sb.append(colourNeg?"\u001B[34m":"-");//blue
            }
            sb.append(count);
            if(neg && colourNeg){
                sb.append("\u001B[0m");//reset
            }

            tableArray[yZero + t.getA().y][xZero+t.getA().x] = sb.toString();
        }
        if (centreDot && tableArray[yZero][xZero].equals("0")){
            tableArray[yZero][xZero] = ".";

        }
        return tableArray;
    }

    @Override
    public int compareTo(Goal goal) {
        if(this.size() < goal.size()){
            return 1;
        }
        if(this.size() > goal.size()){
            return -1;
        }
//        for (int i = 0; i < size(); i++) {
//            int c = get(i).compareTo(goal.get(i));
//            if(c != 0){
//                return c;
//            }
//        }
        return 0;
    }

    public int atomCount() {
        return size();
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
        private final Goal upper;
        private final List<Goal> lowers;
        private final Transformation transformation;

        public Goal getUpper() {
            return upper;
        }

        public List<Goal> getLowers() {
            return lowers;
        }

        public Transformation getTransformation() {
            return transformation;
        }

        private static String TransformationString(Transformation t){
            return t!=null? t.toStringN() : "null";
        }

        public Pair(Goal u, Goal l, Transformation t) {
            upper = u;
            ArrayList<Goal> list = new ArrayList<>();
            list.add(l);
            lowers = Collections.unmodifiableList(list);
            transformation = t;
        }

        public Pair(Goal u, List<Goal> ls, Transformation t) {
            upper = u;
            ArrayList<Goal> list = new ArrayList<>(ls);
            lowers = Collections.unmodifiableList(list);
            transformation = t;
        }


        public boolean contains(Atom a) {
            if (upper.contains(a)) {
                return true;
            }
            for(Goal l: lowers){
                if(l.contains(a)){
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "{T:" + transformation + ", U: " + upper + ", Ls:" + lowers + "}";
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

            return "{T:" + transformation + ", U: " + goalStringN(upper) + ", Ls:" + sLowers.toString() + "}";
        }
    }

    public static class Bag extends ArrayList<Goal>{
        private boolean immutable = false;
        private int atomCount = -1;
        private static Comparator<Goal> fullComp = (a, b) -> {
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
        private static Comparator<Goal> halfComp = (a, b) -> {
            if(a.size() < b.size()){
                return 1;
            }
            if(a.size() > b.size()){
                return -1;
            }
            return 0;
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
            };
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
                Goal goali = this.get(i);
                if(goali.same(goal)){
                    super.remove(i);
                    return true;
                }
                if(halfComp.compare(goali,goal) > 0){
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

        public String toGoalsString() {

            Bounds b = new Bounds(new Bounds(this), new Atom(0,0,0, true));
            int height = 1 + b.yMax - b.yMin;
            int width = 1 + b.xMax - b.xMin;
            List<String[][]> arrays = new ArrayList<>();

            for (int i = 0; i < size(); i++) {
                String[][] tableArray = get(i).getCharTable(b, width, height, false, false, true, true);
                arrays.add(tableArray);
            }

            StringBuilder sb = new StringBuilder();
            for (int j = height+1; j >= 0; j--) {
                for (String[][] array : arrays) {
                    for (int i = 0; i < array[j].length; i++) {
                        sb.append(array[j][i]);
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

        public Factory(int[][] matrix) {
            this.list = new ArrayList<>();
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    int count = Math.abs(matrix[i][j]);
                    boolean positive = matrix[i][j]>0;
                    for (int k = 0; k < count; k++) {
                        list.add(new Atom(j-(matrix[i].length/2), (matrix.length/2)-i, 0, positive));
                    }
                }
            }
        }
        public Factory(int[][] matrix, int multiples) {
            this.list = new ArrayList<>();
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    int count = Math.abs(matrix[i][j]);
                    boolean positive = matrix[i][j]>0;
                    for (int k = 0; k < count*multiples; k++) {
                        list.add(new Atom(j-(matrix[i].length/2), (matrix.length/2)-i, 0, positive));
                    }
                }
            }
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
        return o instanceof Goal && list.equals(((Goal) o).list);
    }
}
