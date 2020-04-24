package uk.co.edstow.cpacgen;

import uk.co.edstow.cpacgen.scamp5.Scamp5PairGenFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static uk.co.edstow.cpacgen.RegisterAllocator.Register.*;
import static uk.co.edstow.cpacgen.scamp5.Scamp5PairGenFactory.Config.SearchStrategy.Exhuastive;
import static uk.co.edstow.cpacgen.scamp5.Scamp5PairGenFactory.Config.SearchStrategy.SortedAtomDistance;

public class Main {
    public static void main(String[] args) {
        List<Goal> final_goals = new ArrayList<>();



        int[][] multi3 = new int[][]{
                { 1, 2, 7, 2, 2},
                { 1, 5, 3, 0, 7},
                { 2, 7, 0, 2, 5},
                { 8, 7, 5, 6, 0},
                { 0, 8, 6, 8, 4}
        };
        //final_goals.add(new Goal.Factory(multi3).get());

        int[][] multiSobelV = new int[][]{
                { 0, 0, 0, 0, 0},
                { 0, 1, 0, -1, 0},
                { 0, 2, 0, -2, 0},
                { 0, 1, 0, -1, 0},
                { 0, 0, 0, 0, 0}
        };
//        final_goals.add(new Goal.Factory(multiSobelV).get());

        int[][] multiSobelH = new int[][]{
                { 0, 0, 0, 0, 0},
                { 0, 1, 2, 1, 0},
                { 0, 0, 0, 0, 0},
                { 0, -1, -2, -1, 0},
                { 0, 0, 0, 0, 0}
        };
//        final_goals.add(new Goal.Factory(multiSobelH).get());

        int[][] multiBox1x1 = new int[][]{
                { 0, 0, 0, 0, 0},
                { 0, 0, 0, 0, 0},
                { 0, 0, 1, 0, 0},
                { 0, 0, 0, 0, 0},
                { 0, 0, 0, 0, 0}
        };
        //final_goals.add(new Goal.Factory(multiBox1x1).get());


        int[][] multiBox2x2 = new int[][]{
                { 0, 0, 0, 0, 0},
                { 0, 0, 1, 1, 0},
                { 0, 0, 1, 1, 0},
                { 0, 0, 0, 0, 0},
                { 0, 0, 0, 0, 0}
        };
        //final_goals.add(new Goal.Factory(multiBox2x2).get());

        int[][] multiBox3x3 = new int[][]{
                { 0, 0, 0, 0, 0},
                { 0, 1, 1, 1, 0},
                { 0, 1, 1, 1, 0},
                { 0, 1, 1, 1, 0},
                { 0, 0, 0, 0, 0}
        };
        //final_goals.add(new Goal.Factory(multiBox3x3).get());

        int[][] multiBox5x5 = new int[][]{
                { 1, 1, 1, 1, 1},
                { 1, 1, 1, 1, 1},
                { 1, 1, 1, 1, 1},
                { 1, 1, 1, 1, 1},
                { 1, 1, 1, 1, 1}
        };
        //final_goals.add(new Goal.Factory(multiBox5x5).get());

        int[][] multiGuass3x3 = new int[][]{
                { 0, 0, 0, 0, 0},
                { 0, 1, 2, 1, 0},
                { 0, 2, 4, 2, 0},
                { 0, 1, 2, 1, 0},
                { 0, 0, 0, 0, 0}
        };
        final_goals.add(new Goal.Factory(multiGuass3x3, 4).get());

        int[][] multiGuass5x5 = new int[][]{
                { 0, 1, 2, 1, 0},
                { 1, 4, 6, 4, 1},
                { 2, 6, 10, 6, 2},
                { 1, 4, 6, 4, 1},
                { 0, 1, 2, 1, 0}
        };
        //final_goals.add(new Goal.Factory(multiGuass5x5).get());

        int[][] multi1 = new int[][]{
                { 0, 0, 0, 0, 0},
                { 0, 0, 0, 0, 0},
                { 0, 0, 4, 0, 0},
                { 0, 0, 0, 0, 0},
                { 0, 0, 0, 0, 0}
        };
        //final_goals.add(new Goal.Factory(multi1).get());
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

        RegisterAllocator.Register[] availableRegisters = new RegisterAllocator.Register[]{A, B, C, D, E, F};
        RegisterAllocator registerAllocator = new RegisterAllocator(A, availableRegisters);

        Scamp5PairGenFactory pairGenFactory = new Scamp5PairGenFactory(
                (goals, depth, rs1, initalGoal) -> {
//                    Scamp5PairGenFactory.Config conf = new Scamp5PairGenFactory.Config(SortedAtomDistance, availableRegisters.length, depth);
                    int max = Integer.MIN_VALUE;
                    for (Goal goal : goals) {
                        max = Math.max(max, goal.atomCount());
                    }
                    int threshold = 0;
                    Scamp5PairGenFactory.Config conf = new Scamp5PairGenFactory.Config(max>threshold? SortedAtomDistance: Exhuastive, availableRegisters.length, depth);
                    conf.useAll();
                    conf.useSubPowerOf2();
                    //conf.useBasicOps();
                    return conf;
                }
        );
        ReverseSearch.RunConfig config = new ReverseSearch.RunConfig();
        config.setWorkers(1)
                .setRegisterAllocator(registerAllocator)
                .setTimeOut(false).setLiveCounter(true).setSearchTime(60000)
                .setTraversalAlgorithm(ReverseSearch.TraversalAlgorithm.SOT);
        ReverseSearch rs = new ReverseSearch(6, final_goals, pairGenFactory, config);
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
        System.out.println("length: " + p.depth() + " Cost: "+p.cost());
        System.out.println("CircuitDepths:" + Arrays.toString(p.circuitDepths()));
        System.out.println(p);
        System.out.println(p.toGoalsString());
        RegisterAllocator.Mapping mapping = registerAllocator.solve(p);
        System.out.println(mapping);
        System.out.println(p.produceCode(mapping));



    }
}
