package uk.co.edstow.cain.scamp5.emulator;

import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.util.Bounds;
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
        public final double writeNoiseConstant = 0.02;
        public final double writeNoiseFactor = 0.02;
        public final double readNoiseConstant = 0.002;
        public final double readNoiseFactor = 0.002;
        private final Random r = new Random(2001);
        double getValue(double noise){
            return r.nextGaussian() * noise;
        }
    }


    enum Reg {
        A(0, 0),B(0, 0),C(0, 0),D(0, 0),E(0, 0),F(0, 0),News(0, 0),
        XNorth(0, 1),XEast(1, 0),XSouth(0, -1),XWest(-1, 0);

        private final int realX;
        private final int realY;

        Reg(int realX, int realY) {
            this.realX = realX;
            this.realY = realY;
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
    public Scamp5Emulator(int rad) {
        this(-rad, rad+1, -rad, rad+1);
    }

    @SuppressWarnings("WeakerAccess")
    public Scamp5Emulator(int xMin, int xMax, int yMin, int yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        NoiseConfig noiseConfig = new NoiseConfig();

        this.tiles = new HashMap<>((xMax-xMin)*(yMax-yMin));
        Reg[] realRegs = Arrays.stream(Reg.values()).filter(Reg::real).toArray(Reg[]::new);
        for (int i = xMin; i < xMax; i++) {
            for (int j = yMin; j < yMax; j++) {
                Pos position = new Pos(i, j);
                ProcessingElement pe = new ProcessingElement(position, noiseConfig, realRegs);
                this.tiles.put(position, pe);
            }
        }

        for (int i = xMin; i < xMax; i++) {
            for (int j = yMin; j < yMax; j++) {
                ProcessingElement pe = this.tiles.get(new Pos(i, j));
                for (Reg reg : Reg.values()) {
                    if(!reg.real()) {
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
        }

        this.instructionPatten = Pattern.compile("([a-z0-9]*)\\(([^)]*)\\)\\s*");
        this.argPattern = Pattern.compile("([^\\s,]+)\\s*(?:,|$)");
        this.dirPattern = Pattern.compile("north|east|south|west");
        this.regPattern = Pattern.compile("[A-F]");


        instructionSet = new HashMap<>();
        instructionBuffer = new ArrayDeque<>();

        instructionSet.put(new InstructionSignature("input", new int[]{0,2}), args ->
                instructionBuffer.add(pe -> pe.input(Reg.valueOf(args[0]), Integer.parseInt(args[1])))
        );

        instructionSet.put(new InstructionSignature("res", new int[]{0}), args -> {
            checkRegsReal(args, 0);
            instructionBuffer.add(pe -> pe.bus(Reg.News));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.News));
        });
        instructionSet.put(new InstructionSignature("res", new int[]{0, 0}), args -> {
            checkRegsReal(args, 0,1);
            instructionBuffer.add(pe -> pe.bus(Reg.News));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.News));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[1]), Reg.News));
        });

        instructionSet.put(new InstructionSignature("mov", new int[]{0,0}), args -> {
            checkRegsReal(args, 0,1);
            instructionBuffer.add(pe -> pe.bus(Reg.News,Reg.valueOf(args[1])));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.News));
        });
        instructionSet.put(new InstructionSignature("add", new int[]{0,0,0}), args -> {
            checkRegsReal(args, 0,1,2);
            instructionBuffer.add(pe -> pe.bus(Reg.News,Reg.valueOf(args[1]),Reg.valueOf(args[2])));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.News));
        });
        instructionSet.put(new InstructionSignature("add", new int[]{0,0,0,0}), args -> {
            checkRegsReal(args, 0,1,2,3);
            instructionBuffer.add(pe -> pe.bus(Reg.News,Reg.valueOf(args[1]),Reg.valueOf(args[2]),Reg.valueOf(args[3])));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.News));
        });
        instructionSet.put(new InstructionSignature("sub", new int[]{0,0,0}), args -> {
            checkRegsReal(args, 0,1,2);
            instructionBuffer.add(pe -> pe.bus(Reg.News,Reg.valueOf(args[1])));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.News, Reg.valueOf(args[2])));
        });
        instructionSet.put(new InstructionSignature("neg", new int[]{0,0}), args -> {
            checkRegsReal(args, 0,1);
            instructionBuffer.add(pe -> pe.bus(Reg.News));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.News, Reg.valueOf(args[1])));
        });
        instructionSet.put(new InstructionSignature("abs", new int[]{0,0}), args -> {
            checkRegsReal(args, 0,1);
            instructionBuffer.add(pe -> pe.bus(Reg.News));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.News, Reg.valueOf(args[1])));
            instructionBuffer.add(pe -> pe.bus(Reg.News, Reg.valueOf(args[1])));
            instructionBuffer.add(pe -> pe.where(Reg.valueOf(args[1])));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.News));
            instructionBuffer.add(pe -> pe.all());
        });

        instructionSet.put(new InstructionSignature("divq", new int[]{0,0}), args -> {
            checkRegsReal(args, 0,1);
            instructionBuffer.add(pe -> pe.bus2(Reg.valueOf(args[0]), Reg.News, Reg.valueOf(args[1])));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.News));
        });
        instructionSet.put(new InstructionSignature("div", new int[]{0,0,0}), args -> {
            checkRegsReal(args, 0,1,2);
            instructionBuffer.add(pe -> pe.bus2(Reg.valueOf(args[0]), Reg.valueOf(args[1]), Reg.valueOf(args[2])));
            instructionBuffer.add(pe -> pe.bus(Reg.News, Reg.valueOf(args[2]), Reg.valueOf(args[1])));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[2]), Reg.News, Reg.valueOf(args[0])));
            instructionBuffer.add(pe -> pe.bus2(Reg.valueOf(args[0]), Reg.valueOf(args[1]), Reg.valueOf(args[2])));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.valueOf(args[1])));
        });
        instructionSet.put(new InstructionSignature("div", new int[]{0,0,0,0}), args -> {
            checkRegsReal(args, 0,1,2,3);
            instructionBuffer.add(pe -> pe.bus2(Reg.valueOf(args[0]), Reg.valueOf(args[1]), Reg.valueOf(args[3])));
            instructionBuffer.add(pe -> pe.bus(Reg.News, Reg.valueOf(args[3]), Reg.valueOf(args[1])));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[2]), Reg.News, Reg.valueOf(args[0])));
            instructionBuffer.add(pe -> pe.bus2(Reg.valueOf(args[0]), Reg.valueOf(args[1]), Reg.valueOf(args[2])));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.valueOf(args[1])));
        });
        instructionSet.put(new InstructionSignature("diva", new int[]{0,0,0}), args -> {
            checkRegsReal(args, 0,1,2);
            instructionBuffer.add(pe -> pe.bus2(Reg.valueOf(args[1]), Reg.valueOf(args[2]), Reg.valueOf(args[0])));
            instructionBuffer.add(pe -> pe.bus(Reg.News, Reg.valueOf(args[1]), Reg.valueOf(args[0])));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.News, Reg.valueOf(args[2])));
            instructionBuffer.add(pe -> pe.bus2(Reg.valueOf(args[1]), Reg.valueOf(args[2]), Reg.valueOf(args[0])));
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.valueOf(args[1])));
        });

        instructionSet.put(new InstructionSignature("movx", new int[]{0,0,1}), args -> {
            checkRegsReal(args, 0,1);
            switch (args[2]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.XWest,Reg.valueOf(args[1]))); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.XEast,Reg.valueOf(args[1]))); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.XSouth,Reg.valueOf(args[1]))); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.XNorth,Reg.valueOf(args[1]))); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.News));
        });

        instructionSet.put(new InstructionSignature("mov2x", new int[]{0,0,1,1}), args -> {
            checkRegsReal(args, 0,1);
            switch (args[2]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.XWest,Reg.valueOf(args[1]))); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.XEast,Reg.valueOf(args[1]))); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.XSouth,Reg.valueOf(args[1]))); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.XNorth,Reg.valueOf(args[1]))); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
            switch (args[3]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.XEast)); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.XWest)); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.XNorth)); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.XSouth)); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
        });

        instructionSet.put(new InstructionSignature("addx", new int[]{0,0,0,1}), args -> {
            checkRegsReal(args, 0,1,2);
            switch (args[3]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.XWest,Reg.valueOf(args[1]),Reg.valueOf(args[2]))); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.XEast,Reg.valueOf(args[1]),Reg.valueOf(args[2]))); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.XSouth,Reg.valueOf(args[1]),Reg.valueOf(args[2]))); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.XNorth,Reg.valueOf(args[1]),Reg.valueOf(args[2]))); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.News));
        });

        instructionSet.put(new InstructionSignature("add2x", new int[]{0,0,0,1,1}), args -> {
            checkRegsReal(args, 0,1,2);
            switch (args[3]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.XWest,Reg.valueOf(args[1]),Reg.valueOf(args[2]))); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.XEast,Reg.valueOf(args[1]),Reg.valueOf(args[2]))); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.XSouth,Reg.valueOf(args[1]),Reg.valueOf(args[2]))); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.XNorth,Reg.valueOf(args[1]),Reg.valueOf(args[2]))); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
            switch (args[4]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.XEast)); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.XWest)); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.XNorth)); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.XSouth)); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
        });

        instructionSet.put(new InstructionSignature("subx", new int[]{0,0,1,0}), args -> {
            checkRegsReal(args, 0,1,3);
            switch (args[2]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.XWest,Reg.valueOf(args[1]))); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.XEast,Reg.valueOf(args[1]))); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.XSouth,Reg.valueOf(args[1]))); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.XNorth,Reg.valueOf(args[1]))); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
            instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.News, Reg.valueOf(args[3])));
        });

        instructionSet.put(new InstructionSignature("sub2x", new int[]{0,0,1,1,0}), args -> {
            checkRegsReal(args, 0,1,4);
            switch (args[2]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.XWest,Reg.valueOf(args[1]))); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.XEast,Reg.valueOf(args[1]))); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.XSouth,Reg.valueOf(args[1]))); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.XNorth,Reg.valueOf(args[1]))); break;
                default: throw new UndefinedInstructionBehaviour("Unrecognised direction");
            }
            switch (args[3]){
                case "east": instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.XEast, Reg.valueOf(args[4]))); break;
                case "west": instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.XWest, Reg.valueOf(args[4]))); break;
                case "north": instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.XNorth, Reg.valueOf(args[4]))); break;
                case "south": instructionBuffer.add(pe -> pe.bus(Reg.valueOf(args[0]), Reg.XSouth, Reg.valueOf(args[4]))); break;
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
            if(!Reg.valueOf(args[idx]).real()){
                throw new UndefinedInstructionBehaviour("All registers in Macro Operations must be 'real'");
            }
        }
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
        Reg r = Reg.valueOf(reg);
        return pe.getRawRegisterContains(r);
    }

    public String getRegToString(int x, int y, String reg){
        ProcessingElement pe = this.tiles.get(new Pos(x, y));
        if (pe == null) {
            return null;
        }
        Reg r = Reg.valueOf(reg);
        return pe.getRegToString(r);
    }

    public Bounds getRegCoverge(int x, int y, String reg){
        ProcessingElement pe = this.tiles.get(new Pos(x, y));
        if (pe == null) {
            return null;
        }
        Reg r = Reg.valueOf(reg);
        return pe.getRegCoverage(r);
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
            Scamp5Emulator e = new Scamp5Emulator(1);

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
            e.runCode(
                    "movx(B, A, south);\n" +
                            "add(A, B, A);\n" +
                            "movx(B, A, north);\n" +
                            "add(B, A, B);\n" +
                            "movx(A, B, west);\n" +
                            "movx(B, B, east);\n" +
                            "sub(A, A, B);");
            e.flushInstructionBuffer();
            System.out.println(e.tiles.get(new Pos(0,0)));
            //
//            e.run("movx(B,F,north)");
//            e.run("add(F,F,B)");
//            e.run("movx(B,F,south)");
//            e.run("add(F,F,B)");
//            e.run("movx(B,F,east)");
//            e.run("movx(F,F,west)");
//            e.run("sub(F,F,B)");
//
//            e.run("divq(D,A)");
//            e.run("divq(C,D)");
//            e.run("movx(E,C,east)");
//            e.run("add(D,D,E)");
//            e.run("movx(E,C,west)");
//            e.run("add(D,D,E)");
//            e.run("divq(C,D)");
//            e.run("mov(D,C)");
//            e.run("divq(C,D)");
//            e.run("movx(E,C,north)");
//            e.run("add(D,D,E)");
//            e.run("movx(D,C,south)");
//            e.run("add(D,D,E)");
//
//            e.run("mov(A,D)");
//
//
//            e.run("divq(D,A)");
//            e.run("divq(C,D)");
//            e.run("movx(E,C,east)");
//            e.run("add(D,D,E)");
//            e.run("movx(E,C,west)");
//            e.run("add(D,D,E)");
//            e.run("divq(C,D)");
//            e.run("mov(D,C)");
//            e.run("divq(C,D)");
//            e.run("movx(E,C,north)");
//            e.run("add(D,D,E)");
//            e.run("movx(D,C,south)");
//            e.run("add(D,D,E)");


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
