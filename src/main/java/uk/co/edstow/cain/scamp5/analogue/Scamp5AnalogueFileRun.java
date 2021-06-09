package uk.co.edstow.cain.scamp5.analogue;

import org.json.JSONObject;
import uk.co.edstow.cain.fileRun.FileRun;
import uk.co.edstow.cain.fileRun.FileRunImplementation;
import uk.co.edstow.cain.fileRun.Kernel3DStdTransFileRun;
import uk.co.edstow.cain.fileRun.Verifier;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.arrayGoal.ArrayGoal;
import uk.co.edstow.cain.goals.atomGoal.AtomGoal;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.pairgen.Generator;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.pairgen.ThresholdPairGen;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.regAlloc.RegisterAllocator;
import uk.co.edstow.cain.scamp5.*;
import uk.co.edstow.cain.scamp5.emulator.Scamp5AnalogueVerifier;
import uk.co.edstow.cain.scamp5.output.Scamp5DefaultOutputFormatter;
import uk.co.edstow.cain.scamp5.output.Scamp5JssOutputFormatter;
import uk.co.edstow.cain.scamp5.output.Scamp5OutputFormatter;
import uk.co.edstow.cain.structures.Plan;

public abstract class Scamp5AnalogueFileRun<G extends Kernel3DGoal<G>> extends Kernel3DStdTransFileRun<G, Scamp5AnalogueTransformation<G>> {

    private Scamp5AnalogueConfig scamp5AnalogueConfig;

    public Scamp5AnalogueFileRun(JSONObject config) {
        super(config);
    }

    @Override
    protected boolean isThreeDimensional() {
        return false;
    }

    @Override
    protected Generator<G, Scamp5AnalogueTransformation<G>, Register> makeGenerator() {
        JSONObject json = config.getJSONObject("pairGen");
        printLn("\t Making Pair Generation Factory:");

        Scamp5AnalogueConfig.Builder configBuilder = new Scamp5AnalogueConfig.Builder();

        Scamp5OutputFormatter outputFormatter;
        if (!json.has("outputFormat")) {
            throw new IllegalArgumentException("you need to define outputFormat inside pairGen");
        }
        JSONObject outputFormatConfig = json.getJSONObject("outputFormat");
        switch (outputFormatConfig.getString("name")) {
            default:
                throw new IllegalArgumentException("Unknown Scamp5 outputFormat : " + outputFormatConfig.getString("name"));
            case "defaultFormat":
                boolean refreshDNEWS = outputFormatConfig.optBoolean("refreshDNEWS", false);
                outputFormatter = new Scamp5DefaultOutputFormatter(refreshDNEWS);
                break;
            case "jssFormat":
                String jssSimulatorName = outputFormatConfig.getString("simulatorName");
                outputFormatter = new Scamp5JssOutputFormatter(jssSimulatorName);
                break;
        }
        configBuilder.setOutputFormatter(outputFormatter);

        String factory = "strategy";
        if(!json.has(factory)) {throw new IllegalArgumentException("you need to define " + factory + " inside pairGen");}
        JSONObject strategy = json.getJSONObject(factory);
        PairGenFactory<G, Scamp5AnalogueTransformation<G>, Register> pairGenFactory = buildPairGenFactory(strategy, configBuilder);
        scamp5AnalogueConfig = configBuilder.build();
        return new Generator<>(new Scamp5AnalogueDirectSolver<>(scamp5AnalogueConfig),pairGenFactory);

    }

    private PairGenFactory<G, Scamp5AnalogueTransformation<G>, Register> buildPairGenFactory(JSONObject strategy, Scamp5AnalogueConfig.Builder configBuilder) {
        PairGenFactory<G, Scamp5AnalogueTransformation<G>, Register> pairGenFactory;
        String configGetterName = strategy.getString("name");
        switch (configGetterName) {
            default:
                throw new IllegalArgumentException("Unknown Scamp5 strategy: " + configGetterName);
            case "Threshold":
                pairGenFactory = getThresholdPairGenFactory(strategy, configBuilder);
                break;
            case "Exhaustive":
                pairGenFactory =  getExhaustivePairGenFactory(strategy, configBuilder);
                break;
            case "AtomDistanceSorted":
                pairGenFactory = getAtomDistanceSortedPairGenFactory(strategy, configBuilder);
                break;
            case "AtomDistance":
                pairGenFactory = getAtomDistancePairGenFactory(strategy, configBuilder);
                break;
        }
        return pairGenFactory;
    }

