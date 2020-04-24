package uk.co.edstow.cpacgen;

import uk.co.edstow.cpacgen.scamp5.Scamp5PairGenFactory;
import uk.co.edstow.cpacgen.util.Bounds;

import java.util.*;

import static uk.co.edstow.cpacgen.RegisterAllocator.Register.*;
import static uk.co.edstow.cpacgen.scamp5.Scamp5PairGenFactory.Config.SearchStrategy.Exhuastive;
import static uk.co.edstow.cpacgen.scamp5.Scamp5PairGenFactory.Config.SearchStrategy.SortedAtomDistance;

public class DemoSuite {
    private static class Test{
        final String name;
        final List<Goal> finalGoals;
        final int divisions;
        Plan result;
        final String aukeScore;

        private Test(String name, List<Goal> finalGoals, int divisions) {
            this.name = name;
            this.finalGoals = finalGoals;
            this.divisions = divisions;
            this.aukeScore = "";
        }

        public Test(String name, List<Goal> finalGoals, int divisions, String aukeScore) {
            this.name = name;
            this.finalGoals = finalGoals;
            this.divisions = divisions;
            this.aukeScore = aukeScore;
        }
    }

    private static final List<Test> demos = new ArrayList<>();

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

    private static void initialiseDemosList(){
        {
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
        {
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
        {
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
        {
            Random rand = new Random(2001);
            {
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
            {
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
    }

    public static void main(String[] args) {
        initialiseDemosList();


        for (Test demo : demos) {
            runFilter(demo);
        }

        for (Test demo : demos) {
            System.out.println(demo.name  + " -> " + demo.result.cost() + " ("+demo.result.depth()+ ")");
            System.out.println(demo.result.getAll().get(0).toGoalsString(Collections.emptyList()));

        }

        System.out.println(makeLatexTable(demos));



    }


    private static void runFilter(Test test){
        System.out.println("Running: "+ test.name);
        RegisterAllocator.Register[] availableRegisters = new RegisterAllocator.Register[]{A, B, C, D, E, F};

        Scamp5PairGenFactory pairGenFactory = new Scamp5PairGenFactory(
                (goals, depth, rs1, initalGoal) -> {
                    int max = Integer.MIN_VALUE;
                    for (Goal goal : goals) {
                        max = Math.max(max, goal.atomCount());
                    }
                    int threshold = 10;
                    Scamp5PairGenFactory.Config conf = new Scamp5PairGenFactory.Config(max>threshold? SortedAtomDistance: Exhuastive, availableRegisters.length, depth);
                    conf.useAll();
                    conf.useSubPowerOf2();
                    return conf;
                }
        );
        RegisterAllocator ra = new RegisterAllocator(A, availableRegisters);

        ReverseSearch.RunConfig config = new ReverseSearch.RunConfig();
        config.setSearchTime(60000).setWorkers(4).setRegisterAllocator(ra).setTimeOut(true);
        ReverseSearch rs = new ReverseSearch(test.divisions, test.finalGoals, pairGenFactory, config);
        rs.search();

        double min = Double.MAX_VALUE;
        int imin = 0;
        for (int i = 0; i < rs.getPlans().size(); i ++){
            Plan pl = rs.getPlans().get(i);
            if(pl.cost() < min){
                imin = i;
                min = pl.cost();
            }
        }
        rs.printStats();

        System.out.println("Best:");
        Plan p = rs.getPlans().get(imin);
        System.out.println("length: " + p.depth() + " Cost: "+p.cost());
        System.out.println(p);
        System.out.println("CircuitDepths:" + Arrays.toString(p.circuitDepths()));
        //System.out.println(p.toGoalsString());
        RegisterAllocator.Mapping mapping = ra.solve(p);
        //System.out.println(mapping);
        System.out.println(p.produceCode(mapping));
        System.out.println(p.getAll().get(0).toGoalsString(Collections.emptyList()));
        test.result = p;

    }

    private static String makeLatexTable(List<Test> demos){
        StringBuilder sb = new StringBuilder();
        sb.append("\\begin{longtable}{| c c | c c |}\n");
        sb.append("\\caption{Kernels Tested in AUKE and CPACGen}\n");
        sb.append("\\label{table:kernels}\n");
        sb.append("\\centering\n");
        sb.append("\\hline\n");
        sb.append("\\multirow{2}{*}{Name} & \\multirow{2}{*}{Approximated Kernel} & \\multicolumn{2}{|c|}{Instruction Count} \\\\\n");
        //sb.append("\\hline\n");
        sb.append("& & AUKE & CPACGen\\\\ \n");
        for (Test demo : demos) {
            sb.append("\\hline\n");
            sb.append(demo.name.replace("%", "\\%").replace("&", "\\&")).append(" & ");
            List<String> filters = goalsToLatex(demo.finalGoals);
            for (int i = 0; i < filters.size(); i++) {
                if(demo.divisions>0) sb.append("$\\frac{1}{").append(1 << demo.divisions).append("}$");
                sb.append(filters.get(i));
                if(i != filters.size()-1) sb.append(",");
            }
            sb.append("&");

            if(demo.aukeScore != null && demo.aukeScore.length() > 0) {
                sb.append(demo.aukeScore);
            } else {
                sb.append("Unknown");
            }
            sb.append("&");
            if(demo.result != null) {
                sb.append(demo.result.depth());
            } else {
                sb.append("Unknown");
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
        int height = 1 + b.yMax - b.yMin;
        int width = 1 + b.xMax - b.xMin;
        List<String> out = new ArrayList<>(goals.size());
        for (Goal goal : goals) {
            String[][] table = goal.getCharTable(b, width, height, false, false, false, false);
            StringBuilder sb = new StringBuilder("$\\begin{bmatrix} ");

            for (int j = height; j > 0; j--) {
                for (int i = 1; i < table[j].length-1; i++) {
                    sb.append(table[j][i]);
                    sb.append(i == table[j].length-2?" \\\\ ":" & ");
                }
            }
            sb.append("\\end{bmatrix}$");
            out.add(sb.toString());
        }
        return out;
    }
}
