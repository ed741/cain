package uk.co.edstow.cain.scamp5.analogue;

import org.json.JSONObject;
import uk.co.edstow.cain.FileRun;
import uk.co.edstow.cain.Verifier;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.arrayGoal.ArrayGoal;
import uk.co.edstow.cain.goals.atomGoal.AtomGoal;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.scamp5.BasicScamp5ConfigGetter;
import uk.co.edstow.cain.scamp5.PatternHeuristic;
import uk.co.edstow.cain.scamp5.ThresholdScamp5ConfigGetter;
import uk.co.edstow.cain.scamp5.emulator.Scamp5AnalogueVerifier;

public abstract class Scamp5AnalogueFileRun<G extends Kernel3DGoal<G>> extends FileRun.Kernel3DStdTransFileRun<G, Scamp5AnalogueTransformation<G>> {


    public Scamp5AnalogueFileRun(JSONObject config) {
        super(config);
    }

    @Override
    protected PairGenFactory<G, Scamp5AnalogueTransformation<G>, Register> makePairGenFactory() {
        JSONObject json = config.getJSONObject("pairGen");
        printLn("\t Making Pair Generation Factory:");
        printLn("Name                        : " + json.getString("name"));

        Scamp5AnalogueConfig.Builder<G> configBuilder = new Scamp5AnalogueConfig.Builder<>();
        String factory = "configGetter";
        if(!json.has(factory)) {throw new IllegalArgumentException("you need to define " + factory + " inside pairGen");}
        String configGetterName = json.getJSONObject(factory).getString("name");
        printLn(factory+"               : " + configGetterName);
        switch (configGetterName) {
            default:
                throw new IllegalArgumentException("Unknown Scamp5 " + factory + " : " + json.getString("configGetter"));
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

    private Scamp5AnaloguePairGenFactory<G> getThresholdPairGenFactory(JSONObject json, Scamp5AnalogueConfig.Builder<G> configBuilder) {
        if(!json.has("threshold")) {throw new IllegalArgumentException("you need to define " + "threshold" + " inside configGetter");}
        int threshold = json.getInt("threshold");
        printLn("Exhaustive Search Threshold  : " + threshold);
        Scamp5AnalogueConfig<G> scampConfig = getScamp5Config(json, configBuilder);
        CostHeuristic<G, Scamp5AnalogueTransformation<G>> heuristic = getCostHeuristic(json, "heuristic");

        return new Scamp5AnaloguePairGenFactory<>(
                new ThresholdScamp5ConfigGetter<>(
                        initialGoals, threshold,
                        heuristic, scampConfig,
                        (goals, conf, scamp5Config, h) -> new Scamp5AnaloguePairGenFactory.AtomDistanceSortedPairGen<>(goals, conf, scampConfig, heuristic),
                        (goals, conf, scamp5Config, h) -> new Scamp5AnaloguePairGenFactory.ExhaustivePairGen<>(goals, conf, scampConfig, heuristic)
                )
        );
    }

    private Scamp5AnaloguePairGenFactory<G> getExhaustivePairGenFactory(JSONObject json, Scamp5AnalogueConfig.Builder<G> configBuilder) {
        Scamp5AnalogueConfig<G> scampConfig = getScamp5Config(json, configBuilder);
        CostHeuristic<G, Scamp5AnalogueTransformation<G>> heuristic = getCostHeuristic(json, "heuristic");
        return new Scamp5AnaloguePairGenFactory<>(new BasicScamp5ConfigGetter<>(scampConfig,
                (goals, conf, scamp5Config) -> new Scamp5AnaloguePairGenFactory.ExhaustivePairGen<>(goals, conf, scamp5Config, heuristic)
        ));
    }
    private Scamp5AnaloguePairGenFactory<G> getAtomDistanceSortedPairGenFactory(JSONObject json, Scamp5AnalogueConfig.Builder<G> configBuilder) {
        Scamp5AnalogueConfig<G> scampConfig = getScamp5Config(json, configBuilder);
        CostHeuristic<G, Scamp5AnalogueTransformation<G>> heuristic = getCostHeuristic(json, "heuristic");
        return new Scamp5AnaloguePairGenFactory<>(new BasicScamp5ConfigGetter<>(scampConfig,
                (goals, conf, scamp5Config) -> new Scamp5AnaloguePairGenFactory.AtomDistanceSortedPairGen<>(goals, conf, scamp5Config, heuristic)
        ));
    }
    private Scamp5AnaloguePairGenFactory<G> getAtomDistancePairGenFactory(JSONObject json, Scamp5AnalogueConfig.Builder<G> configBuilder) {
        Scamp5AnalogueConfig<G> scampConfig = getScamp5Config(json, configBuilder);
        return new Scamp5AnaloguePairGenFactory<>(new BasicScamp5ConfigGetter<>(scampConfig,
                (goals, conf, scamp5Config) -> new Scamp5AnaloguePairGenFactory.AtomDistancePairGen<>(goals, conf, scamp5Config)
        ));
    }

    private Scamp5AnalogueConfig<G> getScamp5Config(JSONObject json, Scamp5AnalogueConfig.Builder<G> configBuilder) {
        if(!json.has("ops")) {throw new IllegalArgumentException("you need to define " + "ops" + " inside configGetter");}
        printLn("Instruction to use          : " + json.getString("ops"));
        switch (json.getString("ops")) {
            default:
                throw new IllegalArgumentException("Unknown Instructions option " + json.getString("ops"));
            case "all":
                return configBuilder.useAll().setSubPowerOf2(true).build();
            case "basic":
                return configBuilder.useBasic().setSubPowerOf2(true).build();
        }
    }

    private CostHeuristic<G, Scamp5AnalogueTransformation<G>> getCostHeuristic(JSONObject json, String name) {
        if(!json.has(name)) {throw new IllegalArgumentException("you need to define " + name + " inside configGetter");}
        printLn("CostHeuristic to use          : " + json.getString(name));
        switch (json.getString(name)) {
            default:
                throw new IllegalArgumentException("Unknown Heuristic option " + json.getString(name));
            case "Pattern":
                return new PatternHeuristic<>(initialGoals);
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


    public static class AtomGoalFileRun extends Scamp5AnalogueFileRun<AtomGoal> {
        public AtomGoalFileRun(JSONObject config) {
            super(config);
        }

        @Override
        protected Kernel3DGoal.Kernel3DGoalFactory<AtomGoal> getGoalFactory() {
            return new AtomGoal.Factory();
        }
    }

    public static class ArrayGoalFileRun extends Scamp5AnalogueFileRun<ArrayGoal> {
        public ArrayGoalFileRun(JSONObject config) {
            super(config);
        }

        @Override
        protected Kernel3DGoal.Kernel3DGoalFactory<ArrayGoal> getGoalFactory() {
            return new ArrayGoal.Factory();
        }
    }
}
