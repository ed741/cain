package uk.co.edstow.cain;

import uk.co.edstow.cain.scamp5.Scamp5PairGenFactory;
import uk.co.edstow.cain.scamp5.emulator.Scamp5Emulator;
import uk.co.edstow.cain.util.Bounds;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.function.Supplier;

import static uk.co.edstow.cain.RegisterAllocator.Register.*;
import static uk.co.edstow.cain.scamp5.Scamp5PairGenFactory.Config.SearchStrategy.Exhaustive;
import static uk.co.edstow.cain.scamp5.Scamp5PairGenFactory.Config.SearchStrategy.SortedAtomDistance;

@SuppressWarnings("SameParameterValue")
class DemoSuite {

    private final static boolean SOBEL = false;
    private final static boolean BOX = true;
    private final static boolean GUASS = false;
    private final static boolean CNN_ON_FPSP_ANALOG_NET_2 = true;
    private final static boolean CNN_ON_FPSP_MAX_POOLED = true;
    private final static boolean RANDOM_DENSE = true;
    private final static boolean RANDOM_SPARSE = true;



    private static class TestSetup {
        final String name;
        final RegisterAllocator.Register[] availableRegisters;
        final Scamp5PairGenFactory pairGenFactory;
        final ReverseSearch.RunConfig runConfig;

        final int cores;
        final Supplier<? extends TraversalSystem> traversalAlgorithm;
        final int registerCount;
        final int threshold;
        final boolean allOps;
        final int seconds;
        final RegisterAllocator registerAllocator;

        private TestSetup(int cores, Supplier<? extends TraversalSystem> traversalAlgorithm, int registerCount, int threshold, boolean allOps, int seconds){
            RegisterAllocator.Register[] allRegisters = new RegisterAllocator.Register[]{A, B, C, D, E, F};
            final RegisterAllocator.Register[] availableRegisters = Arrays.copyOfRange(allRegisters, 0, registerCount);
            RegisterAllocator ra = new RegisterAllocator(new RegisterAllocator.Register[]{A}, availableRegisters);
            Scamp5PairGenFactory pairGenFactory = new Scamp5PairGenFactory(
                    (goals, depth, rs1, initialGoal) -> {
                        int max = Integer.MIN_VALUE;
                        for (Goal goal : goals) {
                            max = Math.max(max, goal.atomCount());
                        }
                        Scamp5PairGenFactory.Config conf = new Scamp5PairGenFactory.Config(max>threshold? SortedAtomDistance: Exhaustive, availableRegisters.length, depth);
                        if(allOps) {
                            conf.useAll();
                            conf.useSubPowerOf2();
                        }else{
                            conf.useBasicOps();
                        }
                        return conf;
                    }
            );

            ReverseSearch.RunConfig config = new ReverseSearch.RunConfig();
            config.setSearchTime(seconds*1000).setWorkers(cores).setRegisterAllocator(ra).setTimeOut(true)
                    .setTraversalAlgorithm(traversalAlgorithm).setLivePrintPlans(1);

            this.name = String.format("%d Core, %s, %d registers, threshold %d, %s, %d seconds", cores, traversalAlgorithm.get().getClass().getSimpleName(), registerCount, threshold, allOps?"AllOps":"BasicOps", seconds);
            this.availableRegisters = availableRegisters;
            this.registerAllocator = ra;
            this.pairGenFactory = pairGenFactory;
            this.runConfig = config;
            this.cores = cores;
            this.traversalAlgorithm = traversalAlgorithm;
            this.registerCount = registerCount;
            this.threshold = threshold;
            this.allOps = allOps;
            this.seconds = seconds;

        }
    }



