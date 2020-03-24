package cpacgen;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Goal extends ArrayList<Atom> {

    public Goal(Collection<? extends Atom> collection) {
        super(collection);
    }

    public Goal() {
    }

    public Goal(Atom... atoms) {
        super(Arrays.asList(atoms));
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

    public Goal without(Goal goal) {
        Goal out = new Goal();
        Goal tmp = new Goal();
        tmp.addAll(goal);
        for (Atom a: this){
            if (!tmp.remove(a)){
                out.add(a);
            }
        }
        return out;
    }

    public boolean same(Goal g){
        Goal tmp = new Goal();
        tmp.addAll(g);
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
        Goal sub = new Goal();
        sub.addAll(this);
        Atom a = sub.remove(0);
        Collection<Goal> subSplits = sub.rallSplits();
        List<Goal> out = new ArrayList<>();
        for (Goal g: subSplits){
            if (out.stream().noneMatch(g::same)){
                out.add(g);
            }
            Goal gp = new Goal();
            gp.addAll(g);
            gp.add(a);
            if (out.stream().noneMatch(gp::same)){
                out.add(gp);
            }
        }
        return out;
    }

    public static class Pair {
        Goal upper;
        List<Goal> lowers;
        Transformation transformation;

        private static String TransformationString(Transformation t){
            return t!=null? t.toStringN() : "null";
        }

        public Pair(Goal u, Goal l, Transformation t) {
            upper = u;
            lowers = new ArrayList<>();
            lowers.add(l);
            transformation = t;
        }

        public Pair(Goal u, List<Goal> ls, Transformation t) {
            upper = u;
            lowers = new ArrayList<>();
            lowers.addAll(ls);
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

}
