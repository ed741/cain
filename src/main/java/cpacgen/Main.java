package cpacgen;

import java.util.ArrayList;
import java.util.List;

import static cpacgen.RegisterAllocator.Register.*;

public class Main {
    public static void main(String[] args) {
        List<Goal> final_goals = new ArrayList<>();

        Goal.Factory finalGoalFactory = new Goal.Factory();

//        finalGoalFactory.add(new Atom(0, 1, 0));
//        finalGoalFactory.add(new Atom(0, 0, 0));
//        finalGoalFactory.add(new Atom(1, 1, 0));
//        finalGoalFactory.add(new Atom(1, 0, 0));
//        finalGoalFactory.add(new Atom(5, 0, 0));

//
        finalGoalFactory.add(new Atom(-1, 1, 0));
        finalGoalFactory.add(new Atom(-1, 0, 0));
        finalGoalFactory.add(new Atom(-1, 0, 0));
        finalGoalFactory.add(new Atom(-1, -1, 0));
        finalGoalFactory.add(new Atom(1, 1, 0));
        finalGoalFactory.add(new Atom(1, 0, 0));
        finalGoalFactory.add(new Atom(1, 0, 0));
        finalGoalFactory.add(new Atom(1, -1, 0));
//        finalGoalFactory.add(new Atom(0, 0, 0));
//        finalGoalFactory.add(new Atom(0, -1, 0));
//        finalGoalFactory.add(new Atom(0, -2, 0));
        final_goals.add(finalGoalFactory.get());

//        finalGoalFactory = new Goal.Factory();
//
//        finalGoalFactory.add(new Atom(1, 1, 0));
//        finalGoalFactory.add(new Atom(0, 1, 0));
//        finalGoalFactory.add(new Atom(0, 1, 0));
//        finalGoalFactory.add(new Atom(-1, 1, 0));
//        finalGoalFactory.add(new Atom(1, -1, 0));
//        finalGoalFactory.add(new Atom(0, -1, 0));
//        finalGoalFactory.add(new Atom(0, -1, 0));
//        finalGoalFactory.add(new Atom(-1, -1, 0));

       // final_goals.add(finalGoalFactory.get());


//        finalGoalFactory = new Goal.Factory();
//
//        finalGoalFactory.add(new Atom(-1, -1, 0));
//        finalGoalFactory.add(new Atom(-1, 0, 0));
//        finalGoalFactory.add(new Atom(-1, 1, 0));
//        finalGoalFactory.add(new Atom(0, 1, 0));
//        finalGoalFactory.add(new Atom(1, 1, 0));
//        finalGoalFactory.add(new Atom(1, 0, 0));
//        finalGoalFactory.add(new Atom(1, -1, 0));
//        finalGoalFactory.add(new Atom(0, -1, 0));
//        finalGoalFactory.add(new Atom(0, 0, 0));

//        //final_goals.add(finalGoalFactory.get());
//        int[][] multi = new int[][]{
//                { 0, 1, 2, 1, 0},
//                { 1, 4, 6, 4, 1},
//                { 2, 6, 10, 6, 2},
//                { 1, 4, 6, 4, 1},
//                { 0, 1, 2, 1, 0}
//        };
        //final_goals.add(new Goal.Factory(multi).get());

        ReverseSplit rs = new ReverseSplit(0, final_goals);
        rs.search();

        System.out.println("print plans");
        double min = Double.MAX_VALUE;
        int imin = 0;
        for (int i = 0; i < rs.getPlans().size(); i ++){
            Plan pl = rs.getPlans().get(i);
            System.out.println(pl);
            if(pl.cost() < min){
                imin = i;
                min = pl.cost();
            }
        }
        rs.printStats();

        System.out.println("Best");
        Plan p = rs.getPlans().get(imin);
        System.out.println("Found new Plan! length: " + p.depth() + " Cost: "+p.cost());
        System.out.println(p);
        System.out.println(p.toGoalsString());
        //p.display();
        RegisterAllocator ra = new RegisterAllocator(p, A, B, C, D, E, F);
        System.out.println(p.produceCode(ra.solve()));


    }
}