    private static List<TestSetup> initialiseTestSetups() {
        List<TestSetup> setups = new ArrayList<>();
//        setups.add(new TestSetup(4, TraversalSystem.SOTFactory(), 6, 10, true, 60));
//        setups.add(new TestSetup(1, TraversalSystem.SOTFactory(), 6, 10, true, 60));
//        setups.add(new TestSetup(1, TraversalSystem.SOTFactory(), 6, 10, false, 60));
//        setups.add(new TestSetup(4, TraversalSystem.DFSFactory(), 6, 10, true, 60));
//        setups.add(new TestSetup(4, TraversalSystem.SOTFactory(), 6, 0, true, 60));
        setups.add(new TestSetup(1, TraversalSystem.SOTFactory(), 6, 10, true, 5));
        setups.add(new TestSetup(1, TraversalSystem.AStarFactory(ws -> {
            int sum = ws.goals.stream().mapToInt(goal -> -1+goal.stream().mapToInt(a -> Math.abs(a.x) + Math.abs(a.y) + Math.abs(a.z) + 1).sum()).sum();
            sum += ws.depth;
            return (double) sum;
        }), 6, 10, true, 5));

        return setups;
    }

    private static class Test{
        final String name;
        final List<Goal> finalGoals;
        final int[] divisions;
        final Map<TestSetup, Plan> results;
        final String aukeScore;

        Test(String name, List<Goal> finalGoals, int divisions, String aukeScore) {
            this.name = name;
            this.finalGoals = finalGoals;
            this.divisions = new int[]{divisions};
            this.aukeScore = aukeScore;
            this.results = new HashMap<>();

        }
    }

    private static int[][] makeRandom(Random r, int size, int min, int max, double sparsity){

        int[][] filter = new int[size][size];
        for (int i = 0; i < filter.length; i++) {
            for (int j = 0; j < filter[i].length; j++) {
                if(r.nextDouble() > sparsity){
                    filter[i][j] = r.nextInt((max+1)-min)+min;
                }
            }
        }
        return filter;
    }