    private PairGenFactory<G, Scamp5AnalogueTransformation<G>, Register> getThresholdPairGenFactory(JSONObject json, Scamp5AnalogueConfig.Builder configBuilder) {
        if(!json.has("threshold")) {throw new IllegalArgumentException("you need to define " + "threshold" + " inside configGetter");}
        int threshold = json.getInt("threshold");
        printLn("Exhaustive Search Threshold  : " + threshold);
        Scamp5AnalogueConfig scampConfig = updateConfigBuilder(json, configBuilder).build();

        CostHeuristic<G, Scamp5AnalogueTransformation<G>, Register> heuristic = null;
        PairGenFactory<G, Scamp5AnalogueTransformation<G>, Register> above;
        PairGenFactory<G, Scamp5AnalogueTransformation<G>, Register> below;
        {
            if (json.has("above")) {
                above = buildPairGenFactory(json.getJSONObject("above"), scampConfig.builder());
            } else {
                heuristic = getCostHeuristic(json.getJSONObject("heuristic"));
                final CostHeuristic<G, Scamp5AnalogueTransformation<G>, Register> heuristicA = heuristic;
                above = (goals, context) -> new Scamp5AnaloguePairGens.AnalogueAtomDistanceSortedPairGen<>(goals, context, scampConfig, heuristicA);
            }
            if (json.has("below")) {
                below = buildPairGenFactory(json.getJSONObject("below"), scampConfig.builder());
            } else {
                if(heuristic == null){heuristic = getCostHeuristic(json.getJSONObject("heuristic"));}
                final CostHeuristic<G, Scamp5AnalogueTransformation<G>, Register> heuristicB = heuristic;
                below = (goals, context) -> new Scamp5AnaloguePairGens.ExhaustivePairGen<>(goals, context, scampConfig, heuristicB);
            }
        }

        return new ThresholdPairGen<>(threshold, above, below);
    }

    private PairGenFactory<G, Scamp5AnalogueTransformation<G>, Register> getExhaustivePairGenFactory(JSONObject json, Scamp5AnalogueConfig.Builder configBuilder) {
        Scamp5AnalogueConfig scampConfig = updateConfigBuilder(json, configBuilder).build();
        CostHeuristic<G, Scamp5AnalogueTransformation<G>, Register> heuristic = getCostHeuristic(json.getJSONObject("heuristic"));
        return (goals, context) -> new Scamp5AnaloguePairGens.ExhaustivePairGen<>(goals, context, scampConfig, heuristic);
    }
    private PairGenFactory<G, Scamp5AnalogueTransformation<G>, Register> getAtomDistanceSortedPairGenFactory(JSONObject json, Scamp5AnalogueConfig.Builder configBuilder) {
        Scamp5AnalogueConfig scampConfig = updateConfigBuilder(json, configBuilder).build();
        CostHeuristic<G, Scamp5AnalogueTransformation<G>, Register> heuristic = getCostHeuristic(json.getJSONObject("heuristic"));
        return (goals, context) -> new Scamp5AnaloguePairGens.AnalogueAtomDistanceSortedPairGen<>(goals, context, scampConfig, heuristic);
    }
    private PairGenFactory<G, Scamp5AnalogueTransformation<G>, Register> getAtomDistancePairGenFactory(JSONObject json, Scamp5AnalogueConfig.Builder configBuilder) {
        Scamp5AnalogueConfig scampConfig = updateConfigBuilder(json, configBuilder).build();
        return (goals, context) -> new Scamp5AnaloguePairGens.AnalogueAtomDistancePairGen<>(goals, context, scampConfig);
    }

