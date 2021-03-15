package uk.co.edstow.cain.scamp5.superPixel;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.co.edstow.cain.fileRun.FileRunImplementation;
import uk.co.edstow.cain.fileRun.Kernel3DBankedFileRun;
import uk.co.edstow.cain.pairgen.Generator;
import uk.co.edstow.cain.pairgen.ThresholdPairGen;
import uk.co.edstow.cain.regAlloc.*;
import uk.co.edstow.cain.goals.BankedKernel3DGoal;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.arrayGoal.BankedArrayGoal;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.scamp5.*;
import uk.co.edstow.cain.scamp5.output.Scamp5DefaultOutputFormatter;
import uk.co.edstow.cain.scamp5.output.Scamp5JssOutputFormatter;
import uk.co.edstow.cain.scamp5.output.Scamp5OutputFormatter;


import java.util.*;

public abstract class Scamp5SuperPixelFileRun<G extends BankedKernel3DGoal<G>> extends Kernel3DBankedFileRun<G, Scamp5SuperPixelTransformation<G>> {

    public Scamp5SuperPixelFileRun(JSONObject config) {
        super(config);
    }

    @Override
    protected boolean isThreeDimensional() {
        return false;
    }

    @Override
    protected Generator<G,Scamp5SuperPixelTransformation<G>, BRegister> makeGenerator() {
        JSONObject json = config.getJSONObject("pairGen");
        printLn("\t Making Pair Generation Factory:");

        Scamp5SuperPixelConfig.Builder configBuilder = new Scamp5SuperPixelConfig.Builder();

        if(!json.has("scratchRegs")) {throw new IllegalArgumentException("you need to define " + "scratchRegs" + " inside pairGen");}
        JSONArray jScratchRegs = json.getJSONArray("scratchRegs");
        List<String> scratchRegs = new ArrayList<String>(jScratchRegs.length());
        for (int i = 0; i < jScratchRegs.length(); i++) {
            scratchRegs.add(jScratchRegs.getString(i));
        }
        configBuilder.scratchRegisters(scratchRegs);


        if(!json.has("selectReg")) {throw new IllegalArgumentException("you need to define " + "selectReg" + " inside pairGen");}
        String selectReg = json.getString("selectReg");
        configBuilder.selectReg(selectReg);

        if(!json.has("maskReg")) {throw new IllegalArgumentException("you need to define " + "maskReg" + " inside pairGen");}
        String maskReg = json.getString("maskReg");
        configBuilder.maskReg(maskReg);

        if(!json.has("maskedReg")) {throw new IllegalArgumentException("you need to define " + "maskedReg" + " inside pairGen");}
        String maskedReg = json.getString("maskedReg");
        configBuilder.maskedReg(maskedReg);

        if(!json.has("northReg")) {throw new IllegalArgumentException("you need to define " + "northReg" + " inside pairGen");}
        String northReg = json.getString("northReg");
        configBuilder.northReg(northReg);

        if(!json.has("eastReg")) {throw new IllegalArgumentException("you need to define " + "eastReg" + " inside pairGen");}
        String eastReg = json.getString("eastReg");
        configBuilder.eastReg(eastReg);

        if(!json.has("southReg")) {throw new IllegalArgumentException("you need to define " + "southReg" + " inside pairGen");}
        String southReg = json.getString("southReg");
        configBuilder.southReg(southReg);

        if(!json.has("westReg")) {throw new IllegalArgumentException("you need to define " + "westReg" + " inside pairGen");}
        String westReg = json.getString("westReg");
        configBuilder.westReg(westReg);

        if(!json.has("width")) {throw new IllegalArgumentException("you need to define " + "width" + " inside pairGen");}
        int width = json.getInt("width");
        configBuilder.width(width);

        if(!json.has("height")) {throw new IllegalArgumentException("you need to define " + "height" + " inside pairGen");}
        int height = json.getInt("height");
        configBuilder.height(height);

        if(!json.has("bitOrder")) {throw new IllegalArgumentException("you need to define " + "bitOrder" + " inside pairGen");}
        JSONArray jbitOrder = json.getJSONArray("bitOrder");
        int banks = jbitOrder.length();
        int[][][] bitOrder = new int[banks][width][height];
        for (int i = 0; i < bitOrder.length; i++) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitOrder[i][x][y] = jbitOrder.getJSONArray(i).getJSONArray(height-1-y).getInt(x);
                }
            }
        }
        configBuilder.banks(banks);
        configBuilder.bitOrder(bitOrder);

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

        updateConfigBuilder(json, configBuilder);

        String factory = "strategy";
        if(!json.has(factory)) {throw new IllegalArgumentException("you need to define " + factory + " inside pairGen");}
        JSONObject strategy = json.getJSONObject(factory);
        PairGenFactory<G, Scamp5SuperPixelTransformation<G>, BRegister> pairGenFactory = buildPairGenFactory(strategy, configBuilder);

        return new Generator<>(new Scamp5SuperPixelDirectSolver<>(configBuilder.build()), pairGenFactory);

    }

    private PairGenFactory<G, Scamp5SuperPixelTransformation<G>, BRegister> buildPairGenFactory(JSONObject strategy, Scamp5SuperPixelConfig.Builder configBuilder) {
        PairGenFactory<G, Scamp5SuperPixelTransformation<G>, BRegister> pairGenFactory;
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

    private PairGenFactory<G,Scamp5SuperPixelTransformation<G>, BRegister> getThresholdPairGenFactory(JSONObject json, Scamp5SuperPixelConfig.Builder configBuilder) {
        if(!json.has("threshold")) {throw new IllegalArgumentException("you need to define " + "threshold" + " inside configGetter");}
        int threshold = json.getInt("threshold");
        printLn("Exhaustive Search Threshold  : " + threshold);
        Scamp5SuperPixelConfig scampConfig = updateConfigBuilder(json, configBuilder).build();
        CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> heuristic = null;
        PairGenFactory<G, Scamp5SuperPixelTransformation<G>, BRegister> above;
        PairGenFactory<G, Scamp5SuperPixelTransformation<G>, BRegister> below;
        {
            if (json.has("above")) {
                above = buildPairGenFactory(json.getJSONObject("above"), scampConfig.builder());
            } else {
                heuristic = getCostHeuristic(json.getJSONObject("heuristic"));
                final CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> heuristicA = heuristic;
                above = (goals, context) -> new Scamp5SuperPixelPairGens.SuperPixelAtomDistanceSortedPairGen<>(goals, context, scampConfig, heuristicA);
            }
            if (json.has("below")) {
                below = buildPairGenFactory(json.getJSONObject("below"), scampConfig.builder());
            } else {
                if (heuristic == null) {heuristic = getCostHeuristic(json.getJSONObject("heuristic"));}
                final CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> heuristicB = heuristic;
                below = (goals, context) -> new Scamp5SuperPixelPairGens.ExhaustivePairGen<>(goals, context, scampConfig, heuristicB);
            }
        }

        return new ThresholdPairGen<>(threshold, above, below);
    }

    private PairGenFactory<G,Scamp5SuperPixelTransformation<G>, BRegister> getExhaustivePairGenFactory(JSONObject json, Scamp5SuperPixelConfig.Builder configBuilder) {
        Scamp5SuperPixelConfig scampConfig = updateConfigBuilder(json, configBuilder).build();
        CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> heuristic = getCostHeuristic(json.getJSONObject("heuristic"));
        return (goals, context) -> new Scamp5SuperPixelPairGens.ExhaustivePairGen<>(goals, context, scampConfig, heuristic);
    }
    private PairGenFactory<G,Scamp5SuperPixelTransformation<G>, BRegister> getAtomDistanceSortedPairGenFactory(JSONObject json, Scamp5SuperPixelConfig.Builder configBuilder) {
        Scamp5SuperPixelConfig scampConfig = updateConfigBuilder(json, configBuilder).build();
        CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> heuristic = getCostHeuristic(json.getJSONObject("heuristic"));
        return (goals, context) -> new Scamp5SuperPixelPairGens.SuperPixelAtomDistanceSortedPairGen<>(goals, context, scampConfig, heuristic);
    }
    private PairGenFactory<G,Scamp5SuperPixelTransformation<G>, BRegister> getAtomDistancePairGenFactory(JSONObject json, Scamp5SuperPixelConfig.Builder configBuilder) {
        Scamp5SuperPixelConfig scampConfig = updateConfigBuilder(json, configBuilder).build();
        return (goals, context) -> new Scamp5SuperPixelPairGens.SuperPixelAtomDistancePairGen<>(goals, context, scampConfig);
    }

    private Scamp5SuperPixelConfig.Builder updateConfigBuilder(JSONObject json, Scamp5SuperPixelConfig.Builder configBuilder) {
        // Currently no options available - all instructions kept on always
        return configBuilder.useMovbx(true).useAdd(true).useAddSelf(true).useSub(true).useDiv(true).useRes(true);
    }


    private CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> getCostHeuristic(JSONObject json) {
        if(!json.has("name")) {throw new IllegalArgumentException("you need to define " + "name" + " inside configGetter");}
        printLn("CostHeuristic to use          : " + json.getString("name"));
        switch (json.getString("name")) {
            default:
                throw new IllegalArgumentException("Unknown Heuristic option " + json.getString("name"));
            case "Pattern":
                return new PatternHeuristic<>(initialGoals);
        }
    }


    public static class ArrayGoalFileRun extends Scamp5SuperPixelFileRun<BankedArrayGoal> {

        @FileRunImplementation(key="scamp5", fields = {"mode"}, values = {"superpixel"})
        public ArrayGoalFileRun(JSONObject config) {
            super(config);
        }

        @Override
        protected Kernel3DGoal.Kernel3DGoalFactory<BankedArrayGoal> getGoalFactory(BRegister reg) {
            return new BankedArrayGoal.Factory(reg.bank);
        }
    }
}