    private static List<Test> initialiseDemosList(){

        List<Test> demos = new ArrayList<>();
        if (SOBEL) {
            final int[][] SobelV = new int[][]{
                    {0, 0, 0, 0, 0},
                    {0, 1, 0, -1, 0},
                    {0, 2, 0, -2, 0},
                    {0, 1, 0, -1, 0},
                    {0, 0, 0, 0, 0}
            };

            final int[][] SobelH = new int[][]{
                    {0, 0, 0, 0, 0},
                    {0, 1, 2, 1, 0},
                    {0, 0, 0, 0, 0},
                    {0, -1, -2, -1, 0},
                    {0, 0, 0, 0, 0}
            };

            demos.add(new Test("Vertical Sobel", Collections.singletonList(new Goal.Factory(SobelV).get()), 0, "7"));
            demos.add(new Test("Vertical and Horizontal Sobel", Arrays.asList(new Goal.Factory(SobelV).get(), new Goal.Factory(SobelH).get()), 0, "(7+7)"));
        }
        if (BOX) {
            final int[][] Box2x2 = new int[][]{
                    {0, 0, 0, 0, 0},
                    {0, 0, 1, 1, 0},
                    {0, 0, 1, 1, 0},
                    {0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0}
            };

            demos.add(new Test("2x2 Box", Collections.singletonList(new Goal.Factory(Box2x2).get()), 0, "4"));

            final int[][] Box3x3 = new int[][]{
                    {0, 0, 0, 0, 0},
                    {0, 1, 1, 1, 0},
                    {0, 1, 1, 1, 0},
                    {0, 1, 1, 1, 0},
                    {0, 0, 0, 0, 0}
            };

            demos.add(new Test("3x3 Box", Collections.singletonList(new Goal.Factory(Box3x3).get()), 0, "8"));

            final int[][] Box5x5 = new int[][]{
                    {1, 1, 1, 1, 1},
                    {1, 1, 1, 1, 1},
                    {1, 1, 1, 1, 1},
                    {1, 1, 1, 1, 1},
                    {1, 1, 1, 1, 1}
            };
            demos.add(new Test("5x5 Box", Collections.singletonList(new Goal.Factory(Box5x5).get()), 0, "15"));
            demos.add(new Test("5x5 and 3x3 Box", Arrays.asList(new Goal.Factory(Box3x3).get(), new Goal.Factory(Box5x5).get()), 0, "(8+15)"));
        }
        if (GUASS) {
            int[][] Guass3x3 = new int[][]{
                    {0, 0, 0, 0, 0},
                    {0, 1, 2, 1, 0},
                    {0, 2, 4, 2, 0},
                    {0, 1, 2, 1, 0},
                    {0, 0, 0, 0, 0}
            };
            demos.add(new Test("3x3 Guass", Collections.singletonList(new Goal.Factory(Guass3x3).get()), 4, "12"));

            int[][] Guass5x5 = new int[][]{
                    {0, 1, 2, 1, 0},
                    {1, 4, 6, 4, 1},
                    {2, 6, 10, 6, 2},
                    {1, 4, 6, 4, 1},
                    {0, 1, 2, 1, 0}
            };
            demos.add(new Test("5x5 Guass", Collections.singletonList(new Goal.Factory(Guass5x5).get()), 6, "50 (44?)"));
            demos.add(new Test("5x5 and 3x3 Guass", Arrays.asList(new Goal.Factory(Guass5x5).get(), new Goal.Factory(Guass3x3, 4).get()), 6, "(50+12)"));
        }
        if (CNN_ON_FPSP_ANALOG_NET_2) {// AnalogNet2, https://github.com/brouwa/CNNs-on-FPSPs/tree/master/1%20-%20AnalogNet2
             final int[][] kernel1 = new int[][]{
                    {0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0},
                    {0, -3, 1, 0, 0},
                    {0, -3, 0, 2, 0},
                    {0, 0, 0, 0, 0}
            };

            final int[][] kernel2 = new int[][]{
                    {0, 0, 0, 0, 0},
                    {0, -4, -1, 1, 0},
                    {0, -1, 2, 0, 0},
                    {0, 1, 1, 0, 0},
                    {0, 0, 0, 0, 0}
            };

            final int[][] kernel3 = new int[][]{
                    {0, 0, 0, 0, 0},
                    {0, -1, 2, 0, 0},
                    {0, -1, 1, -3, 0},
                    {0, 0, -3, 0, 0},
                    {0, 0, 0, 0, 0}
            };

            demos.add(new Test("AnalogNet2", Arrays.asList(new Goal.Factory(kernel1).get(), new Goal.Factory(kernel2).get(), new Goal.Factory(kernel3).get()), 2, "UNKNOWN"));
        }
        if (CNN_ON_FPSP_MAX_POOLED) {//https://github.com/brouwa/CNNs-on-FPSPs/
            final int[][] kernel1 = new int[][]{
                    {0, 0, 0, 0, 0},
                    {0, 1, 1, 0, 0},
                    {0, 0, 0, 0, 0},
                    {0, 1, 0, 0, 0},
                    {0, 0, 0, 0, 0}
            };

            final int[][] kernel2 = new int[][]{
                    {0, 0, 0, 0, 0},
                    {0, 1, 0, -2, 0},
                    {0, 0, -2, 0, 0},
                    {0, -1, -2, 1, 0},
                    {0, 0, 0, 0, 0}
            };

            final int[][] kernel3 = new int[][]{
                    {0, 0, 0, 0, 0},
                    {0, 0, 1, 1, 0},
                    {0, -2, -2, -1, 0},
                    {0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0}
            };

            final int[][] kernel4 = new int[][]{
                    {0, 0, 0, 0, 0},
                    {0, 0, -1, -1, 0},
                    {0, 0, 0, 0, 0},
                    {0, 1, 0, 1, 0},
                    {0, 0, 0, 0, 0}
            };

            demos.add(new Test("MaxPooled", Arrays.asList(new Goal.Factory(kernel1).get(), new Goal.Factory(kernel2).get(), new Goal.Factory(kernel3).get(), new Goal.Factory(kernel4).get()), 2, "UNKNOWN"));
        }
        {

            if (RANDOM_DENSE) {
                Random rand = new Random(2001);
                Goal[] r = new Goal[4];
                for (int i = 0; i < r.length; i++) {
                    r[i] = new Goal.Factory(makeRandom(rand, 3, 0, 8, 0)).get();
                }
                demos.add(new Test("Random0 3x3 [0-8] 0%", Collections.singletonList(r[0]), 3, "30"));
                demos.add(new Test("Random1 3x3 [0-8] 0%", Collections.singletonList(r[1]), 3, "35"));
                demos.add(new Test("Random2 3x3 [0-8] 0%", Collections.singletonList(r[2]), 3, "37"));
                demos.add(new Test("Random3 3x3 [0-8] 0%", Collections.singletonList(r[3]), 3, "34"));


                demos.add(new Test("Random0&1 3x3 [0-8] 0%", Arrays.asList(r[0], r[1]), 3, "(28+35)"));
                demos.add(new Test("Random2&3 3x3 [0-8] 0%", Arrays.asList(r[2], r[3]), 3, "(37+35)"));
            }
            if (RANDOM_SPARSE) {
                Random rand = new Random(2002);
                Goal[] r = new Goal[4];
                for (int i = 0; i < r.length; i++) {
                    r[i] = new Goal.Factory(makeRandom(rand, 3, 1, 8, 0.5)).get();
                }
                demos.add(new Test("Random0 3x3 [1-8] 50%", Collections.singletonList(r[0]), 3, "22"));
                demos.add(new Test("Random1 3x3 [1-8] 50%", Collections.singletonList(r[1]), 3, "15"));
                demos.add(new Test("Random2 3x3 [1-8] 50%", Collections.singletonList(r[2]), 3, "22"));
                demos.add(new Test("Random3 3x3 [1-8] 50%", Collections.singletonList(r[3]), 3, "33"));


                demos.add(new Test("Random0&1 3x3 [1-8] 50%", Arrays.asList(r[0], r[1]), 3,"(22+15)"));
                demos.add(new Test("Random2&3 3x3 [1-8] 50%", Arrays.asList(r[2], r[3]), 3, "(23+33)"));
            }
        }
        return demos;
    }

