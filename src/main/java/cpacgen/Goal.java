package cpacgen;

import cpacgen.util.Bounds;
import scala.util.parsing.combinator.testing.Str;

import java.util.*;


public class Goal implements List<Atom>, Comparable<Goal>{

    private List<Atom> list;

    public Goal(List<Atom> list) {
        ArrayList<Atom> l = new ArrayList<>(list);
        l.sort(Atom.comparator);
        this.list = Collections.unmodifiableList(l);
    }

    public Goal() {
        list = Collections.emptyList();
    }

    public Goal(Atom... atoms) {
        List<Atom> l = Arrays.asList(atoms);
        l.sort(Atom.comparator);
        this.list = Collections.unmodifiableList(l);
    }

    private Goal(ArrayList<Atom> al){
        this.list = Collections.unmodifiableList(al);
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
        List<Atom> tmp = new ArrayList<>(g);
        for (Atom a: this){
            if (!tmp.remove(a)){
                return false;
            }
        }
        return tmp.isEmpty();
    }


    /**
     * @return all possible sub Goals (subsets) of this goal, excluding the emptpy Goal.
     */
    public Collection<Goal> allSplits(){
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

    public String getCharTableString(boolean border){
        Bounds b = new Bounds(Bounds.BoundsFromGoal(this), new Atom(0,0,0));
        int height = 1 + b.yMax - b.yMin;
        int width = 1 + b.xMax - b.xMin;
        char[][] table = getCharTable(b, width, height, border);
        StringBuilder sb = new StringBuilder();
        for (int j = height+1; j >= 0; j--) {
            sb.append(table[j]);
            sb.append(' ');
            sb.append("\n");
        }
        return sb.toString();
    }

    public char[][] getCharTable(boolean border){
        Bounds b = new Bounds(Bounds.BoundsFromGoal(this), new Atom(0,0,0));
        int height = 1 + b.yMax - b.yMin;
        int width = 1 + b.xMax - b.xMin;
        return getCharTable(b, width, height, border);
    }


    public char[][] getCharTable(Bounds b, int width, int height, boolean border) {
        char [][] tableArray = new char[height+2][width+2];
        int xZero = 1-b.xMin;
        int yZero = 1-b.yMin;
        for (int x = b.xMin-1; x <= b.xMax+1; x++) {
            for (int y = b.yMin-1; y <= b.yMax+1; y++) {
                if (b.xMin <= x && x <= b.xMax && b.yMin <= y && y <= b.yMax) {
                    tableArray[yZero + y][xZero + x] = '0';
                } else {
                    tableArray[yZero + y][xZero + x] = border? '*':'+';
                    if(x==0){
                        tableArray[yZero + y][xZero + x] = '|';
                    }
                    if(y==0){
                        tableArray[yZero + y][xZero + x] = '-';
                    }
                }
            }
        }
        for (Atom a : this) {
            tableArray[yZero + a.y][xZero+a.x] += 1;
        }
        if (tableArray[yZero][xZero]=='0'){
            tableArray[yZero][xZero] = '.';

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
        for (int i = 0; i < size(); i++) {
            int c = get(i).compareTo(goal.get(i));
            if(c != 0){
                return c;
            }
        }
        return 0;
    }

    public int atomCount() {
        return size();
    }


    public static class Pair {
        private final Goal upper;
        private final Goal.Bag lowers;
        private final Transformation transformation;

        public Goal getUpper() {
            return upper;
        }

        public Bag getLowers() {
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
            lowers = new Goal.Bag();
            lowers.add(l);
            lowers.setImmutable();
            transformation = t;
        }

        public Pair(Goal u, List<Goal> ls, Transformation t) {
            upper = u;
            lowers = new Goal.Bag();
            lowers.addAll(ls);
            lowers.setImmutable();
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
        public Bag(Bag b) {
            super(b);
        }
        public Bag() {
        }

        public Bag(Goal goal) {
            add(goal);
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
                if(get(i).compareTo(goal) >= 0){
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
                    for (int k = 0; k < matrix[i][j]; k++) {
                        list.add(new Atom(i-(matrix.length/2), j-(matrix[i].length/2), 0));
                    }
                }
            }
        }


        public Goal get(){
            list.sort(null);
            Goal g = new Goal(list);
            list = null;
            return g;
        }

        public void add(Atom a){
            list.add(a);
        }

        public void addAll(Collection<Atom> a){
            list.addAll(a);
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
