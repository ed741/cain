package uk.co.edstow.cain.structures;

import uk.co.edstow.cain.util.Bounds;

import java.util.*;

public class GoalBag extends ArrayList<Goal> {
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

    public GoalBag(GoalBag b) {
        super(b);
    }

    public GoalBag() {
        super();
    }

    public GoalBag(Goal goal) {
        super(goal.size());
        add(goal);
    }

    public GoalBag(GoalBag b, boolean fullSort) {
        super(b);
        if(fullSort) {
            sort(fullComp);
        }
    }

    public GoalBag(Collection<Goal> goals) {
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

    public static String toGoalsString(List<Goal> goals, boolean[] topBorder, boolean[] bottomBorder, boolean centreDot, boolean colourNeg) {
        return toGoalsString(goals, new Bounds(goals), topBorder, bottomBorder, centreDot, colourNeg);
    }
    @SuppressWarnings("ForLoopReplaceableByForEach")
    public static String toGoalsString(List<Goal> goals, Bounds inputBounds, boolean[] topBorder, boolean[] bottomBorder, boolean centreDot, boolean colourNeg) {

        Bounds b = new Bounds(inputBounds, new Atom(0,0,0, true));
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
                        sb.append("_");
                    }
                }
                sb.append(' ');
            }
            sb.append("\n");
        }
        return sb.toString();
    }

}
