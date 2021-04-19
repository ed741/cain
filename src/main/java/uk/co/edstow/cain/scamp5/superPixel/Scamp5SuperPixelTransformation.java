package uk.co.edstow.cain.scamp5.superPixel;

import uk.co.edstow.cain.transformations.BankedTransformation;
import uk.co.edstow.cain.goals.BankedKernel3DGoal;
import uk.co.edstow.cain.regAlloc.BRegister;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.atomGoal.Atom;
import uk.co.edstow.cain.goals.atomGoal.pairGen.SimpleTransformation;
import uk.co.edstow.cain.structures.Bounds;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;

public abstract class Scamp5SuperPixelTransformation<G extends BankedKernel3DGoal<G>> implements BankedTransformation {
    protected final Scamp5SuperPixelConfig config;

    protected Scamp5SuperPixelTransformation(Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
        this.config = scamp5SuperPixelConfig;
    }




    public static class Null<G extends BankedKernel3DGoal<G>> extends Scamp5SuperPixelTransformation<G> {
        private final int inputCount;
        private final int outputCount;

        public Null(int inputCount, int outputCount, Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);
            this.inputCount = inputCount;
            this.outputCount = outputCount;
        }


        @Override
        public int[] inputRegisterIntraInterference() {
            int[] out = new int[inputCount];
            for (int i = 0; i < out.length; i++) {
                out[i]=i;
            }
            return out;
        }

        @Override
        public boolean[] inputRegisterOutputInterference(int u){
            return new boolean[inputCount];
        }

        @Override
        public boolean clobbersInput(int i) {
            return false;
        }

        @Override
        public double cost() {
            return 0;
        }


        @Override
        public String code(List<BRegister> uppers, List<BRegister> lowers, List<BRegister> trash) {
            assert uppers.size() == outputCount;
            assert lowers.size() == inputCount;
            return this.config.outputFormatter.comment(String.format("Null Instruction: %s <- %s", uppers, lowers)) +
                    this.config.outputFormatter.newLine();
        }

        @Override
        public String toString() {
            return "Null_t";
        }

