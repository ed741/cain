package uk.co.edstow.cain.scamp5.digital;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.transformations.StandardTransformation;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.atomGoal.Atom;
import uk.co.edstow.cain.goals.atomGoal.pairGen.SimpleTransformation;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;

public abstract class Scamp5DigitalTransformation<G extends Kernel3DGoal<G>> implements StandardTransformation {
    protected final Scamp5DigitalConfig config;

    protected Scamp5DigitalTransformation(Scamp5DigitalConfig scamp5DigitalConfig) {
        this.config = scamp5DigitalConfig;
    }

    public abstract List<G> applyOpForwards() throws TransformationApplicationException;



    public static class Null<G extends Kernel3DGoal<G>> extends Scamp5DigitalTransformation<G> {
        private final int inputCount;
        private final int outputCount;

        public Null(int inputCount, int outputCount, Scamp5DigitalConfig scamp5DigitalConfig) {
            super(scamp5DigitalConfig);
            this.inputCount = inputCount;
            this.outputCount = outputCount;
        }

        @Override
        public boolean[] inputRegisterOutputInterference(int u){
            return new boolean[inputCount()];
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            int[] out = new int[inputCount()];
            for (int i = 0; i < out.length; i++) {
                out[i]=i;
            }
            return out;
        }

        @Override
        public boolean clobbersInput(int i) {
            return false;
        }


        @Override
        public String code(List<Register> uppers, List<Register> lowers, List<Register> trash) {
            return String.format("//Null Instruction: %s <- %s", uppers, lowers);
        }

        @Override
        public int inputCount() {
            return inputCount;
        }

        @Override
        public int outputCount() {
            return outputCount;
        }

        @Override
        public double cost() {
            return 0;
        }

        @Override
        public String toStringN() {
            return "Null_t";
        }

        @Override
        public String toString() {
            return "Null_t";
        }

        @Override
        public List<G> applyOpForwards() {
            return Collections.emptyList();
        }
    }

    abstract static class SimpleScamp5DigitalTransformation<G extends Kernel3DGoal<G>> extends Scamp5DigitalTransformation<G> {

        SimpleScamp5DigitalTransformation(Scamp5DigitalConfig config) {
            super(config);
        }

        @Override
        public String code(List<Register> uppers, List<Register> lowers, List<Register> trash) {
            if (uppers.size() == 1) {
                return code(uppers.get(0), lowers);
            } else {
                throw new IllegalArgumentException("This Transformation only accepts one Upper register");
            }
        }

        abstract String code(Register upper, List<Register> lowers);

        public abstract G applyForwards() throws TransformationApplicationException;
        public List<G> applyOpForwards() throws TransformationApplicationException{
            return Collections.singletonList(applyForwards());
        }

        @Override
        public boolean[] inputRegisterOutputInterference(int u) {
            return this.inputRegisterOutputInterference();
        }

        abstract boolean[] inputRegisterOutputInterference();

        @Override
        public int ExtraRegisterCount() {
            return inputRegisterOutputInterferes()?1:0;
        }
        abstract boolean inputRegisterOutputInterferes();

        @Override
        public String toString() {
            return toStringN();
        }

        @Override
        public int outputCount() {
            return 1;
        }

        @Override
        public boolean clobbersInput(int i) {
            return false;
        }
    }

    public enum Dir{
        North(0,0,1, "R3"), East(1,1,0, "R4"), South(2,0,-1, "R1"), West(3,-1,0, "R2");
        final int idx;
        final int x;
        final int y;
        private final String code;
        Dir(int idx, int x, int y, String code){
            this.idx = idx;
            this.x = x;
            this.y = y;
            this.code = code;
        }

        public Dir opposite() {
            return Dir.values()[(idx+2)%4];
        }

        public Dir cw() {
            return Dir.values()[(idx+1)%4];
        }

        public Dir ccw() {
            return Dir.values()[(idx+3)%4];
        }

        @SuppressWarnings("WeakerAccess")
        public String toCode() {
            return code;
        }

        public static Dir fromDirection(SimpleTransformation.Direction direction) {
            switch (direction){
                case N: return North;
                case E: return East;
                case S: return South;
                case W: return West;
                default: return null;
            }
        }
    }

    public static class Res<G extends Kernel3DGoal<G>> extends SimpleScamp5DigitalTransformation<G> {
        // u := {}
        final G result;

