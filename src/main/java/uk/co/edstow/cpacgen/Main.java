package uk.co.edstow.cpacgen;

import uk.co.edstow.cpacgen.scamp5.Scamp5PairGenFactory;

import java.util.ArrayList;
import java.util.List;

import static uk.co.edstow.cpacgen.RegisterAllocator.Register.*;

public class Main {
    public static void main(String[] args) {
        List<Goal> final_goals = new ArrayList<>();

        int[][] multi2 = new int[][]{
                { 0, 0, 0, 0, 0},
                { 0, 1, 2, 1, 0},
                { 0, 2, 4, 2, 0},
                { 0, 1, 2, 1, 0},
                { 0, 0, 0, 0, 0}
        };
        //final_goals.add(new Goal.Factory(multi2).get());

        int[][] multi3 = new int[][]{
                { 0, 0, 0, 0, 0},
                { 0, -1, 0, 0, 0},
                { 0, 2, 1, 0, 0},
                { 0, 1, 0, 0, 0},
                { 0, 0, 0, 0, 0}
        };
        //final_goals.add(new Goal.Factory(multi3).get());

        int[][] multi4 = new int[][]{
                { 0, 0, 0, 0, 0},
                { 0, 0, 0, 1, 0},
                { 0, 0, 0, 2, 0},
                { 0, 0, 0, 1, 0},
                { 0, 0, 0, 0, 0}
        };
        //final_goals.add(new Goal.Factory(multi4).get());

        int[][] multiSobelV = new int[][]{
                { 0, 0, 0, 0, 0},
                { 0, 1, 0, -1, 0},
                { 0, 2, 0, -2, 0},
                { 0, 1, 0, -1, 0},
                { 0, 0, 0, 0, 0}
        };
        //final_goals.add(new Goal.Factory(multiSobelV).get());

        int[][] multiSobelH = new int[][]{
                { 0, 0, 0, 0, 0},
                { 0, 1, 2, 1, 0},
                { 0, 0, 0, 0, 0},
                { 0, -1, -2, -1, 0},
                { 0, 0, 0, 0, 0}
        };
        //final_goals.add(new Goal.Factory(multiSobelH).get());

        int[][] multiBox2x2 = new int[][]{
                { 0, 0, 0, 0, 0},
                { 0, 0, 1, 1, 0},
                { 0, 0, 1, 1, 0},
                { 0, 0, 0, 0, 0},
                { 0, 0, 0, 0, 0}
        };
        //final_goals.add(new Goal.Factory(multiBox2x2).get());

        int[][] multiGuass5x5 = new int[][]{
                { 0, 1, 2, 1, 0},
                { 1, 4, 6, 4, 1},
                { 2, 6, 10, 6, 2},
                { 1, 4, 6, 4, 1},
                { 0, 1, 2, 1, 0}
        };
        final_goals.add(new Goal.Factory(multiGuass5x5).get());

//        int[][] multi1 = new int[][]{
//                { 0, 0, 0, 0, 0},
//                { 0, 1, 2, 1, 0},
//                { 0, 1, 4, 4, 1},
//                { 0, 0, 1, 2, 1},
//                { 0, 0, 0, 0, 0}
//        };
//        final_goals.add(new Goal.Factory(multi1).get());
//
//
//        int[][] multi3 = new int[][]{
//                { 0, 0, 0, 0, 0},
//                { 0, 0, 0, 0, 0},
//                { 0, 1, 2, 0, 0},
//                { 0, 2, 5, 2, 0},
//                { 0, 1, 2, 1, 0}
//        };
//        final_goals.add(new Goal.Factory(multi3).get());
//
//        int[][] multi4 = new int[][]{
//                { 0, 1, 2, 1, 0},
//                { 0, 1, 4, 3, 1},
//                { 0, 0, 0, 2, 1},
//                { 0, 0, 0, 0, 0},
//                { 0, 0, 0, 0, 0}
//        };
//        final_goals.add(new Goal.Factory(multi4).get());
        Scamp5PairGenFactory pairGenFactory = new Scamp5PairGenFactory(
                (goals, depth, rs1, initalGoal) ->
                        new Scamp5PairGenFactory.Config(
                                true, true, true, true,
                                true, true,
                                false, false, false,
                                10, depth)
        );
        RegisterAllocator.Register[] availableRegisters = new RegisterAllocator.Register[]{A, B, C, D, E, F};
        int maxDepth = 30;

        ReverseSplit rs = new ReverseSplit(6, final_goals, pairGenFactory, availableRegisters.length, maxDepth);
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
        RegisterAllocator ra = new RegisterAllocator(p, A, availableRegisters);
        System.out.println(p.produceCode(ra.solve()));


    }
}