        @Override
        public int ExtraRegisterCount(int bank) {
            return 0;
        }
    }

    abstract static class SimpleScamp5SuperPixelTransformation<G extends BankedKernel3DGoal<G>> extends Scamp5SuperPixelTransformation<G> {

        SimpleScamp5SuperPixelTransformation(Scamp5SuperPixelConfig config) {
            super(config);
        }

        @Override
        public String code(List<BRegister> uppers, List<BRegister> lowers, List<BRegister> trash) {
            if (uppers.size() == 1) {
                return code(uppers.get(0), lowers) +
                        this.config.outputFormatter.newLine();
            } else {
                throw new IllegalArgumentException("This Transformation only accepts one Upper register");
            }
        }

        abstract String code(BRegister upper, List<BRegister> lowers);

        @Override
        public int ExtraRegisterCount(int bank) {
            return inputRegisterOutputInterferes()?1:0;
        }
        abstract boolean inputRegisterOutputInterferes();

        @Override
        public boolean[] inputRegisterOutputInterference(int u) {
            return this.inputRegisterOutputInterference();
        }
        abstract boolean[] inputRegisterOutputInterference();

        @Override
        public boolean clobbersInput(int i) {
            return false;
        }
    }

    public enum Dir{
        North(0,0,1), East(1,1,0), South(2,0,-1), West(3,-1,0);
        final int idx;
        final int x;
        final int y;
        Dir(int idx, int x, int y){
            this.idx = idx;
            this.x = x;
            this.y = y;
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

        public static Dir fromDirection(SimpleTransformation.Direction direction) {
            switch (direction){
                case N: return North;
                case E: return East;
                case S: return South;
                case W: return West;
                default: throw new IllegalArgumentException("Cannot produce dir for unknown direction: " + direction.toString());
            }
        }

        public <G extends BankedKernel3DGoal<G>> String code(Scamp5SuperPixelConfig config) {
            switch (this){
                case North: return config.northReg;
                case East: return config.eastReg;
                case South: return config.southReg;
                case West: return config.westReg;
                default: throw new IllegalArgumentException("Cannot produce register code for unknown direction: " + this.toString());
            }
        }
    }

    public static class Res<G extends BankedKernel3DGoal<G>> extends Scamp5SuperPixelTransformation<G> {
        // u := {}
        final List<G> results;

        public Res(List<G> results, Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);
            this.results = results;
        }

        @Override
        public String code(List<BRegister> uppers, List<BRegister> lowers, List<BRegister> trash) {
//            assert lowers.size() == inputCount();
            StringBuilder sb = new StringBuilder(String.format("/*SP Res(%s)*/ ", uppers));
            List<String> scratch = new ArrayList<>(config.scratchRegisters);
            scratch.add(config.northReg);
            scratch.add(config.eastReg);
            scratch.add(config.southReg);
            scratch.add(config.westReg);
            for (int bank = 0; bank < config.banks; bank++) {
                List<BRegister> regs = new ArrayList<>();
                for (BRegister reg: uppers)if(reg.bank==bank) regs.add(reg);
                if (!regs.isEmpty()){

                    config.selectBank(sb, bank, config.maskReg, scratch);
                    for (BRegister reg: regs){
                        String sreg = reg.name;
                        sb.append(config.outputFormatter.NOT(config.scratchRegisters.get(0), sreg));
                        sb.append(config.outputFormatter.NOR(sreg, config.scratchRegisters.get(0), config.maskReg));
                    }
                }
            }
            sb.append(config.outputFormatter.newLine());
            return sb.toString();
        }


        @Override
        public double cost() {
            return results.size()*3;
        }

        @Override
        public String toString() {
            return "SP Res/"+results.size();
        }

        private static final int[] inputRegisterIntraInterference = new int[0];
        private static final boolean[] inputRegisterOutputInterference = new boolean[0];



        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }

        @Override
        public boolean clobbersInput(int i) {
            return false;
        }

        @Override
        public boolean[] inputRegisterOutputInterference(int u) {
            return new boolean[0];
        }

        @Override
        public int ExtraRegisterCount(int bank) {
            return 0;
        }
    }

    public static class Negate<G extends BankedKernel3DGoal<G>> extends Scamp5SuperPixelTransformation<G> {
        // a := -neg
        final G neg; //upper
        final G a;   //lower
        protected Negate(G a, G neg, Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);
            this.neg = neg;
            this.a = a;
            assert a.getBank() == neg.getBank();
        }

        @Override
        public String toString() {
            return String.format("SP Neg(%s)", a);
        }

        @Override
        public boolean[] inputRegisterOutputInterference(int u) {
            return new boolean[1];
        }

        @Override
        public int ExtraRegisterCount(int bank) {
            return 0;
        }

        @Override
        public String code(List<BRegister> uppers, List<BRegister> lowers, List<BRegister> trash) {
            assert uppers.size() == 1;
            assert lowers.size() == 1;
            assert uppers.get(0).bank == lowers.get(0).bank;
            int bank = uppers.get(0).bank;
            StringBuilder sb = new StringBuilder(String.format("/*SP Neg(%s, %s)*/ ", uppers.get(0), lowers.get(0)));
            String outputReg = uppers.get(0).name;
            String inputAReg = lowers.get(0).name;

            for (int i = 1; i <= config.getBits(bank); i++) {
                sb.append(String.format("\n/* Bit %d */", i));
                // Copy in Carry in from correct PE
                config.setDirLessSignificantAndSelectBit(sb, bank, i, config.maskReg, Collections.emptyList());
                if (i == 1) { // If least significant bit set carry in directly based on if we're doing a sub or add
                    sb.append(config.outputFormatter.SET(config.maskedReg));
                } else {// for the other bits read in from less significant bit
                    sb.append(config.outputFormatter.DNEWS0(config.maskedReg, config.scratchRegisters.get(0)));
                }
                // maskedReg := Carry in bit

                // Use dir registers as scratch registers
                sb.append(config.outputFormatter.NOT(config.northReg, inputAReg));   // vn(!a) = !a
                sb.append(config.outputFormatter.NOT(config.eastReg, config.maskedReg));   // ve(!c_in) = !vM(c_in)
                sb.append(config.outputFormatter.NOR(config.scratchRegisters.get(0), inputAReg, config.eastReg)); // s0(c_out) = !(!!a + ve(!c_in))
                sb.append(config.outputFormatter.NOR(config.southReg, config.northReg, config.maskedReg)); // vs(!(!a+c_in)) = !(vn(!a) + vM(c_in))
                sb.append(config.outputFormatter.NOR(config.maskedReg, config.scratchRegisters.get(0), config.southReg)); // vM(sum) = !(s0(c_out) + vs(!(!a+c_in)))
                // scratch[0] := Carry out bit
                sb.append(config.outputFormatter.MOV(outputReg, config.maskedReg));

            }
            sb.append(config.outputFormatter.newLine());
            return sb.toString();
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return new int[1];
        }

        @Override
        public boolean clobbersInput(int i) {
            return false;
        }

        @Override
        public double cost() {
            return 8*config.getBits(a.getBank());
        }
    }

    public static abstract class AddSub_2<G extends BankedKernel3DGoal<G>> extends Scamp5SuperPixelTransformation<G> {
        // u := a + b

        final G a;
        final G b;


        public AddSub_2(G a, G b, Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);
            assert a.getBank() == b.getBank();
            this.a = a;
            this.b = b;
        }

        protected static String longAddCode(Scamp5SuperPixelConfig config, List<BRegister> uppers, List<BRegister> lowers) {
            assert uppers.size() == 1;
            assert lowers.size() == 2;
            assert uppers.get(0).bank == lowers.get(0).bank && uppers.get(0).bank == lowers.get(1).bank;
            int bank = uppers.get(0).bank;
            StringBuilder sb = new StringBuilder(String.format("/*SP add(%s, %s, %s)*/ ", uppers.get(0), lowers.get(0), lowers.get(1)));
            String outputReg = uppers.get(0).name;
            String inputAReg = lowers.get(0).name;
            String inputBReg = lowers.get(1).name;
            List<String> scratch = config.scratchRegisters;

            for (int i = 0; i < config.getBits(bank); i++) {
                sb.append(String.format("\n/* Bit %d */", i));
                // Copy in Carry in from correct PE
                config.setDirLessSignificant(sb, bank);
                // the least significant bit will have zeros for n,e,s,w so this should clear maskReg to 0 for that PE
                sb.append(config.outputFormatter.DNEWS0(config.maskReg, config.scratchRegisters.get(0)));
                // maskReg := Carry in bit

                // Use dir registers as scratch registers - we'll have to use setDirLessSignificant(sb, bank) on every
                // Iteration but this save 4 scratch registers!
                sb.append(config.outputFormatter.NOR(config.northReg, inputAReg, inputBReg));           //vn(1) = !(a+b)
                sb.append(config.outputFormatter.NOR(config.eastReg, inputAReg, config.northReg));      //ve(2) = !(a+vn(1))
                sb.append(config.outputFormatter.NOR(config.southReg, inputBReg, config.northReg));     //vs(3) = !(b+vn(1))
                sb.append(config.outputFormatter.NOR(config.westReg, config.eastReg, config.southReg)); //vw(4) = !(ve(2)+vs(3))
                sb.append(config.outputFormatter.NOR(config.eastReg, config.maskReg, config.westReg)); //ve(5) = !(vm(C)+vw(4))
                sb.append(config.outputFormatter.NOR(config.southReg, config.westReg, config.eastReg)); //vs(6) = !(vw(4)+ve(5))
                sb.append(config.outputFormatter.NOR(config.westReg, config.maskReg, config.eastReg)); //vw(7) = !(vm(C)+ve(5))
                sb.append(config.outputFormatter.NOR(scratch.get(0), config.eastReg, config.northReg)); //v0(C) = !(ve(5)+vn(1))
                // scratch[0] := Carry out bit

                // Multiplex to only store sum if we're in the correct bank.
                sb.append(config.outputFormatter.SET(config.maskReg));
                sb.append(config.outputFormatter.MOV(config.maskedReg, outputReg));
                config.selectBank(sb, uppers.get(0).bank, config.maskReg, Arrays.asList(config.northReg, config.eastReg));
                sb.append(config.outputFormatter.NOR(config.maskedReg, config.southReg, config.westReg)); //vm(S) = !(vs(6)+vw(7))
                sb.append(config.outputFormatter.MOV(outputReg, config.maskedReg));

            }
            sb.append(config.outputFormatter.newLine());
            return sb.toString();
        }

        protected static String shortCode(Scamp5SuperPixelConfig config, BRegister upper, BRegister lowerA, BRegister lowerB, boolean sub) {
            assert upper.bank == lowerA.bank && upper.bank == lowerB.bank;
            assert !upper.equals(lowerA);
            assert !upper.equals(lowerB);
            int bank = upper.bank;
            StringBuilder sb = new StringBuilder(String.format("/*SP %s(%s, %s, %s)*/ ", sub?"Sub":"Add", upper, lowerA, lowerB));
            String outputReg = upper.name;
            String inputAReg = lowerA.name;
            String inputBReg = lowerB.name;

            // Multiplex to only store half sum if we're in the correct bank.
            sb.append(config.outputFormatter.SET(config.maskReg));
            sb.append(config.outputFormatter.MOV(config.maskedReg, outputReg));
            config.selectBank(sb, bank, config.maskReg, Arrays.asList(config.northReg, config.eastReg, config.southReg, config.westReg));

            if (sub) { // invert b if we want to subtract
                sb.append(config.outputFormatter.NOT(config.westReg, inputBReg));
                inputBReg = config.westReg;
            }
            // add A and B into maskedReg
            sb.append(config.outputFormatter.NOR(config.northReg, inputAReg, inputBReg));           //vn(1) = !(a+b)
            sb.append(config.outputFormatter.NOR(config.eastReg, inputAReg, config.northReg));      //ve(2) = !(a+vn(1))
            sb.append(config.outputFormatter.NOR(config.southReg, inputBReg, config.northReg));     //vs(3) = !(b+vn(1))
            sb.append(config.outputFormatter.NOR(config.maskedReg, config.eastReg, config.southReg)); //vM(4) = !(ve(2)+vs(3))
            sb.append(config.outputFormatter.MOV(outputReg, config.maskedReg));                      //vO(4) = vM(4)
            // v(4) is in maskedReg

            for (int i = 1; i <= config.getBits(bank); i++) {
                sb.append(String.format("\n/* Bit %d */", i));
                // Copy in Carry in from correct PE
                config.setDirLessSignificantAndSelectBit(sb, bank, i, config.maskReg, Collections.emptyList());
                if (i == 1) { // If least significant bit set carry in directly based on if we're doing a sub or add
                    if (sub) {
                        sb.append(config.outputFormatter.SET(config.maskedReg));
                    } else {
                        sb.append(config.outputFormatter.CLR(config.maskedReg));
                    }
                } else {// for the other bits read in from less significant bit
                    sb.append(config.outputFormatter.DNEWS0(config.maskedReg, config.scratchRegisters.get(0)));
                }
                // maskedReg := Carry in bit

                // Use dir registers as scratch registers
                if (sub) { // invert b if we want to subtract
                    sb.append(config.outputFormatter.NOT(config.westReg, lowerB.name));   // vw(!b) = !b (for subtraction)
                }
                sb.append(config.outputFormatter.NOR(config.northReg, inputAReg, inputBReg));           //vn(1) = !(a+b)
                sb.append(config.outputFormatter.NOR(config.eastReg, config.maskedReg, outputReg)); //ve(5) = !(vM(C)+vO(4))
                sb.append(config.outputFormatter.NOR(config.southReg, outputReg, config.eastReg)); //vs(6) = !(vO(4)+ve(5))
                sb.append(config.outputFormatter.NOR(config.westReg, config.maskedReg, config.eastReg)); //vw(7) = !(vM(C)+ve(5))
                sb.append(config.outputFormatter.NOR(config.scratchRegisters.get(0), config.eastReg, config.northReg)); //v0(C) = !(ve(5)+vn(1))
                // scratch[0] := Carry out bit

                sb.append(config.outputFormatter.NOR(config.maskedReg, config.southReg, config.westReg)); //vM(S) = !(vs(6)+vw(7))
                sb.append(config.outputFormatter.MOV(outputReg, config.maskedReg));

            }
            sb.append(config.outputFormatter.newLine());
            return sb.toString();
        }

        private static final boolean[] inputRegisterOutputInterference = {true, true};
        private static final int[] inputRegisterIntraInterference = {0,0};

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


        @Override
        public int ExtraRegisterCount(int bank) {
            return 1;
        }
    }

    public static class Add_2<G extends BankedKernel3DGoal<G>> extends AddSub_2<G> {

        public Add_2(G a, G b, Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
            super(a, b, scamp5SuperPixelConfig);
        }

        @Override
        public String toString() {
            return String.format("SP Add2(%s, %s)", a, b);
        }

        @Override
        public String code(List<BRegister> uppers, List<BRegister> lowers, List<BRegister> trash) {
            return shortCode(config, uppers.get(0), lowers.get(0), lowers.get(1), false);
        }

        @Override
        public double cost() {
            return 8 + (9 * this.config.getBits(a.getBank()));
        }
    }


    public static class Sub_2<G extends BankedKernel3DGoal<G>> extends AddSub_2<G> {

        public Sub_2(G a, G b, Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
            super(a, b, scamp5SuperPixelConfig);
        }

        @Override
        public String toString() {
            return String.format("SP Sub2(%s, %s)", a, b);
        }

        @Override
        public String code(List<BRegister> uppers, List<BRegister> lowers, List<BRegister> trash) {
            return shortCode(config, uppers.get(0), lowers.get(0), lowers.get(1), true);
        }

        @Override
        public double cost() {
            return 9 + (10 * this.config.getBits(a.getBank()));
        }
    }

    public static class AddSelf<G extends BankedKernel3DGoal<G>> extends Scamp5SuperPixelTransformation<G> {
        // u := a + a

        final G a;
        G sum;


        public AddSelf(G a, Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);

            this.a = a;
            this.sum = null;
        }
        public AddSelf(G a, G sum, Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);

            this.a = a;
            this.sum = sum;
        }

        public AddSelf(G in, boolean upper, Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);

            if(!upper){
                this.a = in;
                this.sum = null;
            } else {
                BankedKernel3DGoal.BankedKernel3DGoalFactory<G> factory = in.newFactory();
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
        public String code(List<BRegister> uppers, List<BRegister> lowers, List<BRegister> trash) {
            assert uppers.size() == 1;
            assert lowers.size() == 1;
            assert uppers.get(0).bank == lowers.get(0).bank;
            int bank = uppers.get(0).bank;
            StringBuilder sb = new StringBuilder(String.format("/*SP addSelf(%s, %s)*/ ", uppers.get(0), lowers.get(0)));
            String outputReg = uppers.get(0).name;
            String inputReg = lowers.get(0).name;
            List<String> scratch = new ArrayList<>(config.scratchRegisters);
            scratch.add(config.northReg);
            scratch.add(config.eastReg);
            scratch.add(config.southReg);
            scratch.add(config.westReg);

            config.setDirLessSignificant(sb, bank);
            // the least significant bit will have zeros for n,e,s,w

            // Multiplex to only store sum if we're in the correct bank.
            sb.append(config.outputFormatter.SET(config.maskReg));
            sb.append(config.outputFormatter.MOV(config.maskedReg, outputReg));
            config.selectBank(sb, uppers.get(0).bank, config.maskReg, scratch);
            sb.append(config.outputFormatter.DNEWS0(config.maskedReg, inputReg));
            sb.append(config.outputFormatter.MOV(outputReg, config.maskedReg));

            sb.append(config.outputFormatter.newLine());
            return sb.toString();
        }

        @Override
        public double cost() {
            return 6;
        }

        @Override
        public String toString() {
            return String.format("SP AddSelf(%s)", a);
        }

        private static final boolean[] inputRegisterOutputInterference = {false};
        private static final int[] inputRegisterIntraInterference = {0};

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

        @Override
        public int ExtraRegisterCount(int bank) {
            return 0;
        }
    }

    public static class Div<G extends BankedKernel3DGoal<G>> extends Scamp5SuperPixelTransformation<G> {
        // u := a*0.5 + error

        final G a;
        G div;

        public Div(G a, Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);

            this.a = a;
            this.div = null;
        }

        public Div(G in, boolean upper, Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);

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
        public String code(List<BRegister> uppers, List<BRegister> lowers, List<BRegister> trash) {
            assert uppers.size() == 1;
            assert lowers.size() == 1;
            assert uppers.get(0).bank == lowers.get(0).bank;
            assert lowers.get(0).bank == this.a.getBank();
            assert this.div == null || uppers.get(0).bank == this.div.getBank();
            int bank = uppers.get(0).bank;
            StringBuilder sb = new StringBuilder(String.format("/*SP Div(%s, %s)*/ ", uppers.get(0), lowers.get(0)));
            String outputReg = uppers.get(0).name;
            String inputReg = lowers.get(0).name;
            List<String> scratch = config.scratchRegisters;

            // Copy in Carry in from correct PE
            config.setDirMoreSignificant(sb, bank);
            // the most significant bit will have zeros for n,e,s,w

            // Multiplex to only store sum if we're in the correct bank.
            //      always write (save) outputReg soo all PEs outside the bank aren't corrupted
            sb.append(config.outputFormatter.SET(config.maskReg));
            sb.append(config.outputFormatter.MOV(config.maskedReg, outputReg));
            //      write shifted result if this is in the selected Bank
            config.selectBank(sb, uppers.get(0).bank, config.maskReg, scratch);
            sb.append(config.outputFormatter.DNEWS0(config.maskedReg, inputReg));
            //      write un-shifted result if all dir bits are 0 (if this is Most significant bit)
            sb.append(config.outputFormatter.OR(scratch.get(0), config.northReg, config.eastReg, config.southReg, config.westReg));
            sb.append(config.outputFormatter.NOT(config.northReg, config.maskReg));
            sb.append(config.outputFormatter.NOR(config.maskReg, scratch.get(0), config.northReg));
            sb.append(config.outputFormatter.MOV(config.maskedReg, inputReg));
            //      write answer into outputReg
            sb.append(config.outputFormatter.MOV(outputReg, config.maskedReg));

            sb.append(config.outputFormatter.newLine());
            return sb.toString();
        }

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
            return config.banks;
        }

        @Override
        public String toString() {
            return String.format("SP Div(%s)", this.a);
        }

        private static final boolean[] inputRegisterOutputInterference = new boolean[]{false};
        private static final int[] inputRegisterIntraInterference = {0};

        @Override
        public boolean[] inputRegisterOutputInterference(int u) {
            return inputRegisterOutputInterference;
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }

        @Override
        public int ExtraRegisterCount(int bank) {
            return 0;
        }

        @Override
        public boolean clobbersInput(int i) {
            return false;
        }
    }

    public static class Movxb<G extends BankedKernel3DGoal<G>> extends Scamp5SuperPixelTransformation<G> {
        //u := a_dir

        final G upper;
        final G lower;
        final int xTranslation;
        final int yTranslation;
        final int xPETranslation;
        final int yPETranslation;
        final double cost;

        public Movxb(G upper, G lower, Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);
            this.upper = upper;
            this.lower = lower;
            assert this.upper.isTranslation(this.lower);
            assert scamp5SuperPixelConfig.isBankSameShape(this.upper.getBank(), this.lower.getBank());
            Bounds uB = this.upper.bounds();
            Bounds lB = this.lower.bounds();
            this.xTranslation = uB.getXMax() - lB.getXMax();
            this.yTranslation = uB.getYMax() - lB.getYMax();
            Tuple<Integer, Integer> peTranslation = config.getBankTranslation(lower.getBank(), upper.getBank());
            this.xPETranslation = this.xTranslation * scamp5SuperPixelConfig.width + peTranslation.getA();
            this.yPETranslation = this.yTranslation * scamp5SuperPixelConfig.height + peTranslation.getB();
            this.cost =
                    (this.xPETranslation!=0?2+this.xPETranslation:0) +
                    (this.yPETranslation!=0?2+this.yPETranslation:0) +
                    (this.xPETranslation==0&&this.yPETranslation==0?1:0)+
                    4;

        }
        public Movxb(G upper, int x, int y, int bank, Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
            this(upper ,upper.translated(x, y, 0).inBank(bank), scamp5SuperPixelConfig);
        }

        @Override
        public String code(List<BRegister> uppers, List<BRegister> lowers, List<BRegister> trash) {
            assert uppers.size() == 1;
            assert lowers.size() == 1;
            StringBuilder sb = new StringBuilder(String.format("/*SP movb(%s, %s)*/ ", uppers.get(0), lowers.get(0)));
            String output = uppers.get(0).name;
            String input = lowers.get(0).name;
            int ubank = uppers.get(0).bank;
            int lbank = lowers.get(0).bank;
            assert ubank == upper.getBank();
            assert lbank == lower.getBank();

            String scratchReg = config.scratchRegisters.get(0);
            String currentReg = input;

            if(xPETranslation != 0) {
                String dirReg = xPETranslation<0? config.eastReg : config.westReg;
                // set move direction
                sb.append(config.outputFormatter.CLR(config.northReg, config.eastReg, config.southReg, config.westReg));
                sb.append(config.outputFormatter.SET(dirReg, config.maskReg));//also set maskReg so we can use maskedReg
                for (int i = 0; i < Math.abs(xPETranslation); i++) {
                    if(i%2 == 0) { // ensure we never do a Mov where dst and src are the same!
                        sb.append(config.outputFormatter.DNEWS0(scratchReg, currentReg));
                        currentReg = scratchReg;
                    }else{
                        sb.append(config.outputFormatter.DNEWS0(config.maskedReg, currentReg));
                        currentReg = config.maskedReg;
                    }
                }
            }
            if(yPETranslation != 0) {
                String dirReg = yPETranslation<0? config.southReg : config.northReg;
                // set move direction
                sb.append(config.outputFormatter.CLR(config.northReg, config.eastReg, config.southReg, config.westReg));
                sb.append(config.outputFormatter.SET(dirReg, config.maskReg));//also set maskReg so we can use maskedReg
                for (int i = 0; i < Math.abs(yPETranslation); i++) {
                    if(i%2 == 0) { // ensure we never do a Mov where dst and src are the same!
                        sb.append(config.outputFormatter.DNEWS0(scratchReg, currentReg));
                        currentReg = scratchReg;
                    }else{
                        sb.append(config.outputFormatter.DNEWS0(config.maskedReg, currentReg));
                        currentReg = config.maskedReg;
                    }
                }
            }
            if (xPETranslation == 0 && yPETranslation == 0){ // catch case when translation is (0,0)
                sb.append(config.outputFormatter.SET(config.maskReg));// set maskReg so we can use maskedReg
            }
            List<String> scratch = new ArrayList<>(config.scratchRegisters);
            scratch.add(config.northReg);
            scratch.add(config.eastReg);
            scratch.add(config.southReg);
            scratch.add(config.westReg);
            if(currentReg == config.maskedReg){
                // the moved result is in maskedReg
                config.selectBank(sb, ubank, config.selectReg, scratch); // invert selectBank into maskReg
                sb.append(config.outputFormatter.NOT(config.maskReg, config.selectReg));
                sb.append(config.outputFormatter.MOV(config.maskedReg, output)); // copy original output value if this PE is Not in bank
            } else {
                // the moved result is in currentReg (and currentReg isn't maskedReg)
                sb.append(config.outputFormatter.MOV(config.maskedReg, output)); // copy original output value
                config.selectBank(sb, ubank, config.maskReg, scratch); // selectBank into maskReg
                sb.append(config.outputFormatter.MOV(config.maskedReg, currentReg)); // copy moved value if PE is in Bank
            }

            sb.append(config.outputFormatter.MOV(output, config.maskedReg)); // copy result into outputReg
            sb.append(config.outputFormatter.newLine());
            return sb.toString();
        }

        private static final boolean[] inputRegisterOutputInterference = new boolean[]{false};
        private static final int[] inputRegisterIntraInterference = {0};

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

        @Override
        public double cost() {
            return cost;
        }

        @Override
        public String toString() {
            return String.format("SP Movxb (%d, %d) (%d -> %d) => (%s)", xTranslation, yTranslation, lower.getBank(), upper.getBank(), upper.toString());
        }

        @Override
        public int ExtraRegisterCount(int bank) {
            return 0;
        }
    }

}