        public Res(G result, Scamp5DigitalConfig scamp5DigitalConfig) {
            super(scamp5DigitalConfig);
            this.result = result;
        }

        @Override
        public String code(Register upper, List<Register> lowers) {
            assert lowers.size() == inputCount();
            List<String> regs = config.registerMapping.get(upper);
            assert regs.size() == config.bits;
            StringBuilder sb = new StringBuilder(String.format("/*Dres(%s)*/", upper));
            int i = config.bits;
            for(; i>=4; i -=4){
                sb.append(config.outputFormatter.CLR(regs.get(i - 1), regs.get(i - 2), regs.get(i - 3), regs.get(i - 4)));
            }
            if(i == 3) {
                sb.append(config.outputFormatter.CLR(regs.get(i - 1), regs.get(i - 2), regs.get(i - 3)));
            }else if (i == 2) {
                sb.append(config.outputFormatter.CLR(regs.get(i - 1), regs.get(i - 2)));
            }else if (i == 1) {
                sb.append(config.outputFormatter.CLR(regs.get(i - 1)));
            }
            return sb.toString();
        }

        @Override
        public int inputCount() {
            return 0;
        }

        @Override
        public G applyForwards() {
            return result;
        }

        @Override
        public double cost() {
            return 1+ ((config.bits-1)/4);
        }

        @Override
        public String toStringN() {
            return "DRes/1";
        }

        private static final boolean[] inputRegisterOutputInterference = new boolean[0];
        private static final int[] inputRegisterIntraInterference = new int[0];

        @Override
        public boolean[] inputRegisterOutputInterference() {
            return inputRegisterOutputInterference;
        }

        @Override
        public boolean inputRegisterOutputInterferes() {
            return false;
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }
    }


    public static class Res_2 <G extends Kernel3DGoal<G>>extends Scamp5DigitalTransformation<G> {
        // u := {}
        final G result1;
        final G result2;

        public Res_2(G a, G b, Scamp5DigitalConfig scamp5DigitalConfig) {
            super(scamp5DigitalConfig);
            this.result1 = a;
            this.result2 = b;
        }

        @Override
        public String code(List<Register> upper, List<Register> lowers, List<Register> trash) {
            assert lowers.size() == inputCount();
            List<String> regs = new ArrayList<>(config.bits*2);
            regs.addAll(config.registerMapping.get(upper.get(0)));
            regs.addAll(config.registerMapping.get(upper.get(1)));
            assert regs.size() == config.bits;
            StringBuilder sb = new StringBuilder(String.format("/*Dres2(%s, %s)*/", upper.get(0), upper.get(1)));
            int i = config.bits;
            for(; i>=4; i -=4){
                sb.append(config.outputFormatter.CLR(regs.get(i - 1), regs.get(i - 2), regs.get(i - 3), regs.get(i - 4)));
            }
            if(i == 3) {
                sb.append(config.outputFormatter.CLR(regs.get(i - 1), regs.get(i - 2), regs.get(i - 3)));
            }else if (i == 2) {
                sb.append(config.outputFormatter.CLR(regs.get(i - 1), regs.get(i - 2)));
            }else if (i == 1) {
                sb.append(config.outputFormatter.CLR(regs.get(i - 1)));
            }
            return sb.toString();
        }

        @Override
        public int inputCount() {
            return 0;
        }

        @Override
        public int outputCount() {
            return 2;
        }

        @Override
        public List<G> applyOpForwards() {
            return Arrays.asList(result1, result2);
        }

        @Override
        public double cost() {
            return 1+ ((config.bits*2-1)/4);
        }

        @Override
        public String toStringN() {
            return "DRes/2";
        }
        @Override
        public String toString() {
            return toStringN();
        }

        private static final boolean[] inputRegisterOutputInterference = new boolean[0];
        private static final int[] inputRegisterIntraInterference = new int[0];

        @Override
        public boolean[] inputRegisterOutputInterference(int u) {
            return inputRegisterOutputInterference;
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }

        @Override
        public boolean clobbersInput(int i) {
            return false;
        }
    }


    public static class Mov<G extends Kernel3DGoal<G>> extends SimpleScamp5DigitalTransformation<G> {
        //u := a

        final G a;
        G moved = null;

