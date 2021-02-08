package uk.co.edstow.cain.scamp5.emulator;

import uk.co.edstow.cain.RegisterAllocator;
import uk.co.edstow.cain.atomGoal.AtomGoal;
import uk.co.edstow.cain.util.Tuple;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scamp5Emulator {
    @SuppressWarnings("WeakerAccess")
    public static int verbose = 0;

    private final int xMin;
    private final int xMax;
    private final int yMin;
    private final int yMax;
    private final Map<Pos, ProcessingElement> tiles;
    private final Pattern instructionPatten;
    private final Pattern argPattern;
    private final Pattern dirPattern;
    private final Pattern regPattern;
    private final Map<InstructionSignature, Consumer<String[]>> instructionSet;
    private final Queue<Instruction> instructionBuffer;
    private final Reg[] realRegisters;


    private static class UndefinedInstructionBehaviour extends RuntimeException{
        UndefinedInstructionBehaviour(String s) {
            super(s);
        }
    }

    private interface Instruction {
        void run(ProcessingElement pe);
        @SuppressWarnings("SameReturnValue")
        default boolean enabled(Pos p){
            return true;
        }
    }

    static class NoiseConfig {
        public final double writeNoiseConstant = 0.000;
        public final double writeNoiseFactor = 0.005;
        public final double readNoiseConstant = 0;
        public final double readNoiseFactor = 0;
        private final Random r = new Random(2002);
        double getValue(double noise){
            return r.nextGaussian() * noise;
        }
    }


    static class Reg extends RegisterAllocator.Register {
        private static Reg News = new Reg(0,0,"News");
        private static Reg XNorth = new Reg(0,1,"XNorth");
        private static Reg XEast = new Reg(1,0,"XEast");
        private static Reg XSouth = new Reg(0,-1,"XSouth");
        private static Reg XWest = new Reg(-1,0,"XWest");
//        News(0, 0), A(0, 0),B(0, 0),C(0, 0),D(0, 0),E(0, 0),F(0, 0),
//
//        G(0, 0),H(0, 0),I(0, 0),J(0, 0),K(0, 0),L(0, 0),// Extras
//        M(0, 0),N(0, 0),O(0, 0),P(0, 0),Q(0, 0),R(0, 0),// Extras
//        S(0, 0),T(0, 0),U(0, 0),V(0, 0),W(0, 0),X(0, 0),Y(0, 0),Z(0, 0),// Extras
//
//        XNorth(0, 1),XEast(1, 0),XSouth(0, -1),XWest(-1, 0);

        public static Reg[] getRegisters(int count){
            Reg[] out = new Reg[count];
            for (int i = 0; i < count; i++) {
                out[i] = new Reg(i);
            }
            return out;
        }
        public static Reg[] getRealRegisters(int count){
            Reg[] out = new Reg[count+1];
            out[0] = Reg.News;
            for (int i = 1; i < out.length; i++) {
                out[i] = new Reg(i);
            }
            return out;
        }

        public static Reg[] getRegisters(RegisterAllocator.Register[] array){
            Reg[] out = new Reg[array.length];
            for (int i = 0; i < out.length; i++) {
                out[i] = new Reg(0,0,array[i].name);
            }
            return out;
        }
        public static Reg[] getRealRegisters(RegisterAllocator.Register[] array){
            Reg[] out = new Reg[array.length+1];
            out[0] = Reg.News;
            for (int i = 1; i < out.length; i++) {
                out[i] = new Reg(0,0,array[i-1].name);
            }
            return out;
        }
        
        public static Reg valueOf(String name, Reg[] possible){
            for (Reg reg : possible) {
                if(name.equals(reg.name)){
                    return reg;
                }
            }
            return null;
        }
        
        public static Reg[] getNeighbourRegisters(){
            return new Reg[]{XNorth, XEast, XSouth, XWest};
        }
        
        private final int realX;
        private final int realY;

        Reg(int realX, int realY, String name) {
            super(name);
            this.realX = realX;
            this.realY = realY;
        }
        
        Reg(int i){
            super(i);
            this.realX = 0;
            this.realY = 0;
        }

        private boolean real() {
            return realX == 0 && realY == 0;
        }
    }

    public Scamp5Emulator(BufferedImage img, Reg reg) {
        this(img.getWidth(), img.getHeight());
        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                int rgba = img.getRGB(i, j);
                int a = (rgba >> 24) & 0xFF;
                int r = (rgba >> 16) & 0xFF;
                int g = (rgba >> 8) & 0xFF;
                int b = rgba & 0xFF;
                int v = (r+g+b)/6;
                v = v>128?128:v;
                v = v<-128?-128:v;
                ProcessingElement pe = tiles.get(new Pos(i, j));
                pe.input(reg, v);
            }
        }

    }
    @SuppressWarnings("WeakerAccess")
    public Scamp5Emulator(int width, int height) {
        this(0, width, 0, height);
    }
    public static Scamp5Emulator newWithRegs(int width, int height, int regs){
        return new Scamp5Emulator(0, width, 0, height, regs);
    }

    public Scamp5Emulator(int rad) {
        this(-rad, rad+1, -rad, rad+1);
    }
    public static Scamp5Emulator newWithRegs(int rad, int regs){
        return new Scamp5Emulator(-rad, rad+1, -rad, rad+1, regs);
    }
    public static Scamp5Emulator newWithRegs(int rad, RegisterAllocator.Register[] regs){
        return new Scamp5Emulator(-rad, rad+1, -rad, rad+1, regs);
    }

    public Scamp5Emulator(int xMin, int xMax, int yMin, int yMax) {
        this(xMin, xMax, yMin, yMax, 6);
    }

    public Scamp5Emulator(int xMin, int xMax, int yMin, int yMax, int r) {
        this(xMin, xMax, yMin, yMax, Reg.getRealRegisters(r));
    }
    public Scamp5Emulator(int xMin, int xMax, int yMin, int yMax, RegisterAllocator.Register[] r) {
        this(xMin, xMax, yMin, yMax, Reg.getRealRegisters(r));
    }

    @SuppressWarnings("WeakerAccess")
    private Scamp5Emulator(int xMin, int xMax, int yMin, int yMax, Reg[] realRegs) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.realRegisters = realRegs;
        NoiseConfig noiseConfig = new NoiseConfig();

        this.tiles = new HashMap<>((xMax-xMin)*(yMax-yMin));
        for (int i = xMin; i < xMax; i++) {
            for (int j = yMin; j < yMax; j++) {
                Pos position = new Pos(i, j);
                ProcessingElement pe = new ProcessingElement(position, noiseConfig, realRegisters);
                this.tiles.put(position, pe);
            }
        }

        for (int i = xMin; i < xMax; i++) {
            for (int j = yMin; j < yMax; j++) {
                ProcessingElement pe = this.tiles.get(new Pos(i, j));
                for (Reg reg : Reg.getNeighbourRegisters()) {
                    Pos p = new Pos(i+reg.realX, j + reg.realY);
                    if(this.tiles.containsKey(p)){
                        pe.addNeighbourRegister(reg, this.tiles.get(p), Reg.News);
                    } else {
                        // edge of sensor
                        pe.addExtraRegister(reg);
                    }
                }
            }
        }

        this.instructionPatten = Pattern.compile("([a-z0-9]*)\\(([^)]*)\\)\\s*");
        this.argPattern = Pattern.compile("([^\\s,]+)\\s*(?:,|$)");
        this.dirPattern = Pattern.compile("north|east|south|west");

        this.regPattern = Pattern.compile("[A-Z]+");


        instructionSet = new HashMap<>();
        instructionBuffer = new ArrayDeque<>();

        instructionSet.put(new InstructionSignature("input", new int[]{0,2}), args ->
                instructionBuffer.add(pe -> pe.input(regValueOf(args[0]), Integer.parseInt(args[1])))
        );

        instructionSet.put(new InstructionSignature("res", new int[]{0}), args -> {
            checkRegsReal(args, 0);
            instructionBuffer.add(pe -> pe.bus(Reg.News));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.News));
        });
        instructionSet.put(new InstructionSignature("res", new int[]{0, 0}), args -> {
            checkRegsReal(args, 0,1);
            instructionBuffer.add(pe -> pe.bus(Reg.News));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.News));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[1]), Reg.News));
        });

        instructionSet.put(new InstructionSignature("mov", new int[]{0,0}), args -> {
            checkRegsReal(args, 0,1);
            instructionBuffer.add(pe -> pe.bus(Reg.News,regValueOf(args[1])));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.News));
        });
        instructionSet.put(new InstructionSignature("add", new int[]{0,0,0}), args -> {
            checkRegsReal(args, 0,1,2);
            instructionBuffer.add(pe -> pe.bus(Reg.News,regValueOf(args[1]),regValueOf(args[2])));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.News));
        });
        instructionSet.put(new InstructionSignature("add", new int[]{0,0,0,0}), args -> {
            checkRegsReal(args, 0,1,2,3);
            instructionBuffer.add(pe -> pe.bus(Reg.News,regValueOf(args[1]),regValueOf(args[2]),regValueOf(args[3])));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.News));
        });
        instructionSet.put(new InstructionSignature("sub", new int[]{0,0,0}), args -> {
            checkRegsReal(args, 0,1,2);
            instructionBuffer.add(pe -> pe.bus(Reg.News,regValueOf(args[1])));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.News, regValueOf(args[2])));
        });
        instructionSet.put(new InstructionSignature("neg", new int[]{0,0}), args -> {
            checkRegsReal(args, 0,1);
            instructionBuffer.add(pe -> pe.bus(Reg.News));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.News, regValueOf(args[1])));
        });
        instructionSet.put(new InstructionSignature("abs", new int[]{0,0}), args -> {
            checkRegsReal(args, 0,1);
            instructionBuffer.add(pe -> pe.bus(Reg.News));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.News, regValueOf(args[1])));
            instructionBuffer.add(pe -> pe.bus(Reg.News, regValueOf(args[1])));
            instructionBuffer.add(pe -> pe.where(regValueOf(args[1])));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.News));
            instructionBuffer.add(pe -> pe.all());
        });

        instructionSet.put(new InstructionSignature("divq", new int[]{0,0}), args -> {
            checkRegsReal(args, 0,1);
            instructionBuffer.add(pe -> pe.bus2(regValueOf(args[0]), Reg.News, regValueOf(args[1])));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.News));
        });
        instructionSet.put(new InstructionSignature("div", new int[]{0,0,0}), args -> {
            checkRegsReal(args, 0,1,2);
            instructionBuffer.add(pe -> pe.bus2(regValueOf(args[0]), regValueOf(args[1]), regValueOf(args[2])));
            instructionBuffer.add(pe -> pe.bus(Reg.News, regValueOf(args[2]), regValueOf(args[1])));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[2]), Reg.News, regValueOf(args[0])));
            instructionBuffer.add(pe -> pe.bus2(regValueOf(args[0]), regValueOf(args[1]), regValueOf(args[2])));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), regValueOf(args[1])));
        });
        instructionSet.put(new InstructionSignature("div", new int[]{0,0,0,0}), args -> {
            checkRegsReal(args, 0,1,2,3);
            instructionBuffer.add(pe -> pe.bus2(regValueOf(args[0]), regValueOf(args[1]), regValueOf(args[3])));
            instructionBuffer.add(pe -> pe.bus(Reg.News, regValueOf(args[3]), regValueOf(args[1])));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[2]), Reg.News, regValueOf(args[0])));
            instructionBuffer.add(pe -> pe.bus2(regValueOf(args[0]), regValueOf(args[1]), regValueOf(args[2])));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), regValueOf(args[1])));
        });
        instructionSet.put(new InstructionSignature("diva", new int[]{0,0,0}), args -> {
            checkRegsReal(args, 0,1,2);
            instructionBuffer.add(pe -> pe.bus2(regValueOf(args[1]), regValueOf(args[2]), regValueOf(args[0])));
            instructionBuffer.add(pe -> pe.bus(Reg.News, regValueOf(args[1]), regValueOf(args[0])));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.News, regValueOf(args[2])));
            instructionBuffer.add(pe -> pe.bus2(regValueOf(args[1]), regValueOf(args[2]), regValueOf(args[0])));
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), regValueOf(args[1])));
        });

        instructionSet.put(new InstructionSignature("movx", new int[]{0,0,1}), args -> {
            checkRegsReal(args, 0,1);
            switch (args[2]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.XWest,regValueOf(args[1]))); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.XEast,regValueOf(args[1]))); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.XSouth,regValueOf(args[1]))); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.XNorth,regValueOf(args[1]))); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.News));
        });

        instructionSet.put(new InstructionSignature("mov2x", new int[]{0,0,1,1}), args -> {
            checkRegsReal(args, 0,1);
            switch (args[2]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.XWest,regValueOf(args[1]))); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.XEast,regValueOf(args[1]))); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.XSouth,regValueOf(args[1]))); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.XNorth,regValueOf(args[1]))); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
            switch (args[3]){
                case "east": instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.XEast)); break;
                case "west": instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.XWest)); break;
                case "north": instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.XNorth)); break;
                case "south": instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.XSouth)); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
        });

        instructionSet.put(new InstructionSignature("addx", new int[]{0,0,0,1}), args -> {
            checkRegsReal(args, 0,1,2);
            switch (args[3]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.XWest,regValueOf(args[1]),regValueOf(args[2]))); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.XEast,regValueOf(args[1]),regValueOf(args[2]))); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.XSouth,regValueOf(args[1]),regValueOf(args[2]))); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.XNorth,regValueOf(args[1]),regValueOf(args[2]))); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.News));
        });

        instructionSet.put(new InstructionSignature("add2x", new int[]{0,0,0,1,1}), args -> {
            checkRegsReal(args, 0,1,2);
            switch (args[3]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.XWest,regValueOf(args[1]),regValueOf(args[2]))); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.XEast,regValueOf(args[1]),regValueOf(args[2]))); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.XSouth,regValueOf(args[1]),regValueOf(args[2]))); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.XNorth,regValueOf(args[1]),regValueOf(args[2]))); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
            switch (args[4]){
                case "east": instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.XEast)); break;
                case "west": instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.XWest)); break;
                case "north": instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.XNorth)); break;
                case "south": instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.XSouth)); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
        });

        instructionSet.put(new InstructionSignature("subx", new int[]{0,0,1,0}), args -> {
            checkRegsReal(args, 0,1,3);
            switch (args[2]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.XWest,regValueOf(args[1]))); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.XEast,regValueOf(args[1]))); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.XSouth,regValueOf(args[1]))); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.XNorth,regValueOf(args[1]))); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
            instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.News, regValueOf(args[3])));
        });

        instructionSet.put(new InstructionSignature("sub2x", new int[]{0,0,1,1,0}), args -> {
            checkRegsReal(args, 0,1,4);
            switch (args[2]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.XWest,regValueOf(args[1]))); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.XEast,regValueOf(args[1]))); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.XSouth,regValueOf(args[1]))); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.XNorth,regValueOf(args[1]))); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
            switch (args[3]){
                case "east": instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.XEast, regValueOf(args[4]))); break;
                case "west": instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.XWest, regValueOf(args[4]))); break;
                case "north": instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.XNorth, regValueOf(args[4]))); break;
                case "south": instructionBuffer.add(pe -> pe.bus(regValueOf(args[0]), Reg.XSouth, regValueOf(args[4]))); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
        });





    }

    public BufferedImage getImage(Reg reg, boolean read){
        int width = xMax - xMin;
        int height = yMax - yMin;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                ProcessingElement pe = tiles.get(new Pos(i, j));
                double v;
                if(read){
                    v = pe.readOutValue(reg);
                } else {
                    v = pe.getValue(reg);
                }
                int vi = (int) ((v*128d)+128);
                vi = vi>255?255:vi;
                vi = vi<0?0:vi;
                int r = (vi << 16);
                int g = (vi << 8);
                int b = vi;
                int rgba = (0xFF000000 | r) | (g | b);
                img.setRGB(i, j, rgba);
            }
        }
        return img;
    }

    private void checkRegsReal(String[] args, int... idxs){
        for (int idx : idxs) {
            Reg r =Reg.valueOf(args[idx], realRegisters);
            if(r==null){
                throw new UndefinedInstructionBehaviour("Register: "+args[idx]+" unknown");
            }
            if(!r.real()){
                throw new UndefinedInstructionBehaviour("All registers in Macro Operations must be 'real'");
            }
        }
    }
    
    private Reg regValueOf(String name){
        Reg r =  Reg.valueOf(name, realRegisters);
        if(r==null){
            throw new UndefinedInstructionBehaviour("Register: "+name+" unknown");
        }
        return r;
    }


    public void run(String instruction){
        if (verbose > 1) System.out.println(instruction);
        pushInstruction(instruction);
        flushInstructionBuffer();
        if (verbose > 5) System.out.println("Result: \n"+tiles.get(new Pos(0,0)));

    }

    @SuppressWarnings("WeakerAccess")
    public void pushInstruction(String instruction){
        Tuple<InstructionSignature, String[]> t = parseInput(instruction);
        InstructionSignature instructionSignature = t.getA();
        String[] argumentArray = t.getB();

        this.instructionSet.get(instructionSignature).accept(argumentArray);
    }

    public void pushCode(String code){
        code = code.replaceAll("//.*(\n|$)", "");
        String[] split = code.split(";");
        for (String s : split) {
            s = s.trim();
            if(s.length()>0){
                pushInstruction(s);
            }
        }
    }

    public void runCode(String code){
        code = code.replaceAll("//.*(\n|$)", "");
        String[] split = code.split(";");
        for (String s : split) {
            s = s.trim();
            if(s.length()>0){
                run(s);
            }
        }
    }

    public void flushInstructionBuffer() {
        int i = 0;
        Instruction instruction = instructionBuffer.poll();
        while (instruction != null) {
            final Instruction finalInstruction = instruction;
            for (Map.Entry<Pos, ProcessingElement> entry : this.tiles.entrySet()) {
                Pos pos = entry.getKey();
                ProcessingElement pe = entry.getValue();
                if (finalInstruction.enabled(pos)) {
                    finalInstruction.run(pe);
                }
            }
            if (verbose > 10) System.out.println(i + " :: " + tiles.get(new Pos(0,0)));

            i++;
            instruction = instructionBuffer.poll();
        }

    }
    public Map<Tuple<Integer, Tuple<Integer, String>>, Double> getRawProcessingElementContains(int x, int y, String reg) {
        ProcessingElement pe = this.tiles.get(new Pos(x, y));
        if (pe == null) {
            return null;
        }
        Reg r = regValueOf(reg);
        return pe.getRawRegisterContains(r);
    }

    public String getRegToString(int x, int y, String reg){
        ProcessingElement pe = this.tiles.get(new Pos(x, y));
        if (pe == null) {
            return null;
        }
        Reg r = regValueOf(reg);
        return pe.getRegToString(r);
    }

    public AtomGoal.AtomBounds getRegCoverge(int x, int y, String reg){
        ProcessingElement pe = this.tiles.get(new Pos(x, y));
        if (pe == null) {
            return null;
        }
        Reg r = regValueOf(reg);
        return pe.getRegCoverage(r);
    }


    public Double readNoise(int x, int y, String reg) {
        ProcessingElement pe = this.tiles.get(new Pos(x, y));
        if (pe == null) {
            return null;
        }
        Reg r = regValueOf(reg);
        return pe.getNoise(r);
    }


    private Tuple<InstructionSignature, String[]> parseInput(String instruction) {
        Matcher matcher = this.instructionPatten.matcher(instruction);
        if (!matcher.matches() || matcher.groupCount() != 2) {
            throw new IllegalArgumentException("Cannot parse \"" + instruction + "\"");
        }
        String name = matcher.group(1);
        List<String> argList = new ArrayList<>();
        String arguments = matcher.group(2);
        Matcher argMatcher = this.argPattern.matcher(arguments);
        while(argMatcher.find() && argMatcher.groupCount()==1){
            argList.add(argMatcher.group(1));
        }
        String[] args = argList.toArray(new String[0]);
        int[] argType = new int[args.length];
        for (int i = 0; i <args.length ; i++) {
            String arg = args[i];
            if(this.regPattern.matcher(arg).matches()){
                argType[i] = 0;
            } else if(this.dirPattern.matcher(arg).matches()){
                argType[i] = 1;
            } else {
                try{
                    int integer = Integer.parseInt(arg);
                    argType[i] = 2;
                } catch (NumberFormatException e){
                    throw new IllegalArgumentException(String.format("Cannot parse Instruction \"%s\", Argument %d (\"%s\")is not Register or Direction or Integer", instruction, i, arg));
                }
            }
        }
        InstructionSignature in = new InstructionSignature(name, argType);
        if(!this.instructionSet.containsKey(in)){
            throw new IllegalArgumentException(String.format("Cannot parse Instruction \"%s\", No instruction signature matching %s found", instruction, in.toString()));
        }
        return new Tuple<>(in, args);
    }






    static class Pos {
        final int x;
        final int y;

        private Pos(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pos pos = (Pos) o;
            return x == pos.x &&
                    y == pos.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "Pos{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }

    static class OriginPos extends Pos{
        final Reg r;
        OriginPos(int x, int y, Reg r) {
            super(x, y);
            this.r = r;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OriginPos pos = (OriginPos) o;
            return x == pos.x &&
                    y == pos.y&&
                    r == pos.r;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, r);
        }

        @Override
        public String toString() {
            return "OPos{" +
                    "x=" + x +
                    ", y=" + y +
                    ", r=" + r +
                    '}';
        }
    }

    private class InstructionSignature {
        private final String identifier;
        private final int[] inputs;

        private InstructionSignature(String identifier, int[] inputs) {
            this.identifier = identifier;
            this.inputs = inputs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InstructionSignature that = (InstructionSignature) o;
            return Objects.equals(identifier, that.identifier) &&
                    Arrays.equals(inputs, that.inputs);
        }

        @Override
        public int hashCode() {

            int result = Objects.hash(identifier);
            result = 31 * result + Arrays.hashCode(inputs);
            return result;
        }

        @Override
        public String toString() {
            StringBuilder signature = new StringBuilder(identifier).append("\\").append(inputs.length).append(":[");
            for (int i = 0; i < inputs.length; i++) {
                switch (inputs[i]) {
                    case 0:
                        signature.append("Register(0)");
                        break;
                    case 1:
                        signature.append("Direction(1)");
                        break;
                    case 2:
                        signature.append("Integer(2)");
                        break;
                    default:
                        signature.append("UNKNOWN");

                }
                signature.append(i==inputs.length-1?"]":",");
            }
            return signature.toString();
        }
    }


    @SuppressWarnings("UnusedAssignment")
    public static void main(String[] args) {
        File imgFile = new File("src/main/resources/test.png");
        BufferedImage img = null;
        try {
            img = ImageIO.read(imgFile);
//            Scamp5Emulator e = new Scamp5Emulator(img, Reg.A);
            Scamp5Emulator e = new Scamp5Emulator(3);

            e.run("input(A,"+(1*128)+")");
//            e.run("input(A,"+(1*128)+")");

//            BufferedImage out = e.getImage(Reg.F, false);
//            ImageIO.write(out, "png", new File("out.png"));

//            e.run("movx(B,A,east)");
//            e.run("movx(C,A,west)");
//            e.run("mov2x(D,A,east,north)");
//            e.run("mov2x(E,A,south,west)");
//            e.run("add(F,B,C)");
//            e.run("movx(F,F,north)");
            verbose = 100;
//            e.runCode(
//                    "//Kernel Code!\n" +
//                            "mov2x(F, A, south, west);//[E, C, D, B]\n" +
//                            "mov2x(E, A, south, west);//[C, D, B]\n" +
//                            "div(C, D, B, E);//[D, B, A]\n" +
//                            "movx(D, C, north);//[B, A]\n" +
//                            "mov2x(B, E, north, east);//[A]\n" +
//                            "movx(A, D, west);//[]\n" +
//                            "mov2x(C, C, south, south);//[]\n" +
//                            "add2x(C, C, D, north, east);//[]\n" +
//                            "add2x(D, D, C, north, north);//[]\n" +
//                            "movx(C, A, south);//[]\n" +
//                            "add2x(D, C, D, east, east);//[]\n" +
//                            "addx(C, C, A, south);//[]\n" +
//                            "mov2x(C, C, south, south);//[]\n" +
//                            "add2x(C, C, A, north, east);//[]\n" +
//                            "addx(D, A, D, south);//[]\n" +
//                            "mov2x(D, D, south, south);//[]\n" +
//                            "add2x(B, E, B, north, east);//[]\n" +
//                            "mov2x(B, B, north, north);//[]\n" +
//                            "add2x(E, F, E, west, north);//[]\n" +
//                            "add2x(C, F, C, north, north);//[]\n" +
//                            "add(B, F, B);//[]\n" +
//                            "add2x(B, F, B, east, south);//[]\n" +
//                            "addx(F, A, F, north);//[]\n" +
//                            "add2x(F, A, F, south, south);//[]\n" +
//                            "add2x(F, A, F, east, south);//[]\n" +
//                            "addx(A, A, E, east);//[]\n" +
//                            "add2x(D, E, D, north, north);//[]\n" +
//                            "movx(E, B, west);//[]\n" +
//                            "add2x(F, A, F, north, east);//[]\n" +
//                            "add2x(C, A, C, east, south);//[]\n" +
//                            "addx(A, F, A, east);//[]\n" +
//                            "add2x(A, A, C, west, west);//[F]\n" +
//                            "add(B, D, B, E);//[F]\n" +
//                            "add2x(C, C, A, east, east);//[D, E, F]\n" +
//                            "add(A, B, A, C);//[D, E, F]\n" +
//                            "//Null Instruction: [null] <- [A]//[B, C, D, E, F]\n");
            e.run("mov(B,A)");
            e.run("add(C,A,B)");
            e.run("add(D,A,B)");
            e.run("add(E,C,D)");

            e.run("add(C,A,B)");
            e.run("add(D,A,B)");
            e.run("add(F,C,D)");

            e.run("add(F,E,F)");

            e.run("add(C,A,B)");
            e.run("add(C,C,B)");
            e.run("add(C,C,B)");
            e.run("add(C,C,B)");
            e.run("add(C,C,B)");
            e.run("add(C,C,B)");
            e.run("add(C,C,B)");
            e.flushInstructionBuffer();
            System.out.println(e.tiles.get(new Pos(0,0)));
            System.out.println(e.getRegToString(0,0,"F"));
            System.out.println(e.readNoise(0,0,"F"));
            System.out.println(e.getRegToString(0,0,"C"));
            System.out.println(e.readNoise(0,0,"C"));


//            BufferedImage out1 = e.getImage(Reg.F, true);
//            ImageIO.write(out1, "png", new File("out1.png"));
//            BufferedImage out2 = e.getImage(Reg.D, true);
//            ImageIO.write(out2, "png", new File("out2.png"));
        } catch (IOException e1) {
            e1.printStackTrace();
        }


//
//        Scamp5Emulator e = new Scamp5Emulator(10,10);
//        System.out.println("input(A,128)");
//        e.run("input(A,128)");
//        e.run("mov2x(B, A, north, east)");
//        e.run("add2x(C, A, B, north, east)");
//        e.run("sub2x(F, A, north, east, B)");


    }
}
