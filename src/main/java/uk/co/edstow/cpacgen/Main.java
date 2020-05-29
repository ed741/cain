package uk.co.edstow.cpacgen;

import uk.co.edstow.cpacgen.scamp5.Scamp5PairGenFactory;
import uk.co.edstow.cpacgen.scamp5.emulator.Scamp5Emulator;
import uk.co.edstow.cpacgen.util.Bounds;
import uk.co.edstow.cpacgen.util.Tuple;

import java.util.*;

import static uk.co.edstow.cpacgen.RegisterAllocator.Register.*;
import static uk.co.edstow.cpacgen.scamp5.Scamp5PairGenFactory.Config.SearchStrategy.Exhaustive;
import static uk.co.edstow.cpacgen.scamp5.Scamp5PairGenFactory.Config.SearchStrategy.SortedAtomDistance;

@SuppressWarnings("unused")
class Main {
    public static void main(String[] args) {
//        DemoSuite.runDemo();
        test();
    }
    public static void test() {
        List<Goal> final_goals = new ArrayList<>();
        int[] divisions = new int[2];



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
//        divisions[0] = 0;
//        final_goals.add(new Goal.Factory(1, multiSobelV).add(0, multiSobelV, 1).get());
//        divisions[1] = 2;

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

        int[][] multiBox7x7 = new int[][]{
                { 1, 1, 1, 1, 1, 1, 1},
                { 1, 1, 1, 1, 1, 1, 1},
                { 1, 1, 1, 1, 1, 1, 1},
                { 1, 1, 1, 1, 1, 1, 1},
                { 1, 1, 1, 1, 1, 1, 1},
                { 1, 1, 1, 1, 1, 1, 1},
                { 1, 1, 1, 1, 1, 1, 1}
        };
        //final_goals.add(new Goal.Factory(multiBox7x7).get());

        int[][] multiGuass3x3 = new int[][]{
                { 0, 0, 0, 0, 0},
                { 0, 1, 2, 1, 0},
                { 0, 2, 4, 2, 0},
                { 0, 1, 2, 1, 0},
                { 0, 0, 0, 0, 0}
        };
//        final_goals.add(new Goal.Factory(multiGuass3x3, 4).get());
//        divisions = 6;
//        final_goals.add(new Goal.Factory(multiGuass3x3).get());
//        divisions = 4;

        int[][] multiGuass5x5 = new int[][]{
                { 0, 1, 2, 1, 0},
                { 1, 4, 6, 4, 1},
                { 2, 6, 10, 6, 2},
                { 1, 4, 6, 4, 1},
                { 0, 1, 2, 1, 0}
        };
//        final_goals.add(new Goal.Factory(multiGuass5x5).get());
//        divisions = 6;

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

//        divisions = new int[]{0,0};
//        final_goals.add(new Goal.Factory(new Atom(0, 0,0,true), new Atom(0, 0,0,true), new Atom(0,0,1,true)).get());

        divisions = new int[]{0};
        final_goals.add(new Goal());

        RegisterAllocator.Register[] availableRegisters = new RegisterAllocator.Register[]{A, B, C, D, E, F};
        RegisterAllocator registerAllocator = new RegisterAllocator(new RegisterAllocator.Register[]{A}, availableRegisters);


        Scamp5PairGenFactory pairGenFactory = new Scamp5PairGenFactory(
                (goals, depth, rs1, initialGoal) -> {
//                    Scamp5PairGenFactory.Config conf = new Scamp5PairGenFactory.Config(SortedAtomDistance, availableRegisters.length, depth);
                    int max = Integer.MIN_VALUE;
                    for (Goal goal : goals) {
                        max = Math.max(max, goal.atomCount());
                    }
                    int threshold = 10;
                    Scamp5PairGenFactory.Config conf = new Scamp5PairGenFactory.Config(max>threshold? SortedAtomDistance: Exhaustive, availableRegisters.length, depth);
                    conf.useAll();
                    conf.useSubPowerOf2();
//                    conf.useBasicOps();
                    return conf;
                }
        );
        ReverseSearch.RunConfig config = new ReverseSearch.RunConfig();
        config.setWorkers(1)
                .setRegisterAllocator(registerAllocator).setLivePrintPlans(2)
                .setTimeOut(true).setLiveCounter(true).setSearchTime(1000)
                .setTraversalAlgorithm(TraversalSystem.SOTFactory());
        ReverseSearch rs = new ReverseSearch(divisions, final_goals, pairGenFactory, config);
        rs.search();

        System.out.println("print plans");
        double min = Double.MAX_VALUE;
        int iMin = 0;
        for (int i = 0; i < rs.getPlans().size(); i ++){
            Plan pl = rs.getPlans().get(i);
            System.out.println(pl);
            if(pl.cost() < min){
                iMin = i;
                min = pl.cost();
            }
        }
        rs.printStats();

        System.out.println("Best");
        Plan p = rs.getPlans().get(iMin);
        System.out.println("length: " + p.depth() + " Cost: "+p.cost());
        System.out.println("CircuitDepths:" + Arrays.toString(p.circuitDepths()));
        System.out.println(p);
        System.out.println(p.toGoalsString());
        RegisterAllocator.Mapping mapping = registerAllocator.solve(p, rs.getInitialGoals());
        System.out.println(mapping);
        String code = p.produceCode(mapping);
        System.out.println(code);

        System.out.println(new Bounds(final_goals).largestMagnitude());
        Scamp5Emulator emulator = new Scamp5Emulator(new Bounds(final_goals).largestMagnitude()*2);
        RegisterAllocator.Register[] initRegisters = registerAllocator.getInitRegisters();
        for (int i = 0; i < initRegisters.length; i++) {
            RegisterAllocator.Register r = initRegisters[i];
            emulator.run(String.format("input(%s,%d)", r, (1 << divisions[i]) * 128));
        }
        emulator.pushCode(code);
        emulator.flushInstructionBuffer();
        for (int i = 0; i < final_goals.size(); i++) {
            System.out.println("Goal: " + i + "In: " + availableRegisters[i].toString());
            System.out.println(final_goals.get(i));
            Map<Tuple<Integer, Tuple<Integer, String>>, Double> testMap = emulator.getRawProcessingElementContains(0, 0, availableRegisters[i].toString());
            System.out.println(testMap);

            Goal.Factory factory = new Goal.Factory();
            testMap.forEach((tuple, d) -> {
                if(d!=0) {
                    factory.add(new Atom(tuple.getA(), tuple.getB().getA(), Arrays.binarySearch(RegisterAllocator.Register.values(), RegisterAllocator.Register.valueOf(tuple.getB().getB())), d >= 0), Math.abs(d.intValue()));
                }
            });

            Goal testOut = factory.get();
            System.out.println("true out:");
            System.out.println(testOut.getCharTableString(true, true, true, true));
            System.out.println("target out:");
            System.out.println(final_goals.get(i).getCharTableString(false, false, true, true));

            Iterator<Tuple<Atom, Integer>> iterator = final_goals.get(i).uniqueCountIterator();
            while (iterator.hasNext()){
                Tuple<Atom, Integer> t = iterator.next();
                Tuple<Integer, Tuple<Integer, String>> coordinate = Tuple.triple(t.getA().x, t.getA().y, RegisterAllocator.Register.values()[t.getA().z].toString());
                Double d = testMap.get(coordinate);
                int expected = t.getA().positive? t.getB(): -t.getB();
                if(d == null || Double.compare(expected, d) != 0){
                    System.out.println("INTEGRITY CHECK ERROR");
                    System.out.println(coordinate);
                    System.out.println(d);
                    System.out.println(t.getB());
                }
                testMap.remove(coordinate);
            }
            if(!testMap.isEmpty()){
                System.out.println("INTEGRITY CHECK ERROR!");
                System.out.println(testMap);
            }

        }


    }
}