        @SuppressWarnings("WeakerAccess")
        public Mov(G a, Scamp5DigitalConfig scamp5DigitalConfig) {
            super(scamp5DigitalConfig);
            this.a = a;
        }

        @SuppressWarnings("WeakerAccess")
        public Mov(G in, boolean upper, Scamp5DigitalConfig scamp5DigitalConfig) {
            super(scamp5DigitalConfig);
            if (upper) {
                this.a = in.copy();
                this.moved = in;
            } else {
                this.a = in;
            }
        }

        @Override
        public String code(Register upper, List<Register> lowers) {
            assert lowers.size() == inputCount();
            StringBuilder sb = new StringBuilder(String.format("/*Dmov(%s, %s)*/", upper, lowers.get(0)));
            List<String> outputs = config.registerMapping.get(upper);
            List<String> inputs = config.registerMapping.get(lowers.get(0));
            for(int i = 0; i<config.bits; i++){
                sb.append(config.outputFormatter.MOV(outputs.get(i), inputs.get(i)));
            }
            return sb.toString();
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public G applyForwards() {
            if(this.moved == null){
                this.moved = a.copy();
            }
            return this.moved;
        }

        @Override
        public double cost() {
            return config.bits;
        }

        @Override
        public String toStringN() {
            return String.format("DMov (%s)", this.a);
        }

        private static final boolean[] inputRegisterOutputInterference = new boolean[1];
        private static final int[] inputRegisterIntraInterference = new int[1];

        @Override
        public boolean[] inputRegisterOutputInterference() {
            return inputRegisterOutputInterference;
        }

        @Override
        public boolean inputRegisterOutputInterferes() {
            return false;
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }
    }


    public static class Add_2<G extends Kernel3DGoal<G>> extends Scamp5DigitalTransformation<G> {
        // u := a + b

        final G a;
        final G b;
        G sum;


        public Add_2(G a, G b, Scamp5DigitalConfig scamp5DigitalConfig) {
            super(scamp5DigitalConfig);

            this.a = a;
            this.b = b;
            this.sum = null;
        }

        @Override
        public String code(List<Register> uppers, List<Register> lowers, List<Register> trash) {
            assert lowers.size() == inputCount();
            assert lowers.size() == inputCount();
            StringBuilder sb = new StringBuilder(String.format("/*Dadd(%s, %s, %s)*/\n", uppers.get(0), lowers.get(0), lowers.get(1)));
            List<String> outputs = config.registerMapping.get(uppers.get(0));
            List<String> inputAs = config.registerMapping.get(lowers.get(0));
            List<String> inputBs = config.registerMapping.get(lowers.get(1));
            List<String> scratch = config.scratchRegisters;
            // scratch[0] := Carry bit
            sb.append(config.outputFormatter.CLR(scratch.get(0)));
            sb.append("\n");
            for (int i = 0; i < config.bits; i++) {
                sb.append(String.format("/* Bit %d */\n", i));
                sb.append(config.outputFormatter.NOR(outputs.get(i), inputAs.get(i), inputBs.get(i))); //vs(1) = !(a+b)
                sb.append(config.outputFormatter.NOR(scratch.get(2), inputAs.get(i), outputs.get(i))); //v2(2) = !(a+vs(1))
                sb.append(config.outputFormatter.NOR(scratch.get(3), inputBs.get(i), outputs.get(i))); //v3(3) = !(b+vs(1))
                sb.append(config.outputFormatter.NOR(scratch.get(1), scratch.get(2), scratch.get(3))); //v1(4) = !(v2(2)+v3(3))
                sb.append(config.outputFormatter.NOR(scratch.get(2), scratch.get(0), scratch.get(1))); //v2(5) = !(v0(C)+v1(4))
                sb.append(config.outputFormatter.NOR(scratch.get(3), scratch.get(1), scratch.get(2))); //v3(6) = !(v1(4)+v2(5))
                sb.append(config.outputFormatter.NOR(scratch.get(1), scratch.get(0), scratch.get(2))); //v1(7) = !(v0(C)+v2(5))
                sb.append(config.outputFormatter.NOR(scratch.get(0), scratch.get(2), outputs.get(i))); //v0(C) = !(v2(5)+vs(1))
                sb.append(config.outputFormatter.NOR(outputs.get(i), scratch.get(3), scratch.get(1))); //vs(S) = !(v3(6)+v1(7))
                sb.append("\n");
            }
            return sb.toString();
        }

