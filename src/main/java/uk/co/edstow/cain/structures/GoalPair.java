package uk.co.edstow.cain.structures;

import uk.co.edstow.cain.Transformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoalPair {
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

    public GoalPair(Goal u, Goal l, Transformation t) {
        uppers = Collections.singletonList(u);
        lowers = Collections.singletonList(l);
        transformation = t;
    }

    public GoalPair(Goal u, List<Goal> ls, Transformation t) {
        uppers = Collections.singletonList(u);
        ArrayList<Goal> list = new ArrayList<>(ls);
        lowers = Collections.unmodifiableList(list);
        transformation = t;
    }

    public GoalPair(List<Goal> us, List<Goal> ls, Transformation t) {
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
            sLowers.append(Goal.goalStringN(g));
            if (g != lowers.get(lowers.size()-1)) {
                sLowers.append(",");
            }
        }
        sLowers.append("]");

        StringBuilder sUppers  = new StringBuilder("[");
        for (Goal g: uppers){
            sUppers.append(Goal.goalStringN(g));
            if (g != uppers.get(uppers.size()-1)) {
                sUppers.append(",");
            }
        }
        sUppers.append("]");

        return "{T:" + transformation + ", U: " + sUppers.toString() + ", Ls:" + sLowers.toString() + "}";
    }

}
