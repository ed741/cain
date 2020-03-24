package cpacgen;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Goal final_goal = new Goal();
        final_goal.add(new Atom(0, 1, 0));
        final_goal.add(new Atom(0, 0, 0));
        final_goal.add(new Atom(1, 1, 0));
        final_goal.add(new Atom(1, 0, 0));


//        final_goal.add(new Atom(-1, 1, 0));
//        final_goal.add(new Atom(-1, 0, 0));
//        final_goal.add(new Atom(-1, -1, 0));
        List<Goal> final_goals = new ArrayList<>();
        final_goals.add(final_goal);
        ReverseSpilt rs = new ReverseSpilt(0, final_goals);
        rs.search();

        System.out.println("print plans");
        double min = Double.MAX_VALUE;
        int imin = 0;
        for (int i = 0; i < rs.plans.size(); i ++){
            Plan pl = rs.plans.get(i);
            System.out.println(pl);
            if(pl.cost() < min){
                imin = i;
                min = pl.cost();
            }
        }
        System.out.println("Best");
        System.out.println(rs.plans.get(imin));
        rs.plans.get(imin).display();
    }
}
