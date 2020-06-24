package uk.co.edstow.cain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.scamp5.Scamp5Config;
import uk.co.edstow.cain.scamp5.Scamp5PairGenFactory;
import uk.co.edstow.cain.scamp5.ThresholdConfigGetter;
import uk.co.edstow.cain.scamp5.emulator.Scamp5Emulator;
import uk.co.edstow.cain.structures.Atom;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.Plan;
import uk.co.edstow.cain.traversal.*;
import uk.co.edstow.cain.util.Tuple;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class FileRun {


    public class Result{
        public final Plan plan;
        public final long nodesExpanded;
        public final int cost;
        public final int depth;
        public final int[] circuitDepths;
        public final long time;
        public final String code;
        public final Atom.Bounds bounds;
        public final List<Goal> initialGoals;
        public final List<Goal> finalGoals;
        public final double error;

        public Result(Plan plan, long nodesExpanded, long time, String code, Atom.Bounds b, double error) {
            this.plan = plan;
            this.nodesExpanded = nodesExpanded;
            this.time = time;
            this.cost = reverseSearch.costFunction.apply(plan);
            this.depth = plan.depth();
            this.circuitDepths = plan.circuitDepths();
            this.code = code;
            this.bounds = b;
            this.initialGoals = reverseSearch.getInitialGoals();
            this.finalGoals = reverseSearch.getFinalGoals();
            this.error= error;

        }
    }

    private List<RegisterAllocator.Register> outputRegisters;
    private int approximationDepth;
    private int verbose;
    private final ReverseSearch reverseSearch;
    private RegisterAllocator registerAllocator;
    private final JSONObject config;

    public FileRun(String path) {
        this.config = fromJson(path);
        verbose = config.has("verbose")? config.getInt("verbose"):10;
        printLn("Json config read from   : '"+ path +"'" );
        printLn("Name                    : "+ config.getString("name"));

        List<Goal> finalGoals = makeFinalGoals(config);

        registerAllocator = makeRegisterAllocator(config);
        printLn("");
        ReverseSearch.RunConfig runConfig = makeRunConfig(config.getJSONObject("runConfig"), registerAllocator);
        printLn("");
        PairGenFactory pairGenFactory = makePairGenFactory(config.getJSONObject("pairGen"), registerAllocator);
        printLn("");

        int[] divisions = new int[registerAllocator.getInitRegisters().length];
        Arrays.fill(divisions, approximationDepth);

        printLn("Initialising Reverse Search:");
        reverseSearch = new ReverseSearch(divisions, finalGoals, pairGenFactory, runConfig);

    }


    public FileRun(String path, List<Goal> finalGoals, int approximationDepth) {
        this(fromJson(path), finalGoals, approximationDepth);
    }
    public FileRun(JSONObject config, List<Goal> finalGoals, int approximationDepth) {
        this.config = config;
        verbose = config.has("verbose")? config.getInt("verbose"):10;
        printLn("Name                    : "+ config.getString("name"));

        configureFinalGoals(finalGoals, approximationDepth);

        registerAllocator = makeRegisterAllocator(config);
        printLn("");
        ReverseSearch.RunConfig runConfig = makeRunConfig(config.getJSONObject("runConfig"), registerAllocator);
        printLn("");
        PairGenFactory pairGenFactory = makePairGenFactory(config.getJSONObject("pairGen"), registerAllocator);
        printLn("");

        int[] divisions = new int[registerAllocator.getInitRegisters().length];
        Arrays.fill(divisions, this.approximationDepth);

        printLn("Initialising Reverse Search:");
        reverseSearch = new ReverseSearch(divisions, finalGoals, pairGenFactory, runConfig);

    }

    public void run() {
        reverseSearch.search();
        reverseSearch.printStats();
    }
    public String getBest(){
        List<Plan> plans = reverseSearch.getPlans();
        if (plans.isEmpty()){
            printLnCritial("No Plans Found!");
            return null;
        }
        double min = Double.MAX_VALUE;
        int iMin = 0;
        for (int i = 0; i < plans.size(); i++) {
            Plan pl = plans.get(i);
            double c = reverseSearch.costFunction.apply(pl);
            if (c < min) {
                iMin = i;
                min = c;
            }
        }
        printLn("Best Plan: ");
        Plan p = plans.get(iMin);
        printLn(p.toString());

        printLnImportant("length: " + p.depth() + " Cost: " + reverseSearch.costFunction.apply(p));
        printLnImportant("CircuitDepths:" + Arrays.toString(p.circuitDepths()));
        RegisterAllocator.Mapping mapping = registerAllocator.solve(p, reverseSearch.getInitialGoals());
        String code = p.produceCode(mapping);
        printLnCritial(code);
        Tuple<Atom.Bounds,Double> b = checkPlan(code, p);
        if(b==null){
            printLnCritial("Plan Was Faulty!");
            return null;
        }
        printLn("Implemented filter:");
        int fgs = reverseSearch.getInitialGoals().size();
        printLn(GoalBag.toGoalsString(reverseSearch.getFinalGoals(), b.getA(), new boolean[fgs], new boolean[fgs], true, true));
        return code;
    }

    public List<Result> getResults(){
        List<Result> results = new ArrayList<>();
        List<Plan> plans = reverseSearch.getPlans();
        for (int i = 0; i < plans.size(); i++) {
            Plan plan = plans.get(i);
            RegisterAllocator.Mapping mapping = registerAllocator.solve(plan, reverseSearch.getInitialGoals());
            String code = plan.produceCode(mapping);
            Tuple<Atom.Bounds,Double> b = checkPlan(code, plan);
            Atom.Bounds coverage = b.getA();
            double noise = b.getB();
            results.add(new Result(
                    plans.get(i),
                    reverseSearch.getPlanNodesExplored().get(i),
                    reverseSearch.getPlanTimes().get(i),
                    code,
                    coverage, noise));
        }
        return results;
    }

    private void configureFinalGoals(List<Goal> finalGoals, int approximationDepth){

        RegisterAllocator.Register[] availableRegisters = getRegisterArray(config.getJSONArray("availableRegisters"));
        outputRegisters = new ArrayList<>();
        for (int i = 0; i < finalGoals.size(); i++) {
            outputRegisters.add(availableRegisters[i]);
        }
        this.approximationDepth = approximationDepth;
        printLn("\tgoals:");
        printLn(GoalBag.toGoalsString(finalGoals, false, false, true, true));
        printLn("Depth                   : "+ this.approximationDepth);
        printLn("Output Registers        : " + outputRegisters.toString());

    }
    private List<Goal> makeFinalGoals(JSONObject config){

        int maxApproximationDepth = config.getInt("maxApproximationDepth");
        printLn("Max Approximation Depth : "+ maxApproximationDepth);
        double maxApproximationError = config.getDouble("maxApproximationError");
        printLn("Max Approximation Error :RegisterAllocator.Register.values() "+maxApproximationError);

        Approximater goalAprox = new Approximater(maxApproximationDepth, maxApproximationError);
        outputRegisters = new ArrayList<>();

        boolean threeDimentional = config.getBoolean("3d");
        printLn("Three Dimensional       : "+threeDimentional);
        JSONObject filter = config.getJSONObject("filter");
        printLn("Kernels                 : " + filter.length());
        Iterator<String> filters = filter.keySet().stream().sorted().iterator();
        while (filters.hasNext()){
            String reg = filters.next();
            Object o = filter.get(reg);
            if(o instanceof JSONArray) {
                addGoal(goalAprox, (JSONArray) o, threeDimentional, 1);
            } else {
                double scale = 1;
                if (filter.getJSONObject(reg).has("scale")){
                    scale = filter.getJSONObject(reg).getDouble("scale");
                }
                if (filter.getJSONObject(reg).has("depth")){
                    scale *= Math.pow(2,filter.getJSONObject(reg).getDouble("depth"));
                }
                addGoal(goalAprox,filter.getJSONObject(reg).getJSONArray("array"), threeDimentional, scale );
            }
            outputRegisters.add(RegisterAllocator.Register.valueOf(reg));
        }

        List<Goal> finalGoals = goalAprox.solve();
        this.approximationDepth = goalAprox.getDepth();
        printLn("Output Registers        : " + outputRegisters.toString());
        printLn("\tApproximated goals:");
        printLn(GoalBag.toGoalsString(finalGoals, false, false, true, true));
        printLn("");
        printLn("Approximation Depth     : "+ goalAprox.getDepth());
        printLn("Approximation Error     : "+ goalAprox.getError());
        return finalGoals;
    }

    private RegisterAllocator makeRegisterAllocator(JSONObject config) {
        printLn("\tMaking Register Allocator:");
        RegisterAllocator.Register[] availableRegisters = getRegisterArray(config.getJSONArray("availableRegisters"));
        printLn("Available registers  : "+Arrays.toString(availableRegisters));

        List<RegisterAllocator.Register> available = new ArrayList<>(outputRegisters);
        for (RegisterAllocator.Register availableRegister : availableRegisters) {
            if (!available.contains(availableRegister)) {
                available.add(availableRegister);
            }
        }
        availableRegisters = available.toArray(availableRegisters);
        RegisterAllocator.Register[] initRegisters = getRegisterArray(config.getJSONArray("initialRegisters"));
        printLn("Initial registers    : "+Arrays.toString(initRegisters));
        return new RegisterAllocator(initRegisters, availableRegisters);
    }

    public int getAvailableRegisterCount() {
        return registerAllocator.getAvailableRegisters();
    }

    public String getAvailableRegisters() {
        return Arrays.toString(registerAllocator.getAvailableRegistersArray());
    }

    private PairGenFactory makePairGenFactory(JSONObject json, RegisterAllocator registerAllocator){
        printLn("\t Making Pair Generation Factory:");
        printLn("Name                        : "+ json.getString("name"));
        switch (json.getString("name")){
            default:
                throw new IllegalArgumentException("Unknown PairGen Factory "+ json.getString("name"));
            case "Scamp5":
                return makeScamp5PairGen(json, registerAllocator);
        }
    }

    private PairGenFactory makeScamp5PairGen(JSONObject json, RegisterAllocator registerAllocator) {
        printLn("Config Getter               : "+ json.getString("configGetter"));
        switch (json.getString("configGetter")){
            default:
                throw new IllegalArgumentException("Unknown Scamp5 ConfigGetter "+ json.getString("configGetter"));
            case "Threshold":
                printLn("Instruction to use          : "+ json.getString("ops"));
                Consumer<Scamp5Config> configConsumer;
                switch (json.getString("ops")){
                    default:
                        throw new IllegalArgumentException("Unknown Instuctions option "+  json.getString("ops"));
                    case "all":
                        configConsumer = c->c.useAll().useSubPowerOf2();
                        break;
                    case "basic":
                        configConsumer = c->c.useBasicOps().useSubPowerOf2();
                        break;
                }
                printLn("Exhustive Search Threshold  : "+ json.getInt("threshold"));
                return new Scamp5PairGenFactory<>(rs -> new ThresholdConfigGetter(rs, registerAllocator.getAvailableRegistersArray(), json.getInt("threshold"), configConsumer));
        }

    }

    private RegisterAllocator.Register[] getRegisterArray(JSONArray availableRegisters) {
        ArrayList<RegisterAllocator.Register> out = new ArrayList<>(availableRegisters.length());
        for (int i = 0; i < availableRegisters.length(); i++) {
            out.add(RegisterAllocator.Register.valueOf(availableRegisters.getString(i)));
        }
        return out.toArray(new RegisterAllocator.Register[availableRegisters.length()]);
    }

    private ReverseSearch.RunConfig makeRunConfig(JSONObject json, RegisterAllocator registerAllocator) {
        printLn("\tMaking RunConfig:");
        ReverseSearch.RunConfig runConfig = new ReverseSearch.RunConfig();
        //         Name                    :
        printLn("Search Time                 : "+json.getInt("searchTime"));
        runConfig.setSearchTime(json.getInt("searchTime"));
        printLn("Time Out                    : "+json.getBoolean("timeOut"));
        runConfig.setTimeOut(json.getBoolean("timeOut"));
        if(json.has("maxNodes")) {
            printLn("Max Nodes                   : "+json.getInt("maxNodes"));
            runConfig.setMaxNodes(json.getInt("maxNodes"));
        }
        printLn("Workers                     : "+json.getInt("workers"));
        runConfig.setWorkers(json.getInt("workers"));

        Function<Plan, Integer> costFunction;
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
                costFunction = p -> p.maxCircuitDepth()* maxDepth1+ p.depth();
                break;
            case "LengthThenCircuitDepth":
                printLn("Cost Function               : Plan Length, then Maximum CircuitDepth");
                final int maxDepth2 = json.getInt("initialMaxDepth");
                costFunction = p -> p.depth()*maxDepth2 + p.maxCircuitDepth();
        }
        runConfig.setCostFunction(costFunction);

        switch (json.getString("traversalAlgorithm")) {
            default:
                throw new IllegalArgumentException("Unknown Traversal Algorithm");
            case "SOT":
                printLn("Traversal Algorithm         : Stow-Optimised-Traversal");
                runConfig.setTraversalAlgorithm(SOT.SOTFactory());
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
            case "BestFirstSearch":
                printLn("Traversal Algorithm         : Best-First-Search");
                runConfig.setTraversalAlgorithm(BestFirstSearch.BestFirstSearchFactory(ws -> (double)costFunction.apply(ws.currentPlan)));
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

        printLn("Goal Reductions Per Step    : " + json.getInt("goalReductionsPerStep"));
        runConfig.setGoalReductionsPerStep(json.getInt("goalReductionsPerStep"));

        printLn("Goal Reductions Tolerance   : " + json.getInt("goalReductionsTolerance"));
        runConfig.setGoalReductionsTolerance(json.getInt("goalReductionsTolerance"));

        runConfig.setRegisterAllocator(registerAllocator);

        return runConfig;
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


    private void addGoal(Approximater goalAprox, JSONArray jsonArray, boolean threeDimentional, double scale) {
        int xMax = 0;
        int yMax = jsonArray.length();
        int zMax = 0;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray row = jsonArray.getJSONArray(i);
            xMax = Math.max(xMax, row.length());
            if(threeDimentional) {
                for (int j = 0; j < row.length(); j++) {
                    zMax = Math.max(zMax, row.getJSONArray(j).length());
                }
            }
        }

        int xOffset = xMax/2;
        int yOffset = yMax/2;
        int zOffset = zMax/2;

        goalAprox.newGoal();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray row = jsonArray.getJSONArray(i);
            for (int j = 0; j < row.length(); j++) {
                Object o = row.get(j);
                if(o instanceof JSONArray){
                    JSONArray array = (JSONArray) o;
                    for (int k = 0; k < array.length(); k++) {
                        double coefficant = array.getDouble(k);
                        int z = k;
                        goalAprox.put(j-xOffset, -(i-yOffset), z-zOffset, coefficant*scale);
                    }
                } else if(!threeDimentional){
                    double coefficant = row.getDouble(j);
                    goalAprox.put(j-xOffset, -(i-yOffset), 0, coefficant*scale);
                } else {
                    throw new IllegalArgumentException("Cannot parse kernel: "+jsonArray);
                }
            }
        }
    }


    public static JSONObject fromJson(String path){

        InputStream in = null;
        JSONTokener tokeniser = null;

        try {
            in = new FileInputStream(path);
            tokeniser = new JSONTokener(in);
        } catch (FileNotFoundException e) {
            try {
                tokeniser = new JSONTokener(path);
            } catch (JSONException ex){
                System.out.println("Cannot find file, or interpret as Json directly!");
                e.printStackTrace();
                ex.printStackTrace();
                System.exit(-1);
            }
        }
        JSONObject config = new JSONObject(tokeniser);
        int verbose = config.has("verbose")? config.getInt("verbose"):10;
        if(verbose>5 && in != null) {
            System.out.println("Json config read from   : '" + path + "'");
        }
        if(verbose>10 && in == null) {
            System.out.println("Json config:\n" + path + "\n");
        }



        return config;
    }


    private void printLn(String s){
        if(verbose>5) System.out.println(s);
    }

    private void printLn(String s, Object... args){
        if(verbose>5) System.out.println(String.format(s, args));
    }

    private void printLnImportant(String s){
        if(verbose>0) System.out.println(s);
    }

    private void printLnImportant(String s, Object... args){
        if(verbose>0) System.out.println(String.format(s, args));
    }

    private void printLnCritial(String s){
        if(verbose>=0) System.out.println(s);
    }

    private void printLnCritial(String s, Object... args){
        if(verbose>=0) System.out.println(String.format(s, args));
    }


    private Tuple<Atom.Bounds, Double> checkPlan(String code, Plan p){
        List<Goal> finalGoals = reverseSearch.getFinalGoals();
        int[] divisions = reverseSearch.getInitialDivisions();
        List<Atom.Bounds> coverage = new ArrayList<>();
        double noise =0;
        Scamp5Emulator emulator = Scamp5Emulator.newWithRegs((new Atom.Bounds(finalGoals).largestMagnitude()+1)*3, registerAllocator.getAvailableRegisters()<=6?6:24);
        RegisterAllocator.Register[] initRegisters = registerAllocator.getInitRegisters();
        for (int i = 0; i < initRegisters.length; i++) {
            RegisterAllocator.Register r = initRegisters[i];
            emulator.run(String.format("input(%s,%d)", r, (1 << divisions[i]) * 128));
        }
        emulator.pushCode(code);
        emulator.flushInstructionBuffer();
        for (int i = 0; i < finalGoals.size(); i++) {

            final String reg = registerAllocator.getAvailableRegistersArray()[i].toString();
            Map<Tuple<Integer, Tuple<Integer, String>>, Double> testMap = emulator.getRawProcessingElementContains(0, 0, reg);
            noise += emulator.readNoise(0,0,reg);

            Iterator<Tuple<Atom, Integer>> iterator = finalGoals.get(i).uniqueCountIterator();
            while (iterator.hasNext()){
                Tuple<Atom, Integer> t = iterator.next();
                Tuple<Integer, Tuple<Integer, String>> coordinate = Tuple.triple(t.getA().x, t.getA().y, RegisterAllocator.Register.values()[t.getA().z].toString());
                Double d = testMap.get(coordinate);
                int expected = t.getA().positive? t.getB(): -t.getB();
                if(d == null || Double.compare(expected, d) != 0){
                    printLnCritial("INTEGRITY CHECK ERROR");
                    printLnCritial(coordinate.toString());
                    printLnCritial("%s",d==null?"null":d);
                    printLnCritial("%s",expected);
                    printLnCritial(code);
                    printLnCritial(GoalBag.toGoalsString(finalGoals));
                    printLnCritial(p.toGoalsString());
                    return null;
                }
                testMap.remove(coordinate);
            }
            if(!testMap.isEmpty()){
                printLnCritial("INTEGRITY CHECK ERROR!");
                printLnCritial(testMap.toString());
                return null;
            }
            coverage.add(emulator.getRegCoverge(0,0,reg));

        }
        return new Tuple<>(Atom.Bounds.combine(coverage), noise);
    }

}
