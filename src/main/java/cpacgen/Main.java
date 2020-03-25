package cpacgen;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Goal.Factory finalGoalFactory = new Goal.Factory();

        finalGoalFactory.add(new Atom(0, 1, 0));
        finalGoalFactory.add(new Atom(0, 0, 0));
        finalGoalFactory.add(new Atom(1, 1, 0));
        finalGoalFactory.add(new Atom(1, 0, 0));


//        finalGoalFactory.add(new Atom(-1, 1, 0));
//        finalGoalFactory.add(new Atom(-1, 0, 0));
//        finalGoalFactory.add(new Atom(-1, -1, 0));
        List<Goal> final_goals = new ArrayList<>();
        final_goals.add(finalGoalFactory.get());
        ReverseSplit rs = new ReverseSplit(2, final_goals);
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
        System.out.println(rs.cache);

        System.out.println("Best");
        Plan p = rs.plans.get(imin);
        System.out.println("Found new Plan! length: " + p.depth() + " Cost: "+p.cost());
        System.out.println(p);
        p.display();
    }
}
