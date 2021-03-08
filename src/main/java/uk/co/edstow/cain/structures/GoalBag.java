package uk.co.edstow.cain.structures;

import java.util.*;
import java.util.stream.Collectors;

public class GoalBag<G extends Goal<G>> implements Iterable<G>{
    private final ArrayList<G> arrayList;
    private boolean immutable = false;
    private final boolean fullsort;
    private double sumTotal = Double.NaN;
    private static final Comparator<Goal<?>> halfComp = (a, b) -> {
        if(a==b){
            return 0;
        }
        return Double.compare(b.total(), a.total());
    };

    public GoalBag(GoalBag<G> b) {
        arrayList = new ArrayList<>(b.arrayList);
        fullsort = b.fullsort;
    }

    public GoalBag() {
        arrayList = new ArrayList<>();
        fullsort = false;
    }

    public GoalBag(G goal) {
        this();
        add(goal);
    }

    public GoalBag(GoalBag<G> b, boolean fullSort) {
        this.arrayList = new ArrayList<>(b.arrayList);
        if(fullSort) {
            arrayList.sort(G::compareTo);
            immutable = true;

        }
        this.fullsort = fullSort;

    }

    public GoalBag(Collection<G> goals) {
        this();
        this.addAll(goals);
    }

    public void setImmutable(){
        immutable = true;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isImmutable(){
        return immutable;
    }

    public double sumTotal(){
        if (isImmutable() && !Double.isNaN(sumTotal)){
            return sumTotal;
        }
        double a = 0;
        for(G g: arrayList){
            a += g.total();
        }
        sumTotal = a;
        return a;
    }


    public boolean add(G goal) {
        assert !immutable;
        for (int i = 0; i < arrayList.size(); i++) {
            if(halfComp.compare(arrayList.get(i), goal) >= 0){
                arrayList.add(i, goal);
                return true;
            }
        }
        arrayList.add(goal);
        return true;
    }

    public boolean addAll(Collection<? extends G> collection) {
        assert !immutable;
        boolean c = false;
        for (G g: collection) {
            add(g);
            c = true;
        }
        return c;
    }

    public boolean remove(G goal){
        assert !immutable;

        for (int i = 0; i < arrayList.size(); i++) {
            G goalI = arrayList.get(i);
            if(goalI.same(goal)){
                arrayList.remove(i);
                return true;
            }
            if(halfComp.compare(goalI,goal) > 0){
                return false;
            }
        }
        return false;
    }

    public G remove(int i){
        assert !immutable;
        return arrayList.remove(i);
    }

    @Override
    public int hashCode() {
        return arrayList.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoalBag<?> goalBag = (GoalBag<?>) o;
        assert this.fullsort;
        assert goalBag.fullsort;
        return Objects.equals(arrayList, goalBag.arrayList);
    }

    public boolean removeEquivalent(G g){
        assert !immutable;
        for (int i = 0; i < arrayList.size(); i++) {
            G goalI = arrayList.get(i);
            if(goalI.equivalent(g)){
                arrayList.remove(i);
                return true;
            }
            if(halfComp.compare(goalI,g) > 0){
                return false;
            }
        }
        return false;
    }

    public int size(){
        return arrayList.size();
    }

    @Override
    public Iterator<G> iterator() {
        return arrayList.iterator();
    }

    public G get(int i) {
        return arrayList.get(i);
    }


    public String toGoalsString(boolean[] topBorder, boolean[] bottomBorder, boolean centreDot, boolean colourNeg){
        assert immutable;
        return toGoalsString(arrayList, topBorder, bottomBorder, centreDot, colourNeg);
    }

    public static <E extends Goal<?>>  String toGoalsString(List<E> goals) {
        return toGoalsString(goals, false, false, true, true);
    }

    public static <E extends Goal<?>>  String toGoalsString(List<E> goals, boolean topBorder, boolean bottomBorder, boolean centreDot, boolean colourNeg) {
        boolean[] tops = new boolean[goals.size()];
        boolean[] bottoms = new boolean[goals.size()];
        Arrays.fill(tops, topBorder);
        Arrays.fill(bottoms, bottomBorder);
        return toGoalsString(goals, tops, bottoms, centreDot, colourNeg);
    }

    public static <E extends Goal<?>>  String toGoalsString(List<E> goals, boolean[] topBorder, boolean[] bottomBorder, boolean centreDot, boolean colourNeg) {
        List<Bounds> b = new ArrayList<>();
        for (Goal<?> goal : goals) {
            b.add(goal.bounds());
        }
        return GoalBag.toGoalsString(goals, new Bounds.SimpleBounds(b), topBorder, bottomBorder, centreDot, colourNeg);
    }

    public static <E extends Goal<?>> String toGoalsString(List<E> goals, Bounds inputBounds, boolean[] topBorder, boolean[] bottomBorder, boolean centreDot, boolean colourNeg){
        Bounds b = inputBounds.includeCentre();
        int height = 1 + b.getYMax() - b.getYMin();
        int width = 1 + b.getXMax() - b.getXMin();
        List<String[][]> arrays = new ArrayList<>();
        List<int[]> widthArrays = new ArrayList<>();

        for (int i = 0; i < goals.size(); i++) {
            Goal<?> goal = goals.get(i);
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
                        sb.append("_");
                    }
                }
                sb.append(' ');
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        return arrayList.isEmpty();
    }

    public boolean containsAll(List<G> goals) {
        return arrayList.containsAll(goals);
    }

    public List<G> asList(){
        return Collections.unmodifiableList(arrayList);
    }

    public Bounds bounds() {
        return new Bounds.SimpleBounds(arrayList.stream().map(Goal::bounds).collect(Collectors.toList()));
    }
}
