package uk.co.edstow.cain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import uk.co.edstow.cain.pairgen.Config;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.scamp5.Scamp5Config;
import uk.co.edstow.cain.scamp5.analogue.Scamp5PairGenFactory;
import uk.co.edstow.cain.scamp5.ThresholdConfigGetter;
import uk.co.edstow.cain.atom.AtomGoal;
import uk.co.edstow.cain.scamp5.emulator.Scamp5Verifier;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.Plan;
import uk.co.edstow.cain.traversal.*;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class FileRun<G extends Goal<G>, C extends Config> {
    private static int verbose;

    public static FileRun<?,?> loadFromJson(String path){

        JSONObject config = fromJson(path);
        verbose = config.has("verbose")? config.getInt("verbose"):10;
        printLn("Json config read from   : '"+ path +"'" );
        printLn("Name                    : "+ config.getString("name"));

        String goalSystem = config.getString("goalSystem");
        switch (goalSystem){
            case "Atom":
                return AtomFileRun.getAtomFileRun(config);
            default:
                throw new IllegalArgumentException("GoalSystem Unknown");
        }
    }



    public class Result{
        public final Plan<G> plan;
        public final long nodesExpanded;
        public final int cost;
        public final int depth;
        public final int[] circuitDepths;
        public final long time;
        public final String code;
        public final List<G> initialGoals;
        public final List<G> finalGoals;
        public final String verificationOutput;

        public Result(Plan<G> plan, long nodesExpanded, long time, String code, String verf) {
            this.plan = plan;
            this.nodesExpanded = nodesExpanded;
            this.time = time;
            this.cost = reverseSearch.costFunction.apply(plan);
            this.depth = plan.depth();
            this.circuitDepths = plan.circuitDepths();
            this.code = code;
            this.initialGoals = reverseSearch.getInitialGoals();
            this.finalGoals = reverseSearch.getFinalGoals();
            this.verificationOutput = verf;

        }
    }


    protected List<RegisterAllocator.Register> outputRegisters;
    protected int approximationDepth;

    protected final ReverseSearch<G,C> reverseSearch;
    protected final List<G> initialGoals;
    protected RegisterAllocator<G> registerAllocator;
    protected final JSONObject config;
    protected final Verifier<G> verifier;


    public FileRun(JSONObject config) {
        this.config = config;

        List<G> finalGoals = makeFinalGoals(config);

        registerAllocator = makeRegisterAllocator(config);
        int[] divisions = new int[registerAllocator.getInitRegisters().length];
        Arrays.fill(divisions, approximationDepth);
        initialGoals = makeInitialGoals(divisions);

        printLn("");
        ReverseSearch.RunConfig<G> runConfig = makeRunConfig(config.getJSONObject("runConfig"), registerAllocator);
        printLn("");
        PairGenFactory<G,C> pairGenFactory = makePairGenFactory(config.getJSONObject("pairGen"), registerAllocator);
        printLn("");

        printLn("Initialising Reverse Search:");
        reverseSearch = new ReverseSearch<>(divisions, initialGoals, finalGoals, pairGenFactory, runConfig);

        verifier = makeVerifier(config);

    }


    public FileRun(String path, List<G> finalGoals, int approximationDepth) {
        this(fromJson(path), finalGoals, approximationDepth);
    }

    public FileRun(JSONObject config, List<G> finalGoals, int approximationDepth) {
        this.config = config;

        configureFinalGoals(finalGoals, approximationDepth);

        registerAllocator = makeRegisterAllocator(config);
        int[] divisions = new int[registerAllocator.getInitRegisters().length];
        Arrays.fill(divisions, this.approximationDepth);
        initialGoals = makeInitialGoals(divisions);

        printLn("");
        ReverseSearch.RunConfig<G> runConfig = makeRunConfig(config.getJSONObject("runConfig"), registerAllocator);
        printLn("");
        PairGenFactory<G,C> pairGenFactory = makePairGenFactory(config.getJSONObject("pairGen"), registerAllocator);
        printLn("");

        printLn("Initialising Reverse Search:");
        reverseSearch = new ReverseSearch<>(divisions, initialGoals, finalGoals, pairGenFactory, runConfig);
        verifier = makeVerifier(config);

    }

    protected abstract List<G> makeFinalGoals(JSONObject config);
    protected abstract List<G> makeInitialGoals(int[] divisions);
    protected abstract PairGenFactory<G,C> makePairGenFactory(JSONObject pairGen, RegisterAllocator<G> registerAllocator);
    protected abstract Verifier<G> makeVerifier(JSONObject config);

    protected RegisterAllocator<G> makeRegisterAllocator(JSONObject config) {
        printLn("\tMaking Register Allocator:");
        RegisterAllocator.Register[] availableRegisters = getRegisterArray(config.getJSONArray("availableRegisters"));
        printLn("Available registers  : " + Arrays.toString(availableRegisters));

        List<RegisterAllocator.Register> available = new ArrayList<>(outputRegisters);
        for (RegisterAllocator.Register availableRegister : availableRegisters) {
            if (!available.contains(availableRegister)) {
                available.add(availableRegister);
            }
        }
        availableRegisters = available.toArray(availableRegisters);
        RegisterAllocator.Register[] initRegisters = getRegisterArray(config.getJSONArray("initialRegisters"));
        printLn("Initial registers    : " + Arrays.toString(initRegisters));
        return new RegisterAllocator<>(initRegisters, availableRegisters);
    }

    protected void configureFinalGoals(List<G> finalGoals, int approximationDepth) {

        RegisterAllocator.Register[] availableRegisters = getRegisterArray(config.getJSONArray("availableRegisters"));
        outputRegisters = new ArrayList<>();
        for (int i = 0; i < finalGoals.size(); i++) {
            outputRegisters.add(availableRegisters[i]);
        }
        this.approximationDepth = approximationDepth;
        printLn("\tgoals:");
        printLn(GoalBag.toGoalsString(finalGoals, false, false, true, true));
        printLn("Depth                   : " + this.approximationDepth);
        printLn("Output Registers        : " + outputRegisters.toString());

    }

    protected ReverseSearch.RunConfig<G> makeRunConfig(JSONObject json, RegisterAllocator<G> registerAllocator) {
        printLn("\tMaking RunConfig:");
        ReverseSearch.RunConfig<G> runConfig = new ReverseSearch.RunConfig<>();
        //         Name                    :
        printLn("Search Time                 : " + json.getInt("searchTime"));
        runConfig.setSearchTime(json.getInt("searchTime"));
        printLn("Time Out                    : " + json.getBoolean("timeOut"));
        runConfig.setTimeOut(json.getBoolean("timeOut"));
        if (json.has("maxNodes")) {
            printLn("Max Nodes                   : " + json.getInt("maxNodes"));
            runConfig.setMaxNodes(json.getInt("maxNodes"));
        }
        printLn("Workers                     : " + json.getInt("workers"));
        runConfig.setWorkers(json.getInt("workers"));

        Function<Plan<G>, Integer> costFunction;
        switch (json.getString("costFunction")) {
            default:
                throw new IllegalArgumentException("Unknown Cost Function");
            case "CircuitDepth":
                printLn("Cost Function               : Maximum Circuit Depth");
                costFunction = Plan::maxCircuitDepth;
                break;
            case "PlanLength":
                printLn("Cost Function               : Plan Length");
                costFunction = Plan::depth;
                break;
            case "InstructionCost":
                printLn("Cost Function               : Total Instruction Cost");
                costFunction = plan -> (int) plan.totalInstructionCost();
                break;
            case "CircuitDepthThenLength":
                printLn("Cost Function               : Maximum CircuitDepth, then Plan Length");
                final int maxDepth1 = json.getInt("initialMaxDepth");
                costFunction = p -> p.maxCircuitDepth() * maxDepth1 + p.depth();
                break;
            case "LengthThenCircuitDepth":
                printLn("Cost Function               : Plan Length, then Maximum CircuitDepth");
                final int maxDepth2 = json.getInt("initialMaxDepth");
                costFunction = p -> p.depth() * maxDepth2 + p.maxCircuitDepth();
        }
        runConfig.setCostFunction(costFunction);

        switch (json.getString("traversalAlgorithm")) {
            default:
                throw new IllegalArgumentException("Unknown Traversal Algorithm");
            case "SOT":
                printLn("Traversal Algorithm         : Stow-Optimised-Traversal");
                runConfig.setTraversalAlgorithm(SOT.SOTFactory());
                break;
            case "SOTN":
                int n = json.getInt("SOTN");
                printLn("Traversal Algorithm         : Stow-Optimised-Traversal-N:"+n);
                runConfig.setTraversalAlgorithm(SOTN.SOTNFactory(n));
                break;
            case "BFS":
                printLn("Traversal Algorithm         : Breadth-First-Search");
                runConfig.setTraversalAlgorithm(BFS.BFSFactory());
                break;
            case "DFS":
                printLn("Traversal Algorithm         : Depth-First-Search");
                runConfig.setTraversalAlgorithm(DFS.DFSFactory());
                break;
            case "HOS":
                printLn("Traversal Algorithm         : Heir-Ordered-Search");
                runConfig.setTraversalAlgorithm(HOS.HOSFactory());
                break;
            case "RT":
                printLn("Traversal Algorithm         : Random-Traversal");
                runConfig.setTraversalAlgorithm(RT.RTFactory());
                break;
            case "BestFirstSearch":
                printLn("Traversal Algorithm         : Best-First-Search");
                runConfig.setTraversalAlgorithm(BestFirstSearch.BestFirstSearchFactory(ws -> (double) costFunction.apply(ws.currentPlan)));
                break;
        }

        printLn("Live Counter                : " + json.getBoolean("liveCounter"));
        runConfig.setLiveCounter(json.getBoolean("liveCounter"));

        printLn("Quiet                       : " + json.getBoolean("quiet"));
        runConfig.setQuiet(json.getBoolean("quiet"));

        printLn("Print Plans Live            : " + json.getInt("livePrintPlans"));
        runConfig.setLivePrintPlans(json.getInt("livePrintPlans"));

        printLn("Initial Max Depth           : " + json.getInt("initialMaxDepth"));
        runConfig.setInitialMaxDepth(json.getInt("initialMaxDepth"));

        printLn("Forced Depth Reduction      : " + json.getInt("forcedDepthReduction"));
        runConfig.setForcedDepthReduction(json.getInt("forcedDepthReduction"));

        printLn("Initial Max Cost            : " + json.getInt("initialMaxCost"));
        runConfig.setInitialMaxCost(json.getInt("initialMaxCost"));

        printLn("Forced Cost Reduction       : " + json.getInt("forcedCostReduction"));
        runConfig.setForcedCostReduction(json.getInt("forcedCostReduction"));

        printLn("Allowable Atoms Coefficient : " + json.getInt("allowableAtomsCoefficient"));
        runConfig.setAllowableAtomsCoefficient(json.getInt("allowableAtomsCoefficient"));

        printLn("AtomGoal Reductions Per Step    : " + json.getInt("goalReductionsPerStep"));
        runConfig.setGoalReductionsPerStep(json.getInt("goalReductionsPerStep"));

        printLn("AtomGoal Reductions Tolerance   : " + json.getInt("goalReductionsTolerance"));
        runConfig.setGoalReductionsTolerance(json.getInt("goalReductionsTolerance"));

        runConfig.setRegisterAllocator(registerAllocator);

        return runConfig;
    }






    public void run() {
        reverseSearch.search();
        reverseSearch.printStats();
    }


    public String getBest() {
        List<Plan<G>> plans = reverseSearch.getPlans();
        if (plans.isEmpty()) {
            printLnCritial("No Plans Found!");
            return null;
        }
        double min = Double.MAX_VALUE;
        int iMin = 0;
        for (int i = 0; i < plans.size(); i++) {
            Plan<G> pl = plans.get(i);
            double c = reverseSearch.costFunction.apply(pl);
            if (c < min) {
                iMin = i;
                min = c;
            }
        }
        printLn("Best Plan: ");
        Plan<G> p = plans.get(iMin);
        printLn(p.toString());

        printLnImportant("length: " + p.depth() + " Cost: " + reverseSearch.costFunction.apply(p));
        printLnImportant("CircuitDepths:" + Arrays.toString(p.circuitDepths()));
        RegisterAllocator<G>.Mapping mapping = registerAllocator.solve(p, reverseSearch.getInitialGoals());
        String code = p.produceCode(mapping);
        printLnCritial(code);
        String verf = verifier.verify(code, reverseSearch, p, registerAllocator);
        if (verf == null) {
            printLnCritial("Plan Was Faulty!");
            return null;
        } else {
            printLn(verf);
        }
        return code;
    }

    public List<Result> getResults() {
        List<Result> results = new ArrayList<>();
        List<Plan<G>> plans = reverseSearch.getPlans();
        for (int i = 0; i < plans.size(); i++) {
            Plan<G> plan = plans.get(i);
            RegisterAllocator<G>.Mapping mapping = registerAllocator.solve(plan, reverseSearch.getInitialGoals());
            String code = plan.produceCode(mapping);
            String verf = verifier.verify(code, reverseSearch, plan, registerAllocator);
            results.add(new Result(
                    plans.get(i),
                    reverseSearch.getPlanNodesExplored().get(i),
                    reverseSearch.getPlanTimes().get(i),
                    code,
                    verf));
        }
        return results;
    }


    private static abstract class AtomFileRun<C extends Config> extends FileRun<AtomGoal, C> {

        public static AtomFileRun<?> getAtomFileRun(JSONObject config){
            JSONObject json = config.getJSONObject("pairGen");
            switch (json.getString("name")) {
                default:
                    throw new IllegalArgumentException("Unknown PairGen Factory " + json.getString("name"));
                case "Scamp5":
                    return new Scamp5AtomFileRun(config);
            }
        }

        public AtomFileRun(JSONObject config) {
            super(config);
        }
        public AtomFileRun(JSONObject config, List<AtomGoal> finalGoals, int approximationDepth) {
            super(config, finalGoals, approximationDepth);
        }

        @Override
        protected List<AtomGoal> makeFinalGoals(JSONObject config) {

            int maxApproximationDepth = config.getInt("maxApproximationDepth");
            printLn("Max Approximation Depth : " + maxApproximationDepth);
            double maxApproximationError = config.getDouble("maxApproximationError");
            printLn("Max Approximation Error : " + maxApproximationError);

            Approximater goalAprox = new Approximater(maxApproximationDepth, maxApproximationError);


            boolean threeDimentional = config.getBoolean("3d");
            printLn("Three Dimensional       : " + threeDimentional);
            JSONObject filter = config.getJSONObject("filter");
            printLn("Kernels                 : " + filter.length());
            Iterator<String> filters = filter.keySet().stream().sorted().iterator();
            while (filters.hasNext()) {
                String reg = filters.next();
                Object o = filter.get(reg);
                if (o instanceof JSONArray) {
                    addGoal(goalAprox, (JSONArray) o, threeDimentional, 1);
                } else {
                    double scale = 1;
                    if (filter.getJSONObject(reg).has("scale")) {
                        scale = filter.getJSONObject(reg).getDouble("scale");
                    }
                    if (filter.getJSONObject(reg).has("depth")) {
                        scale *= Math.pow(2, filter.getJSONObject(reg).getDouble("depth"));
                    }
                    addGoal(goalAprox, filter.getJSONObject(reg).getJSONArray("array"), threeDimentional, scale);
                }
            }

            List<AtomGoal> finalGoals = goalAprox.solve();
            this.approximationDepth = goalAprox.getDepth();
            this.outputRegisters = getOutputRegisters(config);
            printLn("Output Registers        : " + this.outputRegisters.toString());
            printLn("\tApproximated goals:");
            printLn(GoalBag.toGoalsString(finalGoals, false, false, true, true));
            printLn("");
            printLn("Approximation Depth     : " + goalAprox.getDepth());
            printLn("Approximation Error     : " + goalAprox.getError());
            return finalGoals;
        }

        @Override
        protected List<AtomGoal> makeInitialGoals(int[] divisions) {
            List<AtomGoal> initialGoals = new ArrayList<>();
            for (int i = 0; i < divisions.length; i++) {
                int division = divisions[i];
                initialGoals.add(new AtomGoal.Factory().add(new int[]{0,0,i}, 1 << division).get());
            }
            return initialGoals;
        }

        protected Verifier<AtomGoal> makeVerifier(JSONObject config){
            String verf = config.getString("verifier");
            switch (verf){
                case "Scamp5Emulator":
                    Verifier<AtomGoal> v =  new Scamp5Verifier();
                    v.verbose(verbose);
                    return v;
                case "None":
                    return Verifier.SkipVerify();
                default:
                    throw new IllegalArgumentException("Verifier Unknown");
            }
        }

        private static void addGoal(Approximater goalAprox, JSONArray jsonArray, boolean threeDimentional, double scale) {
            int xMax = 0;
            int yMax = jsonArray.length();
            int zMax = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONArray row = jsonArray.getJSONArray(i);
                xMax = Math.max(xMax, row.length());
                if (threeDimentional) {
                    for (int j = 0; j < row.length(); j++) {
                        zMax = Math.max(zMax, row.getJSONArray(j).length());
                    }
                }
            }

            int xOffset = xMax / 2;
            int yOffset = yMax / 2;
            int zOffset = zMax / 2;

            goalAprox.newGoal();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONArray row = jsonArray.getJSONArray(i);
                for (int j = 0; j < row.length(); j++) {
                    Object o = row.get(j);
                    if (o instanceof JSONArray) {
                        JSONArray array = (JSONArray) o;
                        for (int k = 0; k < array.length(); k++) {
                            double coefficant = array.getDouble(k);
                            int z = k;
                            goalAprox.put(j - xOffset, -(i - yOffset), z - zOffset, coefficant * scale);
                        }
                    } else if (!threeDimentional) {
                        double coefficant = row.getDouble(j);
                        goalAprox.put(j - xOffset, -(i - yOffset), 0, coefficant * scale);
                    } else {
                        throw new IllegalArgumentException("Cannot parse kernel: " + jsonArray);
                    }
                }
            }
        }

    }

    public static class Scamp5AtomFileRun extends AtomFileRun<Scamp5Config<AtomGoal>> {

        public Scamp5AtomFileRun(JSONObject config) {
            super(config);
        }
        public Scamp5AtomFileRun(JSONObject config, List<AtomGoal> finalGoals, int approximationDepth) {
            super(config, finalGoals, approximationDepth);
        }

        @Override
        protected PairGenFactory<AtomGoal, Scamp5Config<AtomGoal>> makePairGenFactory(JSONObject json, RegisterAllocator<AtomGoal> registerAllocator) {
            printLn("\t Making Pair Generation Factory:");
            printLn("Name                        : " + json.getString("name"));
            printLn("Config Getter               : " + json.getString("configGetter"));
            switch (json.getString("configGetter")) {
                default:
                    throw new IllegalArgumentException("Unknown Scamp5 ConfigGetter " + json.getString("configGetter"));
                case "Threshold":
                    printLn("Instruction to use          : " + json.getString("ops"));
                    Consumer<Scamp5Config> configConsumer;
                    switch (json.getString("ops")) {
                        default:
                            throw new IllegalArgumentException("Unknown Instuctions option " + json.getString("ops"));
                        case "all":
                            configConsumer = c -> c.useAll().useSubPowerOf2();
                            break;
                        case "basic":
                            configConsumer = c -> c.useBasicOps().useSubPowerOf2();
                            break;
                    }
                    printLn("Exhustive Search Threshold  : " + json.getInt("threshold"));
                    return new Scamp5PairGenFactory<>(new ThresholdConfigGetter(initialGoals, registerAllocator.getAvailableRegistersArray(), json.getInt("threshold"), configConsumer));
            }

        }
    }


    public int getAvailableRegisterCount() {
        return registerAllocator.getAvailableRegisters();
    }

    public String getAvailableRegisters() {
        return Arrays.toString(registerAllocator.getAvailableRegistersArray());
    }

    public String getTraversalAlgorithm() {
        return config.getJSONObject("runConfig").getString("traversalAlgorithm");
    }

    public String getCostFunction() {
        return config.getJSONObject("runConfig").getString("costFunction");
    }

    public int getForcedDepthReduction() {
        return config.getJSONObject("runConfig").getInt("forcedDepthReduction");
    }

    public int getForcedCostReduction() {
        return config.getJSONObject("runConfig").getInt("forcedCostReduction");
    }


    private static RegisterAllocator.Register[] getRegisterArray(JSONArray availableRegisters) {
        ArrayList<RegisterAllocator.Register> out = new ArrayList<>(availableRegisters.length());
        for (int i = 0; i < availableRegisters.length(); i++) {
            out.add(new RegisterAllocator.Register(availableRegisters.getString(i)));
        }
        return out.toArray(new RegisterAllocator.Register[availableRegisters.length()]);
    }

    private static List<RegisterAllocator.Register> getOutputRegisters(JSONObject config) {
        if(config.has("filter")) {
            JSONObject filter = config.getJSONObject("filter");
            return filter.keySet().stream().map(RegisterAllocator.Register::new).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }
    public static JSONObject fromJson(String path) {
        return fromJson(path, false);
    }
    public static JSONObject fromJson(String path, boolean setVerbose) {

        InputStream in = null;
        JSONTokener tokeniser = null;

        try {
            in = new FileInputStream(path);
            tokeniser = new JSONTokener(in);
        } catch (FileNotFoundException e) {
            try {
                tokeniser = new JSONTokener(path);
            } catch (JSONException ex) {
                System.out.println("Cannot find file, or interpret as Json directly!");
                e.printStackTrace();
                ex.printStackTrace();
                System.exit(-1);
            }
        }
        JSONObject config = new JSONObject(tokeniser);
        int verbose = config.has("verbose") ? config.getInt("verbose") : 10;
        if (verbose > 5 && in != null) {
            System.out.println("Json config read from   : '" + path + "'");
        }
        if (verbose > 5 && in == null) {
            System.out.println("Json config read directly");
        }
        if (verbose > 10 && in == null) {
            System.out.println("Json config:\n" + path + "\n");
        }
        if(setVerbose){
            FileRun.verbose = verbose;
            if (verbose > 5) {
                System.out.println("Verbose set to "+ verbose);
            }
        }



        return config;
    }


    private static void printLnVerbose(String s) {
        if (verbose > 10) System.out.println(s);
    }

    private static void printLnVerbose(String s, Object... args) {
        if (verbose > 10) System.out.println(String.format(s, args));
    }

    private static void printLn(String s) {
        if (verbose > 5) System.out.println(s);
    }

    private static void printLn(String s, Object... args) {
        if (verbose > 5) System.out.println(String.format(s, args));
    }

    private static void printLnImportant(String s) {
        if (verbose > 0) System.out.println(s);
    }

    private static void printLnImportant(String s, Object... args) {
        if (verbose > 0) System.out.println(String.format(s, args));
    }

    private static void printLnCritial(String s) {
        if (verbose >= 0) System.out.println(s);
    }

    private static void printLnCritial(String s, Object... args) {
        if (verbose >= 0) System.out.println(String.format(s, args));
    }
}
