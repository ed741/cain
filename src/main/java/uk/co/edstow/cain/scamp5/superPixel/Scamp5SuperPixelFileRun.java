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
import uk.co.edstow.cain.scamp5.output.OutputCode;
import uk.co.edstow.cain.scamp5.output.Scamp5DefaultOutputFormatter;
import uk.co.edstow.cain.scamp5.output.Scamp5JssOutputFormatter;
import uk.co.edstow.cain.scamp5.output.Scamp5OutputFormatter;
import uk.co.edstow.cain.structures.Plan;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public abstract class Scamp5SuperPixelFileRun<G extends BankedKernel3DGoal<G>> extends Kernel3DBankedFileRun<G, Scamp5SuperPixelTransformation<G>> {

    private Scamp5SuperPixelConfig spConfig;

    public Scamp5SuperPixelFileRun(JSONObject config) {
        super(config);
    }

    @Override
    protected boolean isThreeDimensional() {
        return false;
    }

    @Override
    protected int getBanks() {
        if(spConfig != null){
            return spConfig.banks;
        } else {
            return config.getJSONObject("pairGen").getJSONArray("bitOrder").length();
        }
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

        String flagReg = json.optString("flagReg", null);
        configBuilder.flagReg(flagReg);

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
                boolean refreshDNEWS = outputFormatConfig.optBoolean("refreshDNEWS", false);
                outputFormatter = new Scamp5DefaultOutputFormatter(refreshDNEWS);
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
        spConfig = configBuilder.build();
        return new Generator<>(new Scamp5SuperPixelDirectSolver<>(spConfig), pairGenFactory);

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
        return configBuilder.useMovbx(true).useAdd(true).useAddSelf(true).useSub(true).useDiv(true).useRes(true).useNeg(true);
    }


    private CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> getCostHeuristic(JSONObject json) {
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

    private void doBias(OutputCode code) {
        JSONObject json = config.getJSONObject("pairGen");
        if(!json.has("bias")) return;
        printLn("adding Bias to output");
        JSONObject jBias = json.getJSONObject("bias");
        int unitBit = jBias.getInt("unitBit");
        printLnVerbose("unit Bit: %d", unitBit);
        if (!this.spConfig.allSameShape()){
            throw new IllegalArgumentException("Extras only work when all banks have the same shape (and number of bits)");
        }
        if(unitBit < 0 || unitBit > this.spConfig.getBits(this.initialGoals.get(0).getBank())) {
            throw new IllegalArgumentException("UnitBits must be between 1 and the number of bits in each bank (inclusive)");
        }
        JSONArray biass = jBias.getJSONArray("weights");
        String biasDRegister = jBias.getString("biasReg");
        JSONArray inRegisters = jBias.getJSONArray("inRegisters");
        JSONArray outRegisters = jBias.getJSONArray("outRegisters");
        if(biass.length() != outRegisters.length() || biass.length() != inRegisters.length()) {
            throw new IllegalArgumentException("Weights[], outRegisters[] and inRegisters[] must be the same length");
        }
        code.addOutput(spConfig.outputFormatter.comment("Adding Bias:"));
        code.addOutput(spConfig.outputFormatter.newLine());
        for (int i = 0; i < biass.length(); i++) {
            BRegister inReg = BRegister.makeBRegister(inRegisters.getString(i));
            BRegister outReg = BRegister.makeBRegister(outRegisters.getString(i));
            if (outReg.bank != inReg.bank)
                throw new IllegalArgumentException(String.format("Specified output Register '%s' must be in the same bank as input register '%s", outReg, inReg));
            if (outReg.equals(inReg))
                throw new IllegalArgumentException(String.format("Specified output Register '%s' must not be the same as input register '%s", outReg, this.getOutputRegisters().get(i)));
            int bank = inReg.bank;
            BigDecimal biasI = biass.getBigDecimal(i);
            BigInteger shiftedBiasI = biasI.multiply(new BigDecimal(2).pow(unitBit - 1)).toBigInteger();
            boolean[] value = new boolean[spConfig.getBits(0)];
            boolean fill = shiftedBiasI.signum()<0;
            for (int bit = 0; bit < value.length; bit++) {
                boolean v =  bit < shiftedBiasI.bitLength() ? shiftedBiasI.testBit(bit) : fill;
                value[bit] = v;
            }
            if(jBias.optBoolean("enabled", true)) {
                code.addOutput(spConfig.outputFormatter.comment("Adding " + biasI + " to: " + inReg));
                code.addOutput(spConfig.outputFormatter.newLine());
                List<String> scratchRegs = new ArrayList<>(spConfig.scratchRegisters);
                scratchRegs.add(spConfig.northReg);
                scratchRegs.add(spConfig.eastReg);
                scratchRegs.add(spConfig.southReg);
                scratchRegs.add(spConfig.westReg);
                spConfig.setValue(code, bank, value, biasDRegister, scratchRegs);
                code.addOutput(spConfig.outputFormatter.newLine());
                BRegister biasReg = new BRegister(bank, biasDRegister);


                code.addOutput(Scamp5SuperPixelTransformation.AddSub_2.shortCode(spConfig, outReg, inReg, biasReg, false));
            } else {
                code.addOutput(spConfig.outputFormatter.comment("Skipping " + biasI + " to: "+ inReg));
                code.addOutput(spConfig.outputFormatter.newLine());
            }
        }
        code.addOutput(spConfig.outputFormatter.comment("Done - Adding Bias"));
        code.addOutput(spConfig.outputFormatter.newLine());
    }

    private void doRelu(OutputCode code) {
        JSONObject json = config.getJSONObject("pairGen");
        if(!json.has("activation")) return;
        JSONObject jActivation = json.getJSONObject("activation");
        if (!jActivation.optBoolean("enabled", true)) {
            printLnVerbose("Skipping Activation");
            return;
        }
        String activation = jActivation.getString("name");
        printLn("applying activation: '%s' to output", activation);
        if(!activation.equals("relu")) {
            throw new UnsupportedOperationException("'relu' is the only supported activation function");
        }
        JSONArray registers = jActivation.getJSONArray("registers");
        for (int goal = 0; goal < registers.length(); goal++) {
            BRegister reg = BRegister.makeBRegister(registers.getString(goal));
            code.addOutput(spConfig.outputFormatter.comment("Applying Relu to "+reg));
            code.addOutput(spConfig.outputFormatter.newLine());
            int highestBit = spConfig.getBits(reg.bank);

            code.addOutput(spConfig.outputFormatter.SET(spConfig.maskReg));
            code.addOutput(spConfig.outputFormatter.SET(spConfig.maskedReg)); // maskedReg is 1 everywhere
            int steps = spConfig.selectFlood(code, reg.bank, highestBit, spConfig.maskReg, spConfig.scratchRegisters);
            code.addOutput(spConfig.outputFormatter.NOT(spConfig.maskedReg, reg.name)); // maskedReg at highest bit is 0 iff negative. everywhere else is 1
            code.addOutput(spConfig.outputFormatter.OR(spConfig.maskReg, spConfig.northReg, spConfig.eastReg, spConfig.southReg, spConfig.westReg));
            // Dir registers are set for PE's in this bank and Mask is set iff the PE has a dir reg set (so PEs outside the bank and the highest bit in the bank are 0)
            for (int i = 0; i < steps; i++) {
                code.addOutput(spConfig.outputFormatter.DNEWS0(spConfig.scratchRegisters.get(0), spConfig.maskedReg));
                code.addOutput(spConfig.outputFormatter.MOV(spConfig.maskedReg, spConfig.scratchRegisters.get(0)));
            } // if negative flood 0 across the bank, else flood 1 across the bank. nonBank PEs have 1

            code.addOutput(spConfig.outputFormatter.MOV(spConfig.maskReg, spConfig.maskedReg));// maskReg is 0 if output should be 0. maskReg is 1 if output should be equal to input
            code.addOutput(spConfig.outputFormatter.MOV(spConfig.maskedReg, reg.name));// write input value only if it shouldn't be 0
            code.addOutput(spConfig.outputFormatter.MOV(reg.name, spConfig.maskedReg)); // copy out value to Reg.
            code.addOutput(spConfig.outputFormatter.newLine());
        }


    }

    private void doADC(OutputCode code) {
        printLnVerbose("Doing ADC");
        JSONObject json = config.getJSONObject("pairGen");
        if(!json.has("adc")) return;
        JSONObject jADCConf = json.getJSONObject("adc");
        final Register scratch = new Register(jADCConf.getString("analogueScratchReg"));
        JSONArray jADCs = jADCConf.getJSONArray("channels");
        if(spConfig.flagReg == null){throw new IllegalArgumentException("you need to define " + "flagReg" + " inside pairGen");}

        if (!this.spConfig.allSameShape()){
            throw new IllegalArgumentException("Extras only work when all banks have the same shape (and number of bits)");
        }

        for (int i = 0; i < jADCs.length(); i++) {
            printLnVerbose("Starting ADC %s", i);
            JSONObject jADC = jADCs.getJSONObject(i);
            if(!jADC.optBoolean("enabled", true)){
                printLnVerbose("Skipping ADC %s", i);
                continue;
            }
            final int bitDepth = jADC.getInt("bitDepth");
            printLnVerbose("Bit Depth: %d", bitDepth);
            final int unitBit = jADC.getInt("unitBit");
            printLnVerbose("unit Bit: %d", unitBit);
            if(unitBit < 0 || unitBit > this.spConfig.getBits(this.initialGoals.get(0).getBank())) {
                throw new IllegalArgumentException("UnitBits must be between 1 and the number of bits in each bank (inclusive)");
            }

            final Register analogueIn = new Register(jADC.getString("analogueReg"));
            printLnVerbose("AnalogueReg = %s", analogueIn);
            final BRegister digitalOut = BRegister.makeBRegister(jADC.getString("digitalReg"));
            printLnVerbose("DigitalReg = %s", digitalOut);
            final int startBit = unitBit + bitDepth - 1;

            code.addOutput(spConfig.outputFormatter.comment("Analogue to Digital Conversion. "+analogueIn+" -> " +digitalOut));
            code.addOutput(spConfig.outputFormatter.all()); //Flag = true
            code.addOutput(spConfig.outputFormatter.SET(spConfig.maskReg));
            code.addOutput(spConfig.outputFormatter.CLR(spConfig.maskedReg)); // masked reg cleared
            code.addOutput(spConfig.outputFormatter.newLine());

            int comparator = -128;
            for (int b = 0; b < bitDepth; b++) {
                int bit = startBit - b;
                printLnVerbose("Step: "+b+" Bit: "+bit+" comparator: "+comparator);
                code.addOutput(spConfig.outputFormatter.comment("Step: "+b+" Bit: "+bit+" comparator: "+comparator));

                if(b > 0){
                    code.addOutput(spConfig.outputFormatter.movx(analogueIn, analogueIn, spConfig.getDirMoreSignificant(digitalOut.bank, bit)));
                }

                code.addOutput(spConfig.outputFormatter.kernel_end());
                code.addOutput(spConfig.outputFormatter.in(scratch, comparator));
                spConfig.selectBit(code, digitalOut.bank, bit, spConfig.maskReg, false);
                code.addOutput(spConfig.outputFormatter.kernel_begin());
                if(b > 0){
                    code.addOutput(spConfig.outputFormatter.sub(analogueIn, analogueIn, scratch));
                }
                code.addOutput(spConfig.outputFormatter.where(analogueIn));
                code.addOutput(spConfig.outputFormatter.MOV(spConfig.maskedReg, spConfig.flagReg));
                code.addOutput(spConfig.outputFormatter.add(analogueIn, analogueIn, scratch));
                code.addOutput(spConfig.outputFormatter.all());

                comparator /= 2;
                code.addOutput(spConfig.outputFormatter.newLine());
            }

            code.addOutput(spConfig.outputFormatter.MOV(digitalOut.name, spConfig.maskedReg));
            code.addOutput(spConfig.outputFormatter.newLine());
            printLnVerbose("Finished ADC %s", i);

        }
        printLnVerbose("Finished ADCs");
    }

    private void doDAC(OutputCode code) {
        printLnVerbose("Doing DAC");
        JSONObject json = config.getJSONObject("pairGen");
        if(!json.has("dac")) return;
        JSONObject jDACConf = json.getJSONObject("dac");
        final Register scratch = new Register(jDACConf.getString("analogueScratchReg"));
        JSONArray jDACs = jDACConf.getJSONArray("channels");

        if (!this.spConfig.allSameShape()){
            throw new IllegalArgumentException("Extras only work when all banks have the same shape (and number of bits)");
        }

        for (int i = 0; i < jDACs.length(); i++) {

            printLnVerbose("Starting DAC %s", i);
            JSONObject jDAC = jDACs.getJSONObject(i);
            if(!jDAC.optBoolean("enabled", true)){
                printLnVerbose("Skipping DACC %s", i);
                continue;
            }
            final int bitDepth = jDAC.getInt("bitDepth");
            printLnVerbose("Bit Depth: %d", bitDepth);
            final int unitBit = jDAC.getInt("unitBit");
            printLnVerbose("unit Bit: %d", unitBit);
            if(unitBit < 1 || unitBit > this.spConfig.getBits(this.initialGoals.get(0).getBank())) {
                throw new IllegalArgumentException("UnitBits must be between 1 and the number of bits in each bank (inclusive)");
            }

            final BRegister digitalIn = BRegister.makeBRegister(jDAC.getString("digitalReg"));
            printLnVerbose("DigitalReg = %s", digitalIn);
            final Register analogueOut = new Register(jDAC.getString("analogueReg"));
            printLnVerbose("AnalogueReg = %s", analogueOut);
            final int startBit = unitBit + bitDepth - 1;
            if(startBit == spConfig.getBits(digitalIn.bank)){
                printLnVerbose("StartBit = number of bits - this does not bode well for two's complement");
            }

            code.addOutput(spConfig.outputFormatter.comment("Digital to Analogue Conversion. "+digitalIn+" -> " +analogueOut));
            code.addOutput(spConfig.outputFormatter.all());
            code.addOutput(spConfig.outputFormatter.kernel_end());
            code.addOutput(spConfig.outputFormatter.in(analogueOut, -128));
            code.addOutput(spConfig.outputFormatter.kernel_begin());

            code.addOutput(spConfig.outputFormatter.newLine());

            if(spConfig.getBits(digitalIn.bank) - 1 > startBit) {
                code.addOutput(spConfig.outputFormatter.comment("Detect Overflow/OverExposure"));
                code.addOutput(spConfig.outputFormatter.SET(spConfig.maskReg));
                code.addOutput(spConfig.outputFormatter.MOV(spConfig.maskedReg, digitalIn.name));
                if (spConfig.getBits(digitalIn.bank) - 2 > startBit) {
                    spConfig.setDirMoreSignificant(code, digitalIn.bank);
                    for (int b = 0; b < spConfig.getBits(digitalIn.bank) - 2 - startBit; b++) {
                        code.addOutput(spConfig.outputFormatter.DNEWS0(spConfig.scratchRegisters.get(0), spConfig.maskedReg));
                        code.addOutput(spConfig.outputFormatter.OR(spConfig.maskedReg, spConfig.scratchRegisters.get(0), digitalIn.name));
                    }
                }
                code.addOutput(spConfig.outputFormatter.kernel_end());
                code.addOutput(spConfig.outputFormatter.in(scratch, -128));
                int steps = spConfig.selectFlood(code, digitalIn.bank, startBit+1, null, spConfig.scratchRegisters, true, false);
                code.addOutput(spConfig.outputFormatter.kernel_begin());

                code.addOutput(spConfig.outputFormatter.OR(spConfig.maskReg, spConfig.northReg, spConfig.eastReg, spConfig.southReg, spConfig.westReg));
                // Dir registers are set for all PE's except at bit => mask 1 for all PEs except bit
                for (int step = 0; step < steps; step++) {
                    code.addOutput(spConfig.outputFormatter.DNEWS0(spConfig.scratchRegisters.get(0), spConfig.maskedReg));
                    code.addOutput(spConfig.outputFormatter.MOV(spConfig.maskedReg, spConfig.scratchRegisters.get(0)));
                } // if digitalIn[bit] is 1: flood 1, else flood 0 across the Super pixel
                code.addOutput(spConfig.outputFormatter.WHERE(spConfig.maskedReg)); // enable analogue side iff digitalIn[bit] is 1 for any bit from [msb-1 to startBit+1]
                code.addOutput(spConfig.outputFormatter.sub(analogueOut, analogueOut, scratch)); // -128 - -128 = 0
                code.addOutput(spConfig.outputFormatter.sub(analogueOut, analogueOut, scratch)); // 0 - -128 = 128= over exposed
                code.addOutput(spConfig.outputFormatter.newLine());
            }

            int value = -128;
            for (int b = 0; b < bitDepth; b++) {
                int bit = startBit - b;
                printLnVerbose("Step: "+b+" Bit: "+bit+" value: "+value);
                code.addOutput(spConfig.outputFormatter.comment("Step: "+b+" Bit: "+bit+" value: "+value));

                code.addOutput(spConfig.outputFormatter.SET(spConfig.maskReg));
                code.addOutput(spConfig.outputFormatter.MOV(spConfig.maskedReg, digitalIn.name)); // maskedReg at bit = digitalIn

                code.addOutput(spConfig.outputFormatter.kernel_end());
                code.addOutput(spConfig.outputFormatter.in(scratch, value));
                int steps = spConfig.selectFlood(code, digitalIn.bank, bit, null, spConfig.scratchRegisters, true, false);
                code.addOutput(spConfig.outputFormatter.kernel_begin());


                code.addOutput(spConfig.outputFormatter.OR(spConfig.maskReg, spConfig.northReg, spConfig.eastReg, spConfig.southReg, spConfig.westReg));
                // Dir registers are set for all PE's except at bit => mask 1 for all PEs except bit
                for (int step = 0; step < steps; step++) {
                    code.addOutput(spConfig.outputFormatter.DNEWS0(spConfig.scratchRegisters.get(0), spConfig.maskedReg));
                    code.addOutput(spConfig.outputFormatter.MOV(spConfig.maskedReg, spConfig.scratchRegisters.get(0)));
                } // if digitalIn[bit] is 1: flood 1, else flood 0 across the Super pixel

//                if(b == 0){
//                    sb.append(spConfig.outputFormatter.NOT(spConfig.northReg, spConfig.maskedReg));
//                    sb.append(spConfig.outputFormatter.WHERE(spConfig.northReg)); // enable analogue side iff digitalIn[bit] is 0. for most significant bit
//                    sb.append(spConfig.outputFormatter.add(analogueOut, analogueOut, scratch)); // add -128 to 0 to get -128 iff digitalIn[bit=0] is 0
//                } else {
                code.addOutput(spConfig.outputFormatter.WHERE(spConfig.maskedReg)); // enable analogue side iff digitalIn[bit] is 1.
                code.addOutput(spConfig.outputFormatter.sub(analogueOut, analogueOut, scratch));
//                }
                code.addOutput(spConfig.outputFormatter.all());

                value /= 2;
                code.addOutput(spConfig.outputFormatter.newLine());
            }

            if(startBit != spConfig.getBits(digitalIn.bank)){
                // if highest bit is set then we should reset analogue value to -128 as this is a negative number (under-exposed)
                code.addOutput(spConfig.outputFormatter.comment("Detect Underflow/UnderExposure"));
                code.addOutput(spConfig.outputFormatter.SET(spConfig.maskReg));
                code.addOutput(spConfig.outputFormatter.MOV(spConfig.maskedReg, digitalIn.name));

                code.addOutput(spConfig.outputFormatter.kernel_end());
                code.addOutput(spConfig.outputFormatter.in(scratch, -128));
                int steps = spConfig.selectFlood(code, digitalIn.bank, spConfig.getBits(digitalIn.bank), null, spConfig.scratchRegisters, true, false);
                code.addOutput(spConfig.outputFormatter.kernel_begin());
                code.addOutput(spConfig.outputFormatter.OR(spConfig.maskReg, spConfig.northReg, spConfig.eastReg, spConfig.southReg, spConfig.westReg));
                // Dir registers are set for all PE's except at bit => mask = 1 for all PEs except bit
                for (int step = 0; step < steps; step++) {
                    code.addOutput(spConfig.outputFormatter.DNEWS0(spConfig.scratchRegisters.get(0), spConfig.maskedReg));
                    code.addOutput(spConfig.outputFormatter.MOV(spConfig.maskedReg, spConfig.scratchRegisters.get(0)));
                } // if digitalIn[msb] is 1: flood 1, else flood 0 across the Super pixel
                code.addOutput(spConfig.outputFormatter.WHERE(spConfig.maskedReg)); // enable analogue side iff digitalIn[bit] is 1.
                code.addOutput(spConfig.outputFormatter.mov(analogueOut, scratch)); // enable analogue side iff digitalIn[bit] is 1.


            }


            code.addOutput(spConfig.outputFormatter.newLine());


        }
    }


    @Override
    protected OutputCode generateCode(Plan<G, Scamp5SuperPixelTransformation<G>, BRegister> p){
        printLnVerbose("Generating Code for plan with Length %s, Cost %s", p.depth(), p.totalInstructionCost());
        RegisterAllocator.Mapping<G,BRegister> mapping = registerAllocator.solve(p);
        printLnVerbose("Registers allocated");
        StringBuilder sb = new StringBuilder();
        OutputCode code = new OutputCode();
        code.addOutput(spConfig.outputFormatter.kernel_begin());
        code.addOutput(spConfig.outputFormatter.newLine());
        this.doADC(code);
        code.addOutput(spConfig.outputFormatter.comment("Kernel Code!"));
        code.addOutput(spConfig.outputFormatter.newLine());
        code.addOutput(spConfig.outputFormatter.comment("Inputs in: " + mapping.initRegisters().toString()));
        code.addOutput(spConfig.outputFormatter.newLine());
        printLnVerbose("ProduceCode");
        code.addOutput(p.produceCode(mapping));
        printLnVerbose("ProducedCode");
        code.addOutput(spConfig.outputFormatter.newLine());
        this.doBias(code);
        this.doRelu(code);
        this.doDAC(code);
        code.addOutput(spConfig.outputFormatter.kernel_end());
        code.addOutput(spConfig.outputFormatter.newLine());
        printLnVerbose("Code Generated");
        return code;
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