    public static void main(String[] args) {
        runDemo();

    }

    public static void runDemo() {
        List<Test> demos = initialiseDemosList();
        List<TestSetup> setups = initialiseTestSetups();

        for (Test demo : demos) {
            runFilter(demo, setups);
        }

        for (Test demo : demos) {
            System.out.println(demo.name + ":");
            for(Map.Entry<TestSetup, Plan> entry: demo.results.entrySet())
                System.out.println("\t" + entry.getKey().name + " -> circuit-depth: " + entry.getValue().maxCircuitDepth() + " depth: "+entry.getValue().depth());

        }
        System.out.println("\nLatex Table:\n");
        System.out.println(makeLatexTable(demos, setups));
    }


    private static void runFilter(Test test, List<TestSetup> setups){
        System.out.println("Running: "+ test.name);
        System.out.println(Goal.Bag.toGoalsString(test.finalGoals));

        for (TestSetup setup : setups) {
            ReverseSearch rs = new ReverseSearch(test.divisions, test.finalGoals, setup.pairGenFactory, setup.runConfig);
            rs.search();

            double min = Double.MAX_VALUE;
            int iMin = 0;
            for (int i = 0; i < rs.getPlans().size(); i++) {
                Plan pl = rs.getPlans().get(i);
                if (rs.costFunction.apply(pl) < min) {
                    iMin = i;
                    min = rs.costFunction.apply(pl);
                }
            }
            rs.printStats();

            System.out.println("Best: "+ iMin);
            if(iMin < rs.getPlans().size()) {
                Plan p = rs.getPlans().get(iMin);
                System.out.println("length: " + p.depth() + " Cost: " + rs.costFunction.apply(p));
                System.out.println(p);
                System.out.println("CircuitDepths:" + Arrays.toString(p.circuitDepths()));
                //System.out.println(p.toGoalsString());
                RegisterAllocator.Mapping mapping = setup.registerAllocator.solve(p, rs.getInitialGoals());
                //System.out.println(mapping);
                String code = p.produceCode(mapping);
                System.out.println(code);
                System.out.println(Goal.Bag.toGoalsString(test.finalGoals));

                if(checkPlan(test, setup, code)) {
                    System.out.println("Code validated on emulator");
                }else{
                    System.out.println("Code failed validation");
                    System.exit(-1);
                }


                test.results.put(setup, p);




            } else {
                System.out.println("None found!");
            }
        }

    }