        @Override
        public int inputCount() {
            return 2;
        }

        @Override
        public int outputCount() {
            return 1;
        }

        @Override
        public boolean[] inputRegisterOutputInterference(int u) {
            return inputRegisterOutputInterference;
        }

        @Override
        public double cost() {
            return 1 + (9 * this.config.bits);
        }

        @Override
        public String toStringN() {
            return String.format("DAdd2(%s, %s)", a, b);
        }

        @Override
        public String toString() {
            return toStringN();
        }

        private static final boolean[] inputRegisterOutputInterference = {false, false};
        private static final int[] inputRegisterIntraInterference = {0,0};

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }

        @Override
        public boolean clobbersInput(int i) {
            return false;
        }

        @Override
        public List<G> applyOpForwards() throws TransformationApplicationException {
            if (this.sum == null){
                this.sum = a.added(a);
            }
            return Collections.singletonList(this.sum);
        }
    }

    public static class AddSelf<G extends Kernel3DGoal<G>> extends Scamp5DigitalTransformation<G> {
        // u := a + a

        final G a;
        G sum;


        public AddSelf(G a, Scamp5DigitalConfig scamp5DigitalConfig) {
            super(scamp5DigitalConfig);

            this.a = a;
            this.sum = null;
        }
        public AddSelf(G a, G sum, Scamp5DigitalConfig scamp5DigitalConfig) {
            super(scamp5DigitalConfig);

            this.a = a;
            this.sum = sum;
        }

        public AddSelf(G in, boolean upper, Scamp5DigitalConfig scamp5DigitalConfig) {
            super(scamp5DigitalConfig);

            if(!upper){
                this.a = in;
                this.sum = null;
            } else {
                Kernel3DGoal.Kernel3DGoalFactory<G> factory = in.newFactory();
                Iterator<Tuple<Atom, Integer>> it = in.uniqueCountIterator();
                while(it.hasNext()){
                    Tuple<Atom, Integer> t = it.next();
                    int count = t.getB();
                    if(count < 2 || count % 2 != 0){
                        this.a = null;
                        this.sum = in;
                        return;
                    }
                    factory.add(t.getA().x, t.getA().y, t.getA().z, t.getA().positive?count/2:(-count/2));
                }
                this.a = factory.get();
                this.sum = in;
            }

        }

        public boolean isPossible(){
            return a != null;
        }


        @Override
        public String code(List<Register> uppers, List<Register> lowers, List<Register> trash) {
            assert lowers.size() == inputCount();
            assert lowers.size() == inputCount();
            StringBuilder sb = new StringBuilder(String.format("/*DaddSelf(%s, %s)*/\n", uppers.get(0), lowers.get(0)));
            List<String> outputs = config.registerMapping.get(uppers.get(0));
            List<String> inputAs = config.registerMapping.get(lowers.get(0));
            for (int i = config.bits - 1; i > 0; i--) {
                sb.append(config.outputFormatter.MOV(outputs.get(i), inputAs.get(i - 1)));
            }
            sb.append(config.outputFormatter.CLR(outputs.get(0)));
            sb.append("\n");
            return sb.toString();
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public int outputCount() {
            return 1;
        }

        @Override
        public boolean[] inputRegisterOutputInterference(int u) {
            return inputRegisterOutputInterference;
        }

        @Override
        public double cost() {
            return this.config.bits;
        }

        @Override
        public String toStringN() {
            return String.format("DAddSelf(%s)", a);
        }

        @Override
        public String toString() {
            return this.toStringN();
        }

        private static final boolean[] inputRegisterOutputInterference = {false};
        private static final int[] inputRegisterIntraInterference = {0};

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }

        @Override
        public boolean clobbersInput(int i) {
            return false;
        }

        @Override
        public List<G> applyOpForwards() throws TransformationApplicationException {
            if (this.sum == null){
                this.sum = a.added(a);
            }
            return Collections.singletonList(this.sum);
        }
    }

    public static class Div<G extends Kernel3DGoal<G>> extends SimpleScamp5DigitalTransformation<G> {
        // u := a*0.5 + error

        final G a;
        G div;

        public Div(G a, Scamp5DigitalConfig scamp5DigitalConfig) {
            super(scamp5DigitalConfig);

            this.a = a;
            this.div = null;
        }

