package uk.co.edstow.cain.scamp5.digital;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.co.edstow.cain.FileRun;
import uk.co.edstow.cain.regAlloc.RegisterAllocator;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.arrayGoal.ArrayGoal;
import uk.co.edstow.cain.goals.atomGoal.AtomGoal;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.scamp5.BasicScamp5ConfigGetter;
import uk.co.edstow.cain.scamp5.PatternHeuristic;
import uk.co.edstow.cain.scamp5.ThresholdScamp5ConfigGetter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Scamp5DigitalFileRun<G extends Kernel3DGoal<G>> extends FileRun.Kernel3DStdTransFileRun<G, Scamp5DigitalTransformation<G>> {

    public Scamp5DigitalFileRun(JSONObject config) {
        super(config);
    }

//        @Override
//        Goal3DAtomLike.Goal3DAtomLikeFactory<G> getGoalFactory() {
//            return new AtomGoal.Factory();
//        }

//    @Override
//    protected PairGenFactory<G> makePairGenFactory(JSONObject json, RegisterAllocator<G> registerAllocator) {
//        printLn("\t Making Pair Generation Factory:");
//        printLn("Name                        : " + json.getString("name"));
//        printLn("Config Getter               : " + json.getString("configGetter"));
//        int bits = json.getInt("bits");
//        JSONArray jScratchRegs = json.getJSONArray("scratchRegs");
//        List<String> scratchRegs = new ArrayList<String>(jScratchRegs.length());
//        for (int i = 0; i < jScratchRegs.length(); i++) {
//            scratchRegs.add(jScratchRegs.getString(i));
//        }
//        JSONObject jRegMapping = json.getJSONObject("regMapping");
//        Map<RegisterAllocator.Register, List<String>> regMapping = new HashMap<>();
//        for (RegisterAllocator.Register reg : registerAllocator.getAvailableRegistersArray()) {
//            JSONArray jRegList = jRegMapping.getJSONArray(reg.name);
//            List<String> digitalRegs = new ArrayList<String>(jRegList.length());
//            if (bits > jRegList.length()) {
//                throw new IllegalArgumentException("Not Enough Digital Registers for Logical Register: " + reg.toString());
//            }
//            for (int i = 0; i < jRegList.length(); i++) {
//                digitalRegs.add(jRegList.getString(i));
//            }
//            regMapping.put(reg, digitalRegs);
//        }
//
//        switch (json.getString("configGetter")) {
//            default:
//                throw new IllegalArgumentException("Unknown Scamp5 Scamp5ConfigGetter " + json.getString("configGetter"));
//            case "Threshold":
//                Scamp5DigitalConfig<G> scampConfig = new Scamp5DigitalConfig<>(true, true, true, true, true, true, true, regMapping, scratchRegs, bits);
//                printLn("Exhustive Search Threshold  : " + json.getInt("threshold"));
//                return new Scamp5DigitalPairGenFactory<>(
//                        new ThresholdScamp5ConfigGetter<>(
//                                initialGoals, json.getInt("threshold"),
//                                new PatternHeuristic<>(initialGoals), scampConfig,
//                                (goals, conf, scamp5Config, heuristic) -> new Scamp5DigitalPairGenFactory.AtomDistanceSortedPairGen<>(goals, conf, scampConfig, heuristic),
//                                (goals, conf, scamp5Config, heuristic) -> new Scamp5DigitalPairGenFactory.Scamp5ExhaustivePairGen<>(goals, conf, scampConfig, heuristic)
//                        )
//                );
//        }
//
//    }



    @Override
    protected PairGenFactory<G, Scamp5DigitalTransformation<G>> makePairGenFactory() {
        JSONObject json = config.getJSONObject("pairGen");
        printLn("\t Making Pair Generation Factory:");
        printLn("Name                        : " + json.getString("name"));

        if(!json.has("bits")) {throw new IllegalArgumentException("you need to define " + "bits" + " inside pairGen");}
        int bits = json.getInt("bits");
        if(!json.has("scratchRegs")) {throw new IllegalArgumentException("you need to define " + "scratchRegs" + " inside pairGen");}
        JSONArray jScratchRegs = json.getJSONArray("scratchRegs");
        List<String> scratchRegs = new ArrayList<String>(jScratchRegs.length());
        for (int i = 0; i < jScratchRegs.length(); i++) {
            scratchRegs.add(jScratchRegs.getString(i));
        }

        if(!json.has("regMapping")) {throw new IllegalArgumentException("you need to define " + "regMapping" + " inside pairGen");}
        JSONObject jRegMapping = json.getJSONObject("regMapping");
        Map<RegisterAllocator.Register, List<String>> regMapping = new HashMap<>();
        for (RegisterAllocator.Register reg : registerAllocator.getAvailableRegistersArray()) {
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
        Scamp5DigitalConfig.Builder<G> configBuilder = new Scamp5DigitalConfig.Builder<>(new Scamp5DigitalConfig<G>(true, true, true, true, true, true, true, regMapping, scratchRegs, bits));


        if(!json.has("configGetter")) {throw new IllegalArgumentException("you need to define " + "configGetter" + " inside pairGen");}
        String configGetterName = json.getJSONObject("configGetter").getString("name");
        printLn("Config Getter               : " + configGetterName);
        switch (configGetterName) {
            default:
                throw new IllegalArgumentException("Unknown Scamp5 Scamp5ConfigGetter " + json.getString("configGetter"));
            case "Threshold":
                return getThresholdPairGenFactory(json.getJSONObject("configGetter"), configBuilder);
            case "Exhaustive":
                return getExhaustivePairGenFactory(json.getJSONObject("configGetter"), configBuilder);
            case "AtomDistanceSorted":
                return getAtomDistanceSortedPairGenFactory(json.getJSONObject("configGetter"), configBuilder);
            case "AtomDistance":
                return getAtomDistancePairGenFactory(json.getJSONObject("configGetter"), configBuilder);
        }

    }

    private Scamp5DigitalPairGenFactory<G> getThresholdPairGenFactory(JSONObject json, Scamp5DigitalConfig.Builder<G> configBuilder) {
        if(!json.has("threshold")) {throw new IllegalArgumentException("you need to define " + "threshold" + " inside configGetter");}
        int threshold = json.getInt("threshold");
        printLn("Exhaustive Search Threshold  : " + threshold);
        CostHeuristic<G, Scamp5DigitalTransformation<G>> heuristic = getCostHeuristic(json, "heuristic");
        Scamp5DigitalConfig<G> scampConfig = configBuilder.build();
        return new Scamp5DigitalPairGenFactory<>(
                new ThresholdScamp5ConfigGetter<>(
                        initialGoals, threshold,
                        heuristic, scampConfig,
                        (goals, conf, scamp5Config, h) -> new Scamp5DigitalPairGenFactory.AtomDistanceSortedPairGen<>(goals, conf, scampConfig, heuristic),
                        (goals, conf, scamp5Config, h) -> new Scamp5DigitalPairGenFactory.ExhaustivePairGen<>(goals, conf, scampConfig, heuristic)
                )
        );
    }

    private Scamp5DigitalPairGenFactory<G> getExhaustivePairGenFactory(JSONObject json, Scamp5DigitalConfig.Builder<G> configBuilder) {
        CostHeuristic<G, Scamp5DigitalTransformation<G>> heuristic = getCostHeuristic(json, "heuristic");
        Scamp5DigitalConfig<G> scampConfig = configBuilder.build();
        return new Scamp5DigitalPairGenFactory<>(new BasicScamp5ConfigGetter<>(scampConfig,
                (goals, conf, scamp5Config) -> new Scamp5DigitalPairGenFactory.ExhaustivePairGen<>(goals, conf, scamp5Config, heuristic)
        ));
    }
    private Scamp5DigitalPairGenFactory<G> getAtomDistanceSortedPairGenFactory(JSONObject json, Scamp5DigitalConfig.Builder<G> configBuilder) {
        CostHeuristic<G, Scamp5DigitalTransformation<G>> heuristic = getCostHeuristic(json, "heuristic");
        Scamp5DigitalConfig<G> scampConfig = configBuilder.build();
        return new Scamp5DigitalPairGenFactory<>(new BasicScamp5ConfigGetter<>(scampConfig,
                (goals, conf, scamp5Config) -> new Scamp5DigitalPairGenFactory.AtomDistanceSortedPairGen<>(goals, conf, scamp5Config, heuristic)
        ));
    }
    private Scamp5DigitalPairGenFactory<G> getAtomDistancePairGenFactory(JSONObject json, Scamp5DigitalConfig.Builder<G> configBuilder) {
        Scamp5DigitalConfig<G> scampConfig = configBuilder.build();
        return new Scamp5DigitalPairGenFactory<>(new BasicScamp5ConfigGetter<>(scampConfig,
                (goals, conf, scamp5Config) -> new Scamp5DigitalPairGenFactory.AtomDistancePairGen<>(goals, conf, scamp5Config)
        ));
    }

    private CostHeuristic<G, Scamp5DigitalTransformation<G>> getCostHeuristic(JSONObject json, String name) {
        if(!json.has(name)) {throw new IllegalArgumentException("you need to define " + name + " inside configGetter");}
        printLn("CostHeuristic to use          : " + json.getString(name));
        switch (json.getString(name)) {
            default:
                throw new IllegalArgumentException("Unknown Heuristic option " + json.getString(name));
            case "Pattern":
                return new PatternHeuristic<>(initialGoals);
        }
    }




    public static class AtomGoalFileRun extends Scamp5DigitalFileRun<AtomGoal> {
        public AtomGoalFileRun(JSONObject config) {
            super(config);
        }

        @Override
        protected Kernel3DGoal.Kernel3DGoalFactory<AtomGoal> getGoalFactory() {
            return new AtomGoal.Factory();
        }
    }

    public static class ArrayGoalFileRun extends Scamp5DigitalFileRun<ArrayGoal> {
        public ArrayGoalFileRun(JSONObject config) {
            super(config);
        }

        @Override
        protected Kernel3DGoal.Kernel3DGoalFactory<ArrayGoal> getGoalFactory() {
            return new ArrayGoal.Factory();
        }
    }
}