    private Scamp5AnalogueConfig.Builder updateConfigBuilder(JSONObject json, Scamp5AnalogueConfig.Builder configBuilder) {
//        if(!json.has("ops")) {throw new IllegalArgumentException("you need to define " + "ops" + " inside configGetter");}
        if(json.has("ops")) {
            printLn("Instruction to use          : " + json.getString("ops"));
            switch (json.getString("ops")) {
                default:
                    throw new IllegalArgumentException("Unknown Instructions option " + json.getString("ops"));
                case "all":
                    return configBuilder.useAll().setSubPowerOf2(true);
                case "basic":
                    return configBuilder.useBasic().setSubPowerOf2(true);
            }
        } else return configBuilder.useAll().setSubPowerOf2(true);
    }

    private CostHeuristic<G, Scamp5AnalogueTransformation<G>, Register> getCostHeuristic(JSONObject json) {
        if(!json.has("name")) {throw new IllegalArgumentException("you need to define " + "name" + " inside configGetter");}
        printLn("CostHeuristic to use          : " + json.getString("name"));
        switch (json.getString("name")) {
            default:
                throw new IllegalArgumentException("Unknown Heuristic option " + json.getString("name"));
            case "Pattern":
                return new PatternHeuristic<>(initialGoals);
            case "Pattern2":
                return new PatternHeuristic2<>(initialGoals);
        }
    }

    @Override
    protected Verifier<G,Scamp5AnalogueTransformation<G>,Register> makeVerifier() {
        String verf = config.getString("verifier");
        switch (verf) {
            case "Scamp5Emulator":
                Verifier<G,Scamp5AnalogueTransformation<G>,Register> v = new Scamp5AnalogueVerifier<>();
                v.verbose(FileRun.verbose);
                return v;
            case "None":
                return Verifier.SkipVerify();
            default:
                throw new IllegalArgumentException("Verifier Unknown");
        }
    }

    @Override
    protected String generateCode(Plan<G, Scamp5AnalogueTransformation<G>, Register> p){
        RegisterAllocator.Mapping<G,Register> mapping = registerAllocator.solve(p);
        StringBuilder sb = new StringBuilder();
        sb.append(scamp5AnalogueConfig.outputFormatter.comment("Kernel Code!"));
        sb.append(scamp5AnalogueConfig.outputFormatter.newLine());
        sb.append(scamp5AnalogueConfig.outputFormatter.comment("Inputs in: " + mapping.initRegisters().toString()));
        sb.append(scamp5AnalogueConfig.outputFormatter.newLine());
        sb.append(scamp5AnalogueConfig.outputFormatter.kernel_begin());
        sb.append(scamp5AnalogueConfig.outputFormatter.newLine());
        sb.append(p.produceCode(mapping));
        sb.append(scamp5AnalogueConfig.outputFormatter.kernel_end());
        sb.append(scamp5AnalogueConfig.outputFormatter.newLine());
        return sb.toString();
    }


    public static class AtomGoalFileRun extends Scamp5AnalogueFileRun<AtomGoal> {
        @FileRunImplementation(key="scamp5", fields = {"mode", "goalSystem"}, values = {"analogue", "atom"})
        public AtomGoalFileRun(JSONObject config) {
            super(config);
        }

        @Override
        protected Kernel3DGoal.Kernel3DGoalFactory<AtomGoal> getGoalFactory(Register reg) {
            return new AtomGoal.Factory();
        }
    }

    public static class ArrayGoalFileRun extends Scamp5AnalogueFileRun<ArrayGoal> {
        @FileRunImplementation(key="scamp5", fields = {"mode", "goalSystem"}, values = {"analogue", "array"})
        public ArrayGoalFileRun(JSONObject config) {
            super(config);
        }

        @Override
        protected Kernel3DGoal.Kernel3DGoalFactory<ArrayGoal> getGoalFactory(Register reg) {
            return new ArrayGoal.Factory();
        }
    }
}
