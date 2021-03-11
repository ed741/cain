package uk.co.edstow.cain.fileRun;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.reflections.Reflections;
import uk.co.edstow.cain.ReverseSearch;
import uk.co.edstow.cain.pairgen.Generator;
import uk.co.edstow.cain.regAlloc.*;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.Plan;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.traversal.*;
import uk.co.edstow.cain.util.Tuple;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;

public abstract class FileRun<G extends Goal<G>, T extends Transformation<R>, R extends Register> {
    public static int verbose = 0;
    public static Map<String, TargetResolver> register;
    static {
//        System.out.println("Init register");
        int count = 0;
        register = new HashMap<>();
        Reflections reflections = new Reflections(new org.reflections.scanners.MethodAnnotationsScanner());
        @SuppressWarnings("rawtypes") Set<Constructor> constructors = reflections.getConstructorsAnnotatedWith(FileRunImplementation.class);
        for (Constructor<?> constructor : constructors) {
            FileRunImplementation[] fris = constructor.getDeclaredAnnotationsByType(FileRunImplementation.class);
            for (FileRunImplementation fr : fris) {
                if (constructor.getParameterCount() > 2) throw new ImproperFileRunDeclaredException(constructor, "Constructor Parameter Count Error");
                if (constructor.getParameterCount() == 2 && fr.arg().equals("")) throw new ImproperFileRunDeclaredException(constructor, "Arg Parameter Cannot be empty String when required");
                if (constructor.getParameterCount() == 1 && !fr.arg().equals("")) throw new ImproperFileRunDeclaredException(constructor, "Arg Parameter Must not be set when not required");
                if (constructor.getParameterCount() == 0 ) throw new ImproperFileRunDeclaredException(constructor, "Constructor Parameter Count Error");
                if (constructor.getParameterTypes()[0] != JSONObject.class) throw new ImproperFileRunDeclaredException(constructor, "Constructor Parameter[0] Arg-Type Error, must accept JSON object");
                if (constructor.getParameterCount() == 2 && constructor.getParameterTypes()[1] != String.class) throw new ImproperFileRunDeclaredException(constructor,  "Constructor Parameter[1] Arg-Type Error, must accept String");
                if (fr.fields().length != fr.values().length) throw new ImproperFileRunDeclaredException(constructor,  "Fields to Check and expected values are different lengths");
//                StringBuilder sb = new StringBuilder();
//                sb.append("Registering: ");
//                sb.append(fr.key());
//                sb.append(" :: ");
//                sb.append(fr.arg());
//                sb.append(" :: {");
//                for (int i = 0; i < fr.fields().length; i++) {
//                    if(i > 0) {sb.append(",");}
//                    sb.append('"');
//                    sb.append(fr.fields()[i]);
//                    sb.append("\":\"");
//                    sb.append(fr.values()[i]);
//                    sb.append('"');
//                }
//                sb.append("}");
//                System.out.println(sb.toString());
                register.compute(fr.key(), (s, targetResolver) -> {
                    if (targetResolver==null) {targetResolver = new TargetResolver(s);}
                    targetResolver.add(fr, constructor);
                    return targetResolver;
                });
                count++;
            }
        }
//        System.out.println("Registered " + count + " Implementations");
    }

    private static class TargetResolver {
        private final String target;
        private final List<Tuple<FileRunImplementation, Constructor<?>>> constructors;

        TargetResolver(String target) {
            this.target = target;
            constructors = new ArrayList<>();
        }
        void add(FileRunImplementation fri, Constructor<?> constructor){
            assert (fri.key().equals(target));
            constructors.add(new Tuple<>(fri, constructor));
        }
        FileRun<?,?,?> get(JSONObject jsonObject){
            Tuple<FileRunImplementation, Constructor<?>> tuple = null;
            for (Tuple<FileRunImplementation, Constructor<?>> constructor : constructors) {
                boolean match = true;
                for (int i = 0; i < constructor.getA().fields().length; i++) {
                    String field = constructor.getA().fields()[i];
                    String value = constructor.getA().values()[i];
                    if(jsonObject.has(field)){
                        if(jsonObject.get(field) instanceof String){
                            String v = jsonObject.getString(field);
                            match &= value.equals(v);
                        } else throw new IllegalArgumentException("JSON File Error: " +field + " Must be a String field");
                    }
                }
                if(match){
                    if(tuple == null){
                        tuple = constructor;
                    } else {
                        throw new ImproperFileRunDeclaredException(constructor.getB(), "FileRunImplementation Redefinition Error, Fields overlap with another Implementation");
                    }
                }
            }
            if (tuple == null){
                throw new IllegalArgumentException("No target matches the given Json file");
            } else {
                try {
                    if (tuple.getB().getParameterCount() == 2) {
                        return (FileRun<?, ?, ?>) tuple.getB().newInstance(jsonObject, tuple.getA().arg());
                    } else {
                        return (FileRun<?, ?, ?>) tuple.getB().newInstance(jsonObject);
                    }
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    throw new ImproperFileRunDeclaredException(tuple.getB(), "Call Failed");
                }
            }
        }

    }

    public static FileRun<?,?,?> loadFromJson(String path) {

        JSONObject config = fromJson(path);
        verbose = config.has("verbose") ? config.getInt("verbose") : 10;
        printLn("Json config read from   : '" + path + "'");
        printLn("Name                    : " + config.getString("name"));


        String target = config.getString("target");
        TargetResolver resolver = register.getOrDefault(target, new TargetResolver(target));
        FileRun<?, ?, ?> fileRun = resolver.get(config);
        return fileRun;

    }


