package uk.co.edstow.cain.structures;

import java.util.Collections;

public interface Goal<G extends Goal> extends Comparable<G>{

    boolean same(G goal);
    boolean equivalent(G goal);
    double total();

    String[][] getCharTable(Bounds b, boolean top, boolean bottom, boolean centreDot, boolean colourNeg);

    default String getTableString(boolean topBorder, boolean bottomBorder, boolean centreDot, boolean colourNeg){
        return GoalBag.toGoalsString(Collections.singletonList(this), topBorder, bottomBorder, centreDot, colourNeg);
    }

    Bounds bounds();
}
