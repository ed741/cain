package uk.co.edstow.cain.scamp5.superPixel;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.co.edstow.cain.FileRun;
import uk.co.edstow.cain.regAlloc.BankedLinearScanRegisterAllocator;
import uk.co.edstow.cain.regAlloc.BankedRegisterAllocator;
import uk.co.edstow.cain.goals.BankedKernel3DGoal;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.arrayGoal.BankedArrayGoal;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.regAlloc.RegisterAllocator;
import uk.co.edstow.cain.scamp5.BasicScamp5ConfigGetter;
import uk.co.edstow.cain.scamp5.ThresholdScamp5ConfigGetter;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Scamp5SuperPixelFileRun<G extends BankedKernel3DGoal<G>> extends FileRun.Kernel3DFileRun<G, Scamp5SuperPixelTransformation<G>> {

    public Scamp5SuperPixelFileRun(JSONObject config) {
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
    protected List<? extends BankedRegisterAllocator.BRegister> getRegisterArray(JSONArray availableRegisters) {
        ArrayList<BankedRegisterAllocator.BRegister> out = new ArrayList<>(availableRegisters.length());

        for (int i = 0; i < availableRegisters.length(); i++) {
            JSONArray bank = availableRegisters.getJSONArray(i);
            for (int j = 0; j < bank.length(); j++) {
                out.add(new BankedRegisterAllocator.BRegister(i, bank.getString(j)));
            }
        }
        return out;
    }

    @Override
    protected List<? extends BankedRegisterAllocator.BRegister> getOutputRegisters() {
        if (config.has("filter")) {
            JSONObject filter = config.getJSONObject("filter");
            return filter.keySet().stream().map((String bank) -> {
                String[] strs = bank.split(":");
                if(strs.length!=2) throw new IllegalArgumentException("using Banked Kernel3D goal requires that" +
                        " filter registers are specified as '0:A' where 0 is the bank and A is the virtual" +
                        " Register. '"+ bank+ "; does not conform");
                return new BankedRegisterAllocator.BRegister(Integer.parseInt(strs[0]), strs[1]);
            }).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    protected List<? extends BankedRegisterAllocator.BRegister> getInputRegisters(){
        return getRegisterArray(config.getJSONObject("registerAllocator").getJSONArray("initialRegisters"));
    }

    @Override
    protected BankedRegisterAllocator<G, Scamp5SuperPixelTransformation<G>> makeRegisterAllocator() {
        JSONObject regAllocConf = config.getJSONObject("registerAllocator");
        switch (regAllocConf.getString("name")){
            case "linearScan":
                printLn("\tMaking Linear Scan Register Allocator:");
                List<BankedRegisterAllocator.BRegister> availableRegisters = new ArrayList<>(getRegisterArray(regAllocConf.getJSONArray("availableRegisters")));
                printLn("Available registers  : " + availableRegisters.toString());

                List<BankedRegisterAllocator.BRegister> available = new ArrayList<>(getOutputRegisters());
                for (BankedRegisterAllocator.BRegister availableRegister : availableRegisters) {
                    if (!available.contains(availableRegister)) {
                        available.add(availableRegister);
                    }
                }
                List<BankedRegisterAllocator.BRegister> initRegisters = new ArrayList<>(getInputRegisters());
                printLn("Initial registers    : " + initRegisters.toString());
                return new BankedLinearScanRegisterAllocator<>(available.stream().mapToInt(bRegister -> bRegister.bank).max().orElse(0), initRegisters, initialGoals, available);
            default:
                throw new IllegalArgumentException("Register Allocator Unknown");
        }

    }



    @Override
    protected PairGenFactory<G,Scamp5SuperPixelTransformation<G>> makePairGenFactory() {
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


        if(!json.has("selectReg")) {throw new IllegalArgumentException("you need to define " + "selectReg" + " inside pairGen");}
        String selectReg = json.getString("selectReg");

        if(!json.has("maskReg")) {throw new IllegalArgumentException("you need to define " + "maskReg" + " inside pairGen");}
        String maskReg = json.getString("maskReg");

        if(!json.has("maskedReg")) {throw new IllegalArgumentException("you need to define " + "maskedReg" + " inside pairGen");}
        String maskedReg = json.getString("maskedReg");

        if(!json.has("northReg")) {throw new IllegalArgumentException("you need to define " + "northReg" + " inside pairGen");}
        String northReg = json.getString("northReg");

        if(!json.has("eastReg")) {throw new IllegalArgumentException("you need to define " + "eastReg" + " inside pairGen");}
        String eastReg = json.getString("eastReg");

        if(!json.has("southReg")) {throw new IllegalArgumentException("you need to define " + "southReg" + " inside pairGen");}
        String southReg = json.getString("southReg");

        if(!json.has("westReg")) {throw new IllegalArgumentException("you need to define " + "westReg" + " inside pairGen");}
        String westReg = json.getString("westReg");

        if(!json.has("width")) {throw new IllegalArgumentException("you need to define " + "width" + " inside pairGen");}
        int width = json.getInt("width");

        if(!json.has("height")) {throw new IllegalArgumentException("you need to define " + "height" + " inside pairGen");}
        int height = json.getInt("height");

        if(!json.has("bitOrder")) {throw new IllegalArgumentException("you need to define " + "bitOrder" + " inside pairGen");}
        JSONArray jbitOrder = json.getJSONArray("bitOrder");
        int banks = jbitOrder.length();
        int[][][] bitOrder = new int[banks][width][height];
        for (int i = 0; i < bitOrder.length; i++) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitOrder[i][x][y] = jbitOrder.getJSONArray(i).getJSONArray(x).getInt(y);
                }
            }
        }

        Scamp5SuperPixelConfig<G> scampConfig = new Scamp5SuperPixelConfig<G>(true, true, true, true, true, scratchRegs, selectReg, maskReg, maskedReg, northReg, eastReg, southReg, westReg, width, height, banks, bitOrder);

        if(!json.has("configGetter")) {throw new IllegalArgumentException("you need to define " + "configGetter" + " inside pairGen");}
        String configGetterName = json.getJSONObject("configGetter").getString("name");
        printLn("Config Getter               : " + configGetterName);
        switch (configGetterName) {
            default:
                throw new IllegalArgumentException("Unknown Scamp5 Scamp5ConfigGetter " + json.getString("configGetter"));
            case "Threshold":
                return getThresholdPairGenFactory(json.getJSONObject("configGetter"), scampConfig);
            case "Exhaustive":
                return getExhaustivePairGenFactory(json.getJSONObject("configGetter"), scampConfig);
            case "AtomDistanceSorted":
                return getAtomDistanceSortedPairGenFactory(json.getJSONObject("configGetter"), scampConfig);
            case "AtomDistance":
                return getAtomDistancePairGenFactory(json.getJSONObject("configGetter"), scampConfig);
        }

    }

    private Scamp5SuperPixelPairGenFactory<G> getThresholdPairGenFactory(JSONObject json, Scamp5SuperPixelConfig<G> scampConfig) {
        if(!json.has("threshold")) {throw new IllegalArgumentException("you need to define " + "threshold" + " inside configGetter");}
        int threshold = json.getInt("threshold");
        printLn("Exhaustive Search Threshold  : " + threshold);
        CostHeuristic<G, Scamp5SuperPixelTransformation<G>> heuristic = getCostHeuristic(json, "heuristic");

        return new Scamp5SuperPixelPairGenFactory<>(
                new ThresholdScamp5ConfigGetter<>(
                        initialGoals, threshold,
                        heuristic, scampConfig,
                        (goals, conf, scamp5Config, h) -> new Scamp5SuperPixelPairGenFactory.AtomDistanceSortedPairGen<>(goals, conf, scampConfig, heuristic),
                        (goals, conf, scamp5Config, h) -> new Scamp5SuperPixelPairGenFactory.ExhaustivePairGen<>(goals, conf, scampConfig, heuristic)
                )
        );
    }

    private Scamp5SuperPixelPairGenFactory<G> getExhaustivePairGenFactory(JSONObject json, Scamp5SuperPixelConfig<G> scampConfig) {
        CostHeuristic<G, Scamp5SuperPixelTransformation<G>> heuristic = getCostHeuristic(json, "heuristic");
        return new Scamp5SuperPixelPairGenFactory<>(new BasicScamp5ConfigGetter<>(scampConfig,
                (goals, conf, scamp5Config) -> new Scamp5SuperPixelPairGenFactory.ExhaustivePairGen<>(goals, conf, scamp5Config, heuristic)
        ));
    }
    private Scamp5SuperPixelPairGenFactory<G> getAtomDistanceSortedPairGenFactory(JSONObject json, Scamp5SuperPixelConfig<G> scampConfig) {
        CostHeuristic<G, Scamp5SuperPixelTransformation<G>> heuristic = getCostHeuristic(json, "heuristic");
        return new Scamp5SuperPixelPairGenFactory<>(new BasicScamp5ConfigGetter<>(scampConfig,
                (goals, conf, scamp5Config) -> new Scamp5SuperPixelPairGenFactory.AtomDistanceSortedPairGen<>(goals, conf, scamp5Config, heuristic)
        ));
    }
    private Scamp5SuperPixelPairGenFactory<G> getAtomDistancePairGenFactory(JSONObject json, Scamp5SuperPixelConfig<G> scampConfig) {
        return new Scamp5SuperPixelPairGenFactory<>(new BasicScamp5ConfigGetter<>(scampConfig,
                (goals, conf, scamp5Config) -> new Scamp5SuperPixelPairGenFactory.AtomDistancePairGen<>(goals, conf, scamp5Config)
        ));
    }

    private CostHeuristic<G, Scamp5SuperPixelTransformation<G>> getCostHeuristic(JSONObject json, String name) {
        if(!json.has(name)) {throw new IllegalArgumentException("you need to define " + name + " inside configGetter");}
        printLn("CostHeuristic to use          : " + json.getString(name));
        switch (json.getString(name)) {
            default:
                throw new IllegalArgumentException("Unknown Heuristic option " + json.getString(name));
            case "Pattern":
                return null;//new PatternHeuristic<>(initialGoals);
        }
    }



    public static class ArrayGoalFileRun extends Scamp5SuperPixelFileRun<BankedArrayGoal> {
        public ArrayGoalFileRun(JSONObject config) {
            super(config);
        }

        @Override
        protected Kernel3DGoal.Kernel3DGoalFactory<BankedArrayGoal> getGoalFactory() {
            return new BankedArrayGoal.Factory(0);
        }
    }
}