    @SuppressWarnings("ConstantConditions")
    private static String makeLatexTable(List<Test> demos, List<TestSetup> setups){
        StringBuilder sb = new StringBuilder();
        sb.append("\\begin{longtable}{| m{2.5cm} m{4.5cm} | c |");
        setups.forEach(t-> sb.append(" c"));
        sb.append(" | m{2cm} |}\n");
        sb.append("\\caption{Kernels Tested in AUKE and Cain}\n");
        sb.append("\\label{table:kernelResults}\n");
        sb.append("\\endfirsthead\n");
        sb.append("\\endhead\n");
        sb.append("\\hline\n");
        sb.append("Name & Approximated Kernel & AUKE & \\multicolumn{").append(setups.size()).append("}{|c|}{Cain} & \\\\\n");
        sb.append("\\hline\n");
        sb.append("\\hline\n");
        // time
        sb.append("& & \\small{60");
        setups.forEach(t-> sb.append("}& \\small{").append(t.seconds));
        sb.append("} & \\small{Seconds} \\\\ \n");
        // cores
        sb.append("& & \\small{1");
        setups.forEach(t-> sb.append("}& \\small{").append(t.cores));
        sb.append("} & \\small{Threads} \\\\ \n");
        // Traversel algo
        sb.append("& & \\small{DFS");
        setups.forEach(t-> sb.append("}& \\small{").append(t.traversalAlgorithm.get().getClass().getSimpleName()));
        sb.append("} & \\small{Traversal} \\\\ \n");
        // registers
        sb.append("& & \\small{6");
        setups.forEach(t-> sb.append("}& \\small{").append(t.registerCount));
        sb.append("} & \\small{Register}s \\\\ \n");
        // All Ops
        sb.append("& & \\small{Basic");
        setups.forEach(t-> sb.append("}& \\small{").append(t.allOps?"All":"Basic"));
        sb.append("} & \\small{Instructions} \\\\ \n");
        // threshold
        sb.append("& & \\small{-");
        setups.forEach(t-> sb.append("}& \\small{").append(t.threshold));
        sb.append("} & \\small{Threshold} \\\\ \n");

        //sb.append("\\hline\n");
//        sb.append("& &");
//        for (TestSetup setup : setups) {
//            sb.append(" & \\rot{").append(setup.name).append("}");
//        }
//        sb.append(" \\\\ \n");
        sb.append("\\hline\n");
        for (Test demo : demos) {
            sb.append("\\hline\n");
            sb.append(demo.name.replace("%", "\\%").replace("&", "\\&")).append(" & ");
            List<String> filters = goalsToLatex(demo.finalGoals);
            for (int i = 0; i < filters.size(); i++) {
                if(demo.divisions[0]>0) sb.append("$\\frac{1}{").append(1 << demo.divisions[0]).append("}$");
                sb.append(filters.get(i));
                if(i != filters.size()-1) sb.append(",");
            }
            sb.append("&");

            if(demo.aukeScore != null && demo.aukeScore.length() > 0) {
                sb.append(demo.aukeScore);
            } else {
                sb.append("Unknown");
            }
            for (TestSetup setup : setups) {
                sb.append("&");
                if(demo.results.get(setup) != null) {
                    sb.append(demo.results.get(setup).depth());
                } else {
                    sb.append("-");
                }
            }
            sb.append("& ");
            if(1 < demo.finalGoals.stream().mapToInt(g -> Bounds.BoundsFromGoal(g).largestMagnitude()).max().getAsInt()){
                sb.append("\\vspace{2.5em}");
            } else {
                sb.append("\\vspace{1.1em}");
            }
            sb.append("\\\\ \n");
        }
        sb.append("\\hline\n");
        sb.append("\\end{longtable}");

        return sb.toString();
    }