    protected final JSONObject config;
    protected final List<G> finalGoals;
    protected final List<G> initialGoals;
    protected final RegisterAllocator<G, T, R> registerAllocator;
    protected final Generator<G,T,R> pairGenFactory;
    protected final ReverseSearch<G,T,R> reverseSearch;
    protected final Verifier<G,T,R> verifier;


    public FileRun(JSONObject config) {
        this.config = config;

        printLn("making finalGoals");
        finalGoals = makeFinalGoals();
        printLn("making InitialGoals");
        initialGoals = makeInitialGoals();
        printLn("making RegisterAllocator");
        registerAllocator = makeRegisterAllocator();
        printLn("making RunConfig");
        ReverseSearch.RunConfig<G,T,R> runConfig = makeRunConfig(config.getJSONObject("runConfig"));
        printLn("making PairGenFactory");
        pairGenFactory = makeGenerator();


        printLn("Initialising Reverse Search:");
        reverseSearch = new ReverseSearch<>(initialGoals, finalGoals, pairGenFactory, runConfig, registerAllocator);

        verifier = makeVerifier();
    }

    protected abstract List<G> makeFinalGoals();

    protected abstract List<G> makeInitialGoals();

    protected abstract RegisterAllocator<G,T,R> makeRegisterAllocator();

    protected abstract Generator<G,T,R> makeGenerator();

    protected abstract Verifier<G,T,R> makeVerifier();

    protected ReverseSearch.RunConfig<G,T,R> makeRunConfig(JSONObject json) {
        printLn("\tMaking RunConfig:");
        ReverseSearch.RunConfig<G,T,R> runConfig = new ReverseSearch.RunConfig<>();
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

        Function<Plan<G,T,R>, Integer> costFunction;
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
                printLn("Traversal Algorithm         : Stow-Optimised-Traversal-N:" + n);
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
        runConfig.setAllowableSumTotalCoefficient(json.getInt("allowableAtomsCoefficient"));

        printLn("AtomGoal Reductions Per Step    : " + json.getInt("goalReductionsPerStep"));
        runConfig.setGoalReductionsPerStep(json.getInt("goalReductionsPerStep"));

        printLn("AtomGoal Reductions Tolerance   : " + json.getInt("goalReductionsTolerance"));
        runConfig.setGoalReductionsTolerance(json.getInt("goalReductionsTolerance"));

        return runConfig;
    }


    public void run() {
        reverseSearch.search();
        reverseSearch.printStats();
    }


    public String getBest() {
        List<Plan<G,T,R>> plans = reverseSearch.getPlans();
        if (plans.isEmpty()) {
            printLnCritial("No Plans Found!");
            return null;
        }
        double min = Double.MAX_VALUE;
        int iMin = 0;
        for (int i = 0; i < plans.size(); i++) {
            Plan<G,T,R> pl = plans.get(i);
            double c = reverseSearch.costFunction.apply(pl);
            if (c < min) {
                iMin = i;
                min = c;
            }
        }
        printLn("Best Plan: ");
        Plan<G,T,R> p = plans.get(iMin);
        printLn(p.toString());
        printLn(p.toGoalsString());

        printLnImportant("length: " + p.depth() + " Cost: " + reverseSearch.costFunction.apply(p));
        printLnImportant("CircuitDepths:" + Arrays.toString(p.circuitDepths()));
        RegisterAllocator.Mapping<G,R> mapping = registerAllocator.solve(p);
        String code = p.produceCode(mapping);
        printLnCritial(code);
        Verifier.VerificationResult verf = verifier.verify(code, initialGoals, finalGoals, p, registerAllocator);
        if (!verf.passed()) {
            printLnCritial("Plan Was Faulty!");
            return null;
        } else {
            printLn(verf.getInfo());
        }
        return code;
    }

    public List<Result<G,T,R>> getResults() {
        List<Result<G,T,R>> results = new ArrayList<>();
        List<Plan<G,T,R>> plans = reverseSearch.getPlans();
        for (int i = 0; i < plans.size(); i++) {
            Plan<G,T,R> plan = plans.get(i);
            RegisterAllocator.Mapping<G,R> mapping = registerAllocator.solve(plan);
            String code = plan.produceCode(mapping);
            Verifier.VerificationResult verf = verifier.verify(code, initialGoals, finalGoals, plan, registerAllocator);
            results.add(new Result<>(this,
                    plans.get(i),
                    reverseSearch.getPlanNodesExplored().get(i),
                    reverseSearch.getPlanTimes().get(i),
                    code,
                    verf));
        }
        return results;
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
        if (setVerbose) {
            FileRun.verbose = verbose;
            if (verbose > 5) {
                System.out.println("Verbose set to " + verbose);
            }
        }


        return config;
    }


    public static void printLnVerbose(String s) {
        if (verbose > 10) System.out.println(s);
    }

    public static void printLnVerbose(String s, Object... args) {
        if (verbose > 10) System.out.printf((s) + "%n", args);
    }

    public static void printLn(String s) {
        if (verbose > 5) System.out.println(s);
    }

    public static void printLn(String s, Object... args) {
        if (verbose > 5) System.out.printf((s) + "%n", args);
    }

    public static void printLnImportant(String s) {
        if (verbose > 0) System.out.println(s);
    }

    public static void printLnImportant(String s, Object... args) {
        if (verbose > 0) System.out.printf((s) + "%n", args);
    }

    public static void printLnCritial(String s) {
        if (verbose >= 0) System.out.println(s);
    }

    public static void printLnCritial(String s, Object... args) {
        if (verbose >= 0) System.out.printf((s) + "%n", args);
    }
}
