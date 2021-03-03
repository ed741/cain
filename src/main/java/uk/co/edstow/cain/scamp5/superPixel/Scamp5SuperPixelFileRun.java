package uk.co.edstow.cain.scamp5.superPixel;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.co.edstow.cain.FileRun;
import uk.co.edstow.cain.fileRun.FileRunImplementation;
import uk.co.edstow.cain.regAlloc.*;
import uk.co.edstow.cain.goals.BankedKernel3DGoal;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.arrayGoal.BankedArrayGoal;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.scamp5.BasicScamp5ConfigGetter;
import uk.co.edstow.cain.scamp5.PatternHeuristic;
import uk.co.edstow.cain.scamp5.ThresholdScamp5ConfigGetter;
import uk.co.edstow.cain.transformations.BankedTransformation;


import java.util.*;

public abstract class Scamp5SuperPixelFileRun<G extends BankedKernel3DGoal<G>> extends FileRun.Kernel3DBankedFileRun<G, Scamp5SuperPixelTransformation<G>> {

    public Scamp5SuperPixelFileRun(JSONObject config) {
        super(config);
    }

    @Override
    protected PairGenFactory<G,Scamp5SuperPixelTransformation<G>, BRegister> makePairGenFactory() {
        JSONObject json = config.getJSONObject("pairGen");
        printLn("\t Making Pair Generation Factory:");
        printLn("Name                        : " + json.getString("name"));

        Scamp5SuperPixelConfig.Builder<G> configBuilder = new Scamp5SuperPixelConfig.Builder<>();

        if(!json.has("scratchRegs")) {throw new IllegalArgumentException("you need to define " + "scratchRegs" + " inside pairGen");}
        JSONArray jScratchRegs = json.getJSONArray("scratchRegs");
        List<String> scratchRegs = new ArrayList<String>(jScratchRegs.length());
        for (int i = 0; i < jScratchRegs.length(); i++) {
            scratchRegs.add(jScratchRegs.getString(i));
        }
        configBuilder.setScratchRegisters(scratchRegs);


        if(!json.has("selectReg")) {throw new IllegalArgumentException("you need to define " + "selectReg" + " inside pairGen");}
        String selectReg = json.getString("selectReg");
        configBuilder.setSelectReg(selectReg);

        if(!json.has("maskReg")) {throw new IllegalArgumentException("you need to define " + "maskReg" + " inside pairGen");}
        String maskReg = json.getString("maskReg");
        configBuilder.setMaskReg(maskReg);

        if(!json.has("maskedReg")) {throw new IllegalArgumentException("you need to define " + "maskedReg" + " inside pairGen");}
        String maskedReg = json.getString("maskedReg");
        configBuilder.setMaskedReg(maskedReg);

        if(!json.has("northReg")) {throw new IllegalArgumentException("you need to define " + "northReg" + " inside pairGen");}
        String northReg = json.getString("northReg");
        configBuilder.setNorthReg(northReg);

        if(!json.has("eastReg")) {throw new IllegalArgumentException("you need to define " + "eastReg" + " inside pairGen");}
        String eastReg = json.getString("eastReg");
        configBuilder.setEastReg(eastReg);

        if(!json.has("southReg")) {throw new IllegalArgumentException("you need to define " + "southReg" + " inside pairGen");}
        String southReg = json.getString("southReg");
        configBuilder.setSouthReg(southReg);

        if(!json.has("westReg")) {throw new IllegalArgumentException("you need to define " + "westReg" + " inside pairGen");}
        String westReg = json.getString("westReg");
        configBuilder.setWestReg(westReg);

        if(!json.has("width")) {throw new IllegalArgumentException("you need to define " + "width" + " inside pairGen");}
        int width = json.getInt("width");
        configBuilder.setWidth(width);

        if(!json.has("height")) {throw new IllegalArgumentException("you need to define " + "height" + " inside pairGen");}
        int height = json.getInt("height");
        configBuilder.setHeight(height);

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
        configBuilder.setBanks(banks);
        configBuilder.setBitOrder(bitOrder);

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

        configBuilder.setUseMovbx(true).setUseAdd(true).setUseAddSelf(true).setUseDiv(true).setUseRes(true);


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

    private Scamp5SuperPixelPairGenFactory<G> getThresholdPairGenFactory(JSONObject json, Scamp5SuperPixelConfig.Builder<G> configBuilder) {
        if(!json.has("threshold")) {throw new IllegalArgumentException("you need to define " + "threshold" + " inside configGetter");}
        int threshold = json.getInt("threshold");
        printLn("Exhaustive Search Threshold  : " + threshold);
        CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> heuristic = getCostHeuristic(json, "heuristic");
        Scamp5SuperPixelConfig<G> scampConfig = configBuilder.build();

        return new Scamp5SuperPixelPairGenFactory<>(
                new ThresholdScamp5ConfigGetter<>(
                        initialGoals, threshold,
                        heuristic, scampConfig,
                        (goals, conf, scamp5Config, h) -> new Scamp5SuperPixelPairGenFactory.SuperPixelAtomDistanceSortedPairGen<>(goals, conf, scampConfig, heuristic),
                        (goals, conf, scamp5Config, h) -> new Scamp5SuperPixelPairGenFactory.ExhaustivePairGen<>(goals, conf, scampConfig, heuristic)
                )
        );
    }

    private Scamp5SuperPixelPairGenFactory<G> getExhaustivePairGenFactory(JSONObject json, Scamp5SuperPixelConfig.Builder<G> configBuilder) {
        CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> heuristic = getCostHeuristic(json, "heuristic");
        return new Scamp5SuperPixelPairGenFactory<>(new BasicScamp5ConfigGetter<>(configBuilder.build(),
                (goals, conf, scamp5Config) -> new Scamp5SuperPixelPairGenFactory.ExhaustivePairGen<>(goals, conf, scamp5Config, heuristic)
        ));
    }
    private Scamp5SuperPixelPairGenFactory<G> getAtomDistanceSortedPairGenFactory(JSONObject json, Scamp5SuperPixelConfig.Builder<G> configBuilder) {
        CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> heuristic = getCostHeuristic(json, "heuristic");
        return new Scamp5SuperPixelPairGenFactory<>(new BasicScamp5ConfigGetter<>(configBuilder.build(),
                (goals, conf, scamp5Config) -> new Scamp5SuperPixelPairGenFactory.SuperPixelAtomDistanceSortedPairGen<>(goals, conf, scamp5Config, heuristic)
        ));
    }
    private Scamp5SuperPixelPairGenFactory<G> getAtomDistancePairGenFactory(JSONObject json, Scamp5SuperPixelConfig.Builder<G> configBuilder) {
        return new Scamp5SuperPixelPairGenFactory<>(new BasicScamp5ConfigGetter<>(configBuilder.build(),
                (goals, conf, scamp5Config) -> new Scamp5SuperPixelPairGenFactory.SuperPixelAtomDistancePairGen<>(goals, conf, scamp5Config)
        ));
    }

    private CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> getCostHeuristic(JSONObject json, String name) {
        if(!json.has(name)) {throw new IllegalArgumentException("you need to define " + name + " inside configGetter");}
        printLn("CostHeuristic to use          : " + json.getString(name));
        switch (json.getString(name)) {
            default:
                throw new IllegalArgumentException("Unknown Heuristic option " + json.getString(name));
            case "Pattern":
                return new PatternHeuristic<>(initialGoals);
        }
    }

    public static class ArrayGoalFileRun extends Scamp5SuperPixelFileRun<BankedArrayGoal> {

        @FileRunImplementation(key="Scamp5SuperPixel")
        public ArrayGoalFileRun(JSONObject config) {
            super(config);
        }

        @Override
        protected Kernel3DGoal.Kernel3DGoalFactory<BankedArrayGoal> getGoalFactory(BRegister reg) {
            return new BankedArrayGoal.Factory(reg.bank);
        }
    }
}