    private static List<String> goalsToLatex(List<Goal> goals){
        Bounds b = new Bounds(new Bounds(goals), new Atom(0,0,0, true));
        int xr = Math.max(Math.abs(b.xMax), Math.abs(b.xMin));
        int yr = Math.max(Math.abs(b.yMax), Math.abs(b.yMin));
        int r = Math.max(xr, yr);
        b = new Bounds(b, new Atom(-r,-r,0, true));
        b = new Bounds(b, new Atom(r,r,0, true));
        List<String> out = new ArrayList<>(goals.size());
        for (Goal goal : goals) {
            String[][] table = goal.getCharTable(b, false, false, false, false);
            StringBuilder sb = new StringBuilder("$\\begin{bsmallmatrix} ");

            for (int j = table.length-1; j > 0; j--) {
                for (int i = 1; i < table[j].length-1; i++) {
                    sb.append(table[j][i]);
                    sb.append(i == table[j].length-2?" \\\\ ":" & ");
                }
            }
            sb.append("\\end{bsmallmatrix}$");
            out.add(sb.toString());
        }
        return out;
    }

    private static boolean checkPlan(Test test, TestSetup setup, String code){
        Scamp5Emulator emulator = new Scamp5Emulator(new Bounds(test.finalGoals).largestMagnitude()*3);
//        Scamp5Emulator.verbose = 100;
        RegisterAllocator.Register[] initRegisters = setup.registerAllocator.getInitRegisters();
        for (int i = 0; i < initRegisters.length; i++) {
            RegisterAllocator.Register r = initRegisters[i];
            emulator.run(String.format("input(%s,%d)", r, (1 << test.divisions[i]) * 128));
        }
        emulator.pushCode(code);
        emulator.flushInstructionBuffer();
        for (int i = 0; i < test.finalGoals.size(); i++) {

            Map<Tuple<Integer, Tuple<Integer, String>>, Double> testMap = emulator.getRawProcessingElementContains(0, 0, setup.availableRegisters[i].toString());

            Iterator<Tuple<Atom, Integer>> iterator = test.finalGoals.get(i).uniqueCountIterator();
            while (iterator.hasNext()){
                Tuple<Atom, Integer> t = iterator.next();
                Tuple<Integer, Tuple<Integer, String>> coordinate = Tuple.triple(t.getA().x, t.getA().y, RegisterAllocator.Register.values()[t.getA().z].toString());
                Double d = testMap.get(coordinate);
                int expected = t.getA().positive? t.getB(): -t.getB();
                if(d == null || Double.compare(expected, d) != 0){
                    System.out.println("INTEGRITY CHECK ERROR");
                    System.out.println(coordinate);
                    System.out.println(d);
                    System.out.println(expected);
                    return false;
                }
                testMap.remove(coordinate);
            }
            if(!testMap.isEmpty()){
                System.out.println("INTEGRITY CHECK ERROR!");
                System.out.println(testMap);
                return false;
            }

        }
        return true;
    }


}
