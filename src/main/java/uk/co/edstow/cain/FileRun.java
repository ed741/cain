package uk.co.edstow.cain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.scamp5.Config;
import uk.co.edstow.cain.scamp5.Scamp5PairGenFactory;
import uk.co.edstow.cain.scamp5.ThresholdConfigGetter;
import uk.co.edstow.cain.scamp5.emulator.Scamp5Emulator;
import uk.co.edstow.cain.structures.Atom;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.traversal.*;
import uk.co.edstow.cain.util.Bounds;
import uk.co.edstow.cain.util.Tuple;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class FileRun {
    private final Approximater goalAprox;
    private final List<RegisterAllocator.Register> outputRegisters;
    private int verbose;
    private final ReverseSearch reverseSearch;
    private RegisterAllocator registerAllocator;

    public FileRun(String path) {
        JSONObject config = fromJson(path);
        verbose = config.has("verbose")? config.getInt("verbose"):10;
        printLn("Json config read from   : '"+ path +"'" );
        printLn("Name                    : "+ config.getString("name"));


        int maxApproximationDepth = config.getInt("maxApproximationDepth");
        printLn("Max Approximation Depth : "+ maxApproximationDepth);
        double maxApproximationError = config.getDouble("maxApproximationError");
        printLn("Max Approximation Error : "+maxApproximationError);

        goalAprox = new Approximater(maxApproximationDepth, maxApproximationError);
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
        printLn("Output Registers        : " + outputRegisters.toString());
        printLn("\tApproximated goals:");
        printLn(GoalBag.toGoalsString(finalGoals, false, false, true, true));
        printLn("");
        printLn("Approximation Depth     : "+ goalAprox.getDepth());
        printLn("Approximation Error     : "+ goalAprox.getError());

        registerAllocator = makeRegisterAllocator(config);
        printLn("");
        ReverseSearch.RunConfig runConfig = makeRunConfig(config.getJSONObject("runConfig"), registerAllocator);
        printLn("");
        PairGenFactory pairGenFactory = makePairGenFactory(config.getJSONObject("pairGen"), registerAllocator);
        printLn("");

        int[] divisions = new int[registerAllocator.getInitRegisters().length];
        for (int i = 0; i < divisions.length; i++) {
            divisions[i] = goalAprox.getDepth();
        }

        printLn("Initialising Reverse Search:");
        reverseSearch = new ReverseSearch(divisions, finalGoals, pairGenFactory, runConfig);


    }

    public void run(){
        reverseSearch.search();
        reverseSearch.printStats();
        List<Plan> plans = reverseSearch.getPlans();
        if (plans.isEmpty()){
            printLnCritial("No Plans Found!");
            System.exit(-1);
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
        Bounds b = checkPlan(code);
        if(b==null){
            printLnCritial("Plan Was Faulty!");
            System.exit(-1);
        }
        printLn("Implemented filter:");
        int fgs = reverseSearch.getInitialGoals().size();
        printLn(GoalBag.toGoalsString(reverseSearch.getFinalGoals(), b, new boolean[fgs], new boolean[fgs], true, true));



    }

    private RegisterAllocator makeRegisterAllocator(JSONObject config) {
        RegisterAllocator.Register[] availableRegisters = getRegisterArray(config.getJSONArray("availableRegisters"));
        List<RegisterAllocator.Register> available = new ArrayList<>(outputRegisters);
        for (RegisterAllocator.Register availableRegister : availableRegisters) {
            if (!available.contains(availableRegister)) {
                available.add(availableRegister);
            }
        }
        availableRegisters = available.toArray(availableRegisters);
        RegisterAllocator.Register[] initRegisters = getRegisterArray(config.getJSONArray("initialRegisters"));
        return new RegisterAllocator(initRegisters, availableRegisters);
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
                Consumer<Config> configConsumer;
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
        //runConfig.setCostFunction(costFunction);

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


    private JSONObject fromJson(String path){

        InputStream in;
        JSONTokener tokeniser = null;

        try {
            in = new FileInputStream(path);
            tokeniser = new JSONTokener(in);
        } catch (FileNotFoundException e) {
            try {
                tokeniser = new JSONTokener(path);
            } catch (JSONException ex){
                printLnCritial("Cannot find file, or interpret as Json directly!");
                e.printStackTrace();
                ex.printStackTrace();
                System.exit(-1);
            }
        }

        return new JSONObject(tokeniser);
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


    private Bounds checkPlan(String code){
        List<Goal> finalGoals = reverseSearch.getFinalGoals();
        int[] divisions = reverseSearch.getInitialDivisions();
        List<Bounds> bounds = new ArrayList<>();
        Scamp5Emulator emulator = new Scamp5Emulator(new Bounds(finalGoals).largestMagnitude()*2);
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
                    return null;
                }
                testMap.remove(coordinate);
            }
            if(!testMap.isEmpty()){
                printLnCritial("INTEGRITY CHECK ERROR!");
                printLnCritial(testMap.toString());
                return null;
            }
            bounds.add(emulator.getRegCoverge(0,0,reg));

        }
        return Bounds.combine(bounds);
    }
}
