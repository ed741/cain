package uk.co.edstow.cain.scamp5.digital;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.co.edstow.cain.fileRun.FileRunImplementation;
import uk.co.edstow.cain.fileRun.Kernel3DStdTransFileRun;
import uk.co.edstow.cain.pairgen.Generator;
import uk.co.edstow.cain.pairgen.ThresholdPairGen;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.arrayGoal.ArrayGoal;
import uk.co.edstow.cain.goals.atomGoal.AtomGoal;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.scamp5.*;
import uk.co.edstow.cain.scamp5.analogue.Scamp5AnalogueTransformation;
import uk.co.edstow.cain.scamp5.output.Scamp5DefaultOutputFormatter;
import uk.co.edstow.cain.scamp5.output.Scamp5JssOutputFormatter;
import uk.co.edstow.cain.scamp5.output.Scamp5OutputFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Scamp5DigitalFileRun<G extends Kernel3DGoal<G>> extends Kernel3DStdTransFileRun<G, Scamp5DigitalTransformation<G>> {

    public Scamp5DigitalFileRun(JSONObject config) {
        super(config);
    }

    @Override
    protected boolean isThreeDimensional() {
        return false;
    }

    @Override
    protected Generator<G, Scamp5DigitalTransformation<G>, Register> makeGenerator() {
        JSONObject json = config.getJSONObject("pairGen");
        printLn("\t Making Pair Generation Factory:");
        Scamp5DigitalConfig.Builder configBuilder = new Scamp5DigitalConfig.Builder();
        if(!json.has("bits")) {throw new IllegalArgumentException("you need to define " + "bits" + " inside pairGen");}
        int bits = json.getInt("bits");
        configBuilder.bits(bits);
        if(!json.has("scratchRegs")) {throw new IllegalArgumentException("you need to define " + "scratchRegs" + " inside pairGen");}
        JSONArray jScratchRegs = json.getJSONArray("scratchRegs");
        List<String> scratchRegs = new ArrayList<String>(jScratchRegs.length());
        for (int i = 0; i < jScratchRegs.length(); i++) {
            scratchRegs.add(jScratchRegs.getString(i));
        }
        configBuilder.scratchRegisters(scratchRegs);

        if(!json.has("regMapping")) {throw new IllegalArgumentException("you need to define " + "regMapping" + " inside pairGen");}
        JSONObject jRegMapping = json.getJSONObject("regMapping");
        Map<Register, List<String>> regMapping = new HashMap<>();
        for (Register reg : registerAllocator.getAvailableRegistersArray()) {
            JSONArray jRegList = jRegMapping.getJSONArray(reg.name);
            List<String> digitalRegs = new ArrayList<>(jRegList.length());
            if (bits > jRegList.length()) {
                throw new IllegalArgumentException("Not Enough Digital Registers for Logical Register: " + reg.toString());
            }
            for (int i = 0; i < jRegList.length(); i++) {
                digitalRegs.add(jRegList.getString(i));
            }
            regMapping.put(reg, digitalRegs);
        }
        configBuilder.registerMapping(regMapping);

        Scamp5OutputFormatter outputFormatter;
        if (!json.has("outputFormat")) {
            throw new IllegalArgumentException("you need to define outputFormat inside pairGen");
        }
        JSONObject outputFormatConfig = json.getJSONObject("outputFormat");
        switch (outputFormatConfig.getString("name")) {
            default:
                throw new IllegalArgumentException("Unknown Scamp5 outputFormat : " + outputFormatConfig.getString("name"));
            case "defaultFormat":
                outputFormatter = new Scamp5DefaultOutputFormatter();
                break;
            case "jssFormat":
                String jssSimulatorName = outputFormatConfig.getString("simulatorName");
                outputFormatter = new Scamp5JssOutputFormatter(jssSimulatorName);
                break;
        }
        configBuilder.setOutputFormatter(outputFormatter);
        configBuilder = updateConfigBuilder(json, configBuilder);

        String factory = "strategy";
        if(!json.has(factory)) {throw new IllegalArgumentException("you need to define " + factory + " inside pairGen");}
        JSONObject strategy = json.getJSONObject(factory);
        PairGenFactory<G, Scamp5DigitalTransformation<G>, Register> pairGenFactory = buildPairGenFactory(strategy, configBuilder);

        return new Generator<>(new Scamp5DigitalDirectSolver<>(configBuilder.build()), pairGenFactory);

    }

    private PairGenFactory<G, Scamp5DigitalTransformation<G>, Register> buildPairGenFactory(JSONObject strategy, Scamp5DigitalConfig.Builder configBuilder) {
        PairGenFactory<G, Scamp5DigitalTransformation<G>, Register> pairGenFactory;
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

    private PairGenFactory<G, Scamp5DigitalTransformation<G>, Register> getThresholdPairGenFactory(JSONObject json, Scamp5DigitalConfig.Builder configBuilder) {
        if(!json.has("threshold")) {throw new IllegalArgumentException("you need to define " + "threshold" + " inside configGetter");}
        int threshold = json.getInt("threshold");
        printLn("Exhaustive Search Threshold  : " + threshold);
        Scamp5DigitalConfig scampConfig = updateConfigBuilder(json, configBuilder).build();
        CostHeuristic<G, Scamp5DigitalTransformation<G>, Register> heuristic = null;
        PairGenFactory<G, Scamp5DigitalTransformation<G>, Register> above;
        PairGenFactory<G, Scamp5DigitalTransformation<G>, Register> below;
        {
            if (json.has("above")) {
                above = buildPairGenFactory(json.getJSONObject("above"), scampConfig.builder());
            } else {
                heuristic = getCostHeuristic(json.getJSONObject("heuristic"));
                final CostHeuristic<G, Scamp5DigitalTransformation<G>, Register> heuristicA = heuristic;
                above = (goals, context) -> new Scamp5DigitalPairGens.DigitalAtomDistanceSortedPairGen<>(goals, context, scampConfig, heuristicA);
            }
            if (json.has("below")) {
                below = buildPairGenFactory(json.getJSONObject("below"), scampConfig.builder());
            } else {
                if (heuristic == null) {heuristic = getCostHeuristic(json.getJSONObject("heuristic"));}
                final CostHeuristic<G, Scamp5DigitalTransformation<G>, Register> heuristicB = heuristic;
                below = (goals, context) -> new Scamp5DigitalPairGens.ExhaustivePairGen<>(goals, context, scampConfig, heuristicB);
            }
        }

        return new ThresholdPairGen<>(threshold, above, below);
    }

    private PairGenFactory<G, Scamp5DigitalTransformation<G>, Register> getExhaustivePairGenFactory(JSONObject json, Scamp5DigitalConfig.Builder configBuilder) {
        CostHeuristic<G, Scamp5DigitalTransformation<G>, Register> heuristic = getCostHeuristic(json.getJSONObject("heuristic"));
        Scamp5DigitalConfig scampConfig = configBuilder.build();
        return (goals, context) -> new Scamp5DigitalPairGens.ExhaustivePairGen<>(goals, context, scampConfig, heuristic);
    }
    private PairGenFactory<G, Scamp5DigitalTransformation<G>, Register> getAtomDistanceSortedPairGenFactory(JSONObject json, Scamp5DigitalConfig.Builder configBuilder) {
        CostHeuristic<G, Scamp5DigitalTransformation<G>, Register> heuristic = getCostHeuristic(json.getJSONObject("heuristic"));
        Scamp5DigitalConfig scampConfig = configBuilder.build();
        return (goals, context) -> new Scamp5DigitalPairGens.DigitalAtomDistanceSortedPairGen<>(goals, context, scampConfig, heuristic);
    }
    private PairGenFactory<G, Scamp5DigitalTransformation<G>, Register> getAtomDistancePairGenFactory(JSONObject json, Scamp5DigitalConfig.Builder configBuilder) {
        Scamp5DigitalConfig scampConfig = configBuilder.build();
        return (goals, context) -> new Scamp5DigitalPairGens.DigitalAtomDistancePairGen<>(goals, context, scampConfig);
    }

    private Scamp5DigitalConfig.Builder updateConfigBuilder(JSONObject json, Scamp5DigitalConfig.Builder configBuilder) {
        // Currently no options available - all instructions kept on always
        return configBuilder.useMov(true).useMovx(true).useAdd(true).useAddSelf(true).useDiv(true).useRes(true).useRes2(true);
    }

    private CostHeuristic<G, Scamp5DigitalTransformation<G>, Register> getCostHeuristic(JSONObject json) {
        if(!json.has("name")) {throw new IllegalArgumentException("you need to define " + "name" + " inside configGetter");}
        printLn("CostHeuristic to use          : " + json.getString("name"));
        switch (json.getString("name")) {
            default:
                throw new IllegalArgumentException("Unknown Heuristic option " + json.getString("name"));
            case "Pattern":
                return new PatternHeuristic<>(initialGoals);
        }
    }




    public static class AtomGoalFileRun extends Scamp5DigitalFileRun<AtomGoal> {
        @FileRunImplementation(key="scamp5", fields = {"mode", "goalSystem"}, values = {"digital", "atom"})
        public AtomGoalFileRun(JSONObject config) {
            super(config);
        }

        @Override
        protected Kernel3DGoal.Kernel3DGoalFactory<AtomGoal> getGoalFactory(Register reg) {
            return new AtomGoal.Factory();
        }
    }

    public static class ArrayGoalFileRun extends Scamp5DigitalFileRun<ArrayGoal> {
        @FileRunImplementation(key="scamp5", fields = {"mode", "goalSystem"}, values = {"digital", "array"})
        public ArrayGoalFileRun(JSONObject config) {
            super(config);
        }

        @Override
        protected Kernel3DGoal.Kernel3DGoalFactory<ArrayGoal> getGoalFactory(Register reg) {
            return new ArrayGoal.Factory();
        }
    }
}