        public Div(G in, boolean upper, Scamp5DigitalConfig scamp5DigitalConfig) {
            super(scamp5DigitalConfig);

            if(!upper){
                this.a = in;
                this.div = null;
            } else {
                this.a = in.added(in);
                this.div = in;
            }
        }

        public boolean isPossible(){
            Iterator<Tuple<Atom, Integer>> it = a.uniqueCountIterator();
            while(it.hasNext()){
                Tuple<Atom, Integer> t = it.next();
                int count = t.getB();
                if(count < 2 || count % 2 != 0){
                    return false;
                }
            }
            return true;
        }

        @Override
        public String code(Register upper, List<Register> lowers) {
            assert lowers.size() == inputCount();
            assert lowers.size() == inputCount();
            StringBuilder sb = new StringBuilder(String.format("/*DDiv(%s, %s)*/\n", upper, lowers.get(0)));
            List<String> outputs = config.registerMapping.get(upper);
            List<String> inputAs = config.registerMapping.get(lowers.get(0));
            for (int i = 0; i < config.bits - 1; i++) {
                sb.append(config.outputFormatter.MOV(outputs.get(i), inputAs.get(i + 1)));
            }
            sb.append(config.outputFormatter.CLR(outputs.get(config.bits - 1)));
            sb.append("\n");
            return sb.toString();
        }

        @Override
        public int inputCount() {
            return 1;
        }


        @Override
        public G applyForwards() throws TransformationApplicationException {
            if(this.div == null){
                Kernel3DGoal.Kernel3DGoalFactory<G> factory = a.newFactory();
                Iterator<Tuple<Atom, Integer>> it = a.uniqueCountIterator();
                while(it.hasNext()){
                    Tuple<Atom, Integer> t = it.next();
                    int count = t.getB();
                    if(count < 2 || count % 2 != 0){
                        throw new TransformationApplicationException("Cannot divide uneven number of atoms!");
                    }
                    factory.add(t.getA().x, t.getA().y, t.getA().z, t.getA().positive?count/2:(-count/2));
                }
                this.div = factory.get();
            }
            return this.div;
        }

        @Override
        public double cost() {
            return config.bits;
        }

        @Override
        public String toStringN() {
            return String.format("DDiv(%s)", this.a);
        }

        private static final boolean[] inputRegisterOutputInterference = new boolean[]{false};
        private static final int[] inputRegisterIntraInterference = {0};

        @Override
        public boolean[] inputRegisterOutputInterference() {
            return inputRegisterOutputInterference;
        }

        @Override
        public boolean inputRegisterOutputInterferes() {
            return true;
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }
    }


    public static class Movx<G extends Kernel3DGoal<G>> extends Mov<G> {
        //u := a_dir

        final Dir dir;

        public Movx(G a, Dir dir, Scamp5DigitalConfig scamp5DigitalConfig) {
            super(a, scamp5DigitalConfig);
            this.dir = dir;
        }

        public Movx(G in, Dir dir, boolean upper, Scamp5DigitalConfig scamp5DigitalConfig) {

            super(upper?in.translated(-dir.x, -dir.y, 0):in, scamp5DigitalConfig);
            this.moved = upper?in:null;
            this.dir = dir;
        }

        @Override
        public String code(Register upper, List<Register> lowers) {
            assert lowers.size() == inputCount();
            StringBuilder sb = new StringBuilder(String.format("/*Dmovx(%s, %s, %s)*/", upper, lowers.get(0), dir.toString()));
            List<String> outputs = config.registerMapping.get(upper);
            List<String> inputs = config.registerMapping.get(lowers.get(0));
            sb.append(config.outputFormatter.CLR(Dir.values()[0].code, Dir.values()[1].code, Dir.values()[2].code, Dir.values()[3].code));
            sb.append(config.outputFormatter.SET(dir.code));
            for (int i = 0; i < config.bits; i++) {
                sb.append(config.outputFormatter.DNEWS0(outputs.get(i), inputs.get(i)));
            }
            return sb.toString();
        }

        @Override
        public G applyForwards() {
            if(this.moved == null){
                this.moved = a.translated(dir.x, dir.y, 0);
            }
            return this.moved;
        }

        @Override
        public double cost() {
            return config.bits+2;
        }

        @Override
        public String toStringN() {
            return String.format("DMovX %s (%s)", dir, this.a);
        }
    }

}
