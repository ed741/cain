package uk.co.edstow.cain.scamp5.digital;

import uk.co.edstow.cain.RegisterAllocator;
import uk.co.edstow.cain.Transformation;
import uk.co.edstow.cain.atomGoal.Atom;
import uk.co.edstow.cain.atomGoal.AtomGoal;
import uk.co.edstow.cain.atomGoal.pairGen.SimpleTransformation;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;

public abstract class Scamp5DigitalTransformation extends Transformation {
    protected final Scamp5DigitalConfig<AtomGoal> config;

    protected Scamp5DigitalTransformation(Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig) {
        this.config = scamp5DigitalConfig;
    }

    public abstract List<AtomGoal> applyOpForwards() throws TransformationApplicationException;


    abstract static class SimpleScamp5DigitalTransformation extends Scamp5DigitalTransformation {

        SimpleScamp5DigitalTransformation(Scamp5DigitalConfig<AtomGoal> config) {
            super(config);
        }

        @Override
        public String code(List<RegisterAllocator.Register> uppers, List<RegisterAllocator.Register> lowers, List<RegisterAllocator.Register> trash) {
            if (uppers.size() == 1) {
                return code(uppers.get(0), lowers);
            } else {
                throw new IllegalArgumentException("This Transformation only accepts one Upper register");
            }
        }

        abstract String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers);

        public abstract AtomGoal applyForwards() throws TransformationApplicationException;
        public List<AtomGoal> applyOpForwards() throws TransformationApplicationException{
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

    public static class Res extends SimpleScamp5DigitalTransformation {
        // u := {}
        final AtomGoal result;

        public Res(AtomGoal result, Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig) {
            super(scamp5DigitalConfig);
            this.result = result;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            List<String> regs = config.registerMapping.get(upper);
            assert regs.size() == config.bits;
            StringBuilder sb = new StringBuilder(String.format("/*Dres(%s)*/", upper));
            int i = config.bits;
            for(; i>=4; i -=4){
                sb.append(String.format("CLR(%s, %s, %s, %s); ", regs.get(i-1), regs.get(i-2), regs.get(i-3), regs.get(i-4)));
            }
            if(i == 3) {
                sb.append(String.format("CLR(%s, %s, %s); ", regs.get(i-1), regs.get(i-2), regs.get(i-3)));
            }else if (i == 2) {
                sb.append(String.format("CLR(%s, %s); ", regs.get(i-1), regs.get(i-2)));
            }else if (i == 1) {
                sb.append(String.format("CLR(%s); ", regs.get(i-1)));
            }
            return sb.toString();
        }

        @Override
        public int inputCount() {
            return 0;
        }

        @Override
        public AtomGoal applyForwards() {
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


    public static class Res_2 extends Scamp5DigitalTransformation {
        // u := {}
        final AtomGoal result1;
        final AtomGoal result2;

        public Res_2(AtomGoal a, AtomGoal b, Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig) {
            super(scamp5DigitalConfig);
            this.result1 = a;
            this.result2 = b;
        }

        @Override
        public String code(List<RegisterAllocator.Register> upper, List<RegisterAllocator.Register> lowers, List<RegisterAllocator.Register> trash) {
            assert lowers.size() == inputCount();
            List<String> regs = new ArrayList<>(config.bits*2);
            regs.addAll(config.registerMapping.get(upper.get(0)));
            regs.addAll(config.registerMapping.get(upper.get(1)));
            assert regs.size() == config.bits;
            StringBuilder sb = new StringBuilder(String.format("/*Dres2(%s, %s)*/", upper.get(0), upper.get(1)));
            int i = config.bits;
            for(; i>=4; i -=4){
                sb.append(String.format("CLR(%s, %s, %s, %s); ", regs.get(i-1), regs.get(i-2), regs.get(i-3), regs.get(i-4)));
            }
            if(i == 3) {
                sb.append(String.format("CLR(%s, %s, %s); ", regs.get(i-1), regs.get(i-2), regs.get(i-3)));
            }else if (i == 2) {
                sb.append(String.format("CLR(%s, %s); ", regs.get(i-1), regs.get(i-2)));
            }else if (i == 1) {
                sb.append(String.format("CLR(%s); ", regs.get(i-1)));
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
        public List<AtomGoal> applyOpForwards() {
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


    public static class Mov extends SimpleScamp5DigitalTransformation {
        //u := a

        final AtomGoal a;
        AtomGoal moved = null;

        @SuppressWarnings("WeakerAccess")
        public Mov(AtomGoal a, Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig) {
            super(scamp5DigitalConfig);
            this.a = a;
        }

        @SuppressWarnings("WeakerAccess")
        public Mov(AtomGoal in, boolean upper, Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig) {
            super(scamp5DigitalConfig);
            if (upper) {
                this.a = new AtomGoal(in);
                this.moved = in;
            } else {
                this.a = in;
            }
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            StringBuilder sb = new StringBuilder(String.format("/*Dmov(%s, %s)*/", upper, lowers.get(0)));
            List<String> outputs = config.registerMapping.get(upper);
            List<String> inputs = config.registerMapping.get(lowers.get(0));
            for(int i = 0; i<config.bits; i++){
                sb.append(String.format("MOV(%s, %s); ", outputs.get(i), inputs.get(i)));
            }
            return sb.toString();
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public AtomGoal applyForwards() {
            if(this.moved == null){
                this.moved = new AtomGoal(a);
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


    public static class Add_2 extends Scamp5DigitalTransformation {
        // u := a + b

        final AtomGoal a;
        final AtomGoal b;
        AtomGoal sum;


        public Add_2(AtomGoal a, AtomGoal b, Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig) {
            super(scamp5DigitalConfig);

            this.a = a;
            this.b = b;
            this.sum = null;
        }

        @Override
        public String code(List<RegisterAllocator.Register> uppers, List<RegisterAllocator.Register> lowers, List<RegisterAllocator.Register> trash) {
            assert lowers.size() == inputCount();
            assert lowers.size() == inputCount();
            StringBuilder sb = new StringBuilder(String.format("/*Dadd(%s, %s, %s)*/\n", uppers.get(0), lowers.get(0), lowers.get(1)));
            List<String> outputs = config.registerMapping.get(uppers.get(0));
            List<String> inputAs = config.registerMapping.get(lowers.get(0));
            List<String> inputBs = config.registerMapping.get(lowers.get(1));
            List<String> scratch = config.scratchRegisters;
            // scratch[0] := Carry bit
            sb.append(String.format("CLR(%s); \n", scratch.get(0)));
            for(int i = 0; i<config.bits; i++){
                sb.append(String.format("/* Bit %d */\n", i));
                sb.append(String.format("NOR(%s, %s, %s); ", outputs.get(i), inputAs.get(i), inputBs.get(i))); //vs(1) = !(a+b)
                sb.append(String.format("NOR(%s, %s, %s); ", scratch.get(2), inputAs.get(i), outputs.get(i))); //v2(2) = !(a+vs(1))
                sb.append(String.format("NOR(%s, %s, %s); ", scratch.get(3), inputBs.get(i), outputs.get(i))); //v3(3) = !(b+vs(1))
                sb.append(String.format("NOR(%s, %s, %s); ", scratch.get(1), outputs.get(i), scratch.get(2))); //v1(4) = !(vs(1)+v2(2))
                sb.append(String.format("NOR(%s, %s, %s); ", scratch.get(2), scratch.get(0), scratch.get(1))); //v2(5) = !(C+v1(4))
                sb.append(String.format("NOR(%s, %s, %s); ", scratch.get(3), scratch.get(1), scratch.get(2))); //v3(6) = !(v1(4)+v2(5))
                sb.append(String.format("NOR(%s, %s, %s); ", scratch.get(1), scratch.get(0), scratch.get(2))); //v1(7) = !(C+v2(5))
                sb.append(String.format("NOR(%s, %s, %s); ", scratch.get(0), scratch.get(2), outputs.get(i))); //vC(c) = !(v2(5)+vs(1))
                sb.append(String.format("NOR(%s, %s, %s); ", outputs.get(i), scratch.get(3), scratch.get(1))); //vs(S) = !(v3(6)+v1(7))
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
        public List<AtomGoal> applyOpForwards() throws TransformationApplicationException {
            if (this.sum == null){
                this.sum = new AtomGoal.Factory(a).addAll(b).get();
            }
            return Collections.singletonList(this.sum);
        }
    }

    public static class AddSelf extends Scamp5DigitalTransformation {
        // u := a + a

        final AtomGoal a;
        AtomGoal sum;


        public AddSelf(AtomGoal a, Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig) {
            super(scamp5DigitalConfig);

            this.a = a;
            this.sum = null;
        }
        public AddSelf(AtomGoal a, AtomGoal sum, Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig) {
            super(scamp5DigitalConfig);

            this.a = a;
            this.sum = sum;
        }

        public AddSelf(AtomGoal in, boolean upper, Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig) {
            super(scamp5DigitalConfig);

            if(!upper){
                this.a = in;
                this.sum = null;
            } else {
                boolean divisable = true;
                AtomGoal.Factory factory = new AtomGoal.Factory();
                for (Iterator<Tuple<Atom, Integer>> it = in.uniqueCountIterator(); it.hasNext(); ) {
                    Tuple<Atom, Integer> t = it.next();
                    divisable &= (t.getB()%2)==0;
                    factory.add(t.getA(), t.getB()/2);
                }
                if(divisable) {
                    a = factory.get();
                } else {
                    a = null;
                }
                this.sum = in;
            }

        }

        public boolean isPossible(){
            return a != null;
        }


        @Override
        public String code(List<RegisterAllocator.Register> uppers, List<RegisterAllocator.Register> lowers, List<RegisterAllocator.Register> trash) {
            assert lowers.size() == inputCount();
            assert lowers.size() == inputCount();
            StringBuilder sb = new StringBuilder(String.format("/*DaddSelf(%s, %s)*/\n", uppers.get(0), lowers.get(0)));
            List<String> outputs = config.registerMapping.get(uppers.get(0));
            List<String> inputAs = config.registerMapping.get(lowers.get(0));
            for(int i = config.bits-1; i>0; i--){
                sb.append(String.format("MOV(%s, %s); ", outputs.get(i), inputAs.get(i-1)));
            }
            sb.append(String.format("CLR(%s); \n", outputs.get(0)));
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
        public List<AtomGoal> applyOpForwards() throws TransformationApplicationException {
            if (this.sum == null){
                this.sum = new AtomGoal.Factory(a).addAll(a).get();
            }
            return Collections.singletonList(this.sum);
        }
    }

    public static class Div extends SimpleScamp5DigitalTransformation {
        // u := a*0.5 + error

        final AtomGoal a;
        AtomGoal div;

        public Div(AtomGoal a, Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig) {
            super(scamp5DigitalConfig);

            this.a = a;
            this.div = null;
        }

        public Div(AtomGoal in, boolean upper, Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig) {
            super(scamp5DigitalConfig);

            if(!upper){
                this.a = in;
                this.div = null;
            } else {
                this.a = new AtomGoal.Factory(in).addAll(in).get();
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
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            assert lowers.size() == inputCount();
            StringBuilder sb = new StringBuilder(String.format("/*DDiv(%s, %s)*/\n", upper, lowers.get(0)));
            List<String> outputs = config.registerMapping.get(upper);
            List<String> inputAs = config.registerMapping.get(lowers.get(0));
            for(int i = 0; i<config.bits-1; i++){
                sb.append(String.format("MOV(%s, %s); ", outputs.get(i), inputAs.get(i+1)));
            }
            sb.append(String.format("CLR(%s); \n", outputs.get(config.bits-1)));
            return sb.toString();
        }

        @Override
        public int inputCount() {
            return 1;
        }


        @SuppressWarnings("ConstantConditions")
        @Override
        public AtomGoal applyForwards() throws TransformationApplicationException {
            if(this.div == null){
                AtomGoal.Factory factory = new AtomGoal.Factory();
                if (!this.a.isEmpty()) {
                    int count = 1;
                    Atom last = a.get(0);
                    for (int i = 1; i < a.size()+1; i++) {
                        Atom c = i < a.size()?a.get(i):null;
                        if(c == null || !last.equals(c)){
                            if(count/2 != (count+1)/2){
                                throw new TransformationApplicationException("Cannot divide uneven number of atoms!");
                            } else {
                                for (int j = 0; j < count / 2; j++) {
                                    factory.add(last);
                                }
                            }
                            last = c;
                            count = 1;
                        } else {
                            count++;
                        }
                    }
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


    public static class Movx extends Mov {
        //u := a_dir

        final Dir dir;

        public Movx(AtomGoal a, Dir dir, Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig) {
            super(a, scamp5DigitalConfig);
            this.dir = dir;
        }

        public Movx(AtomGoal in, Dir dir, boolean upper, Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig) {

            super(upper?in.translated(-dir.x, -dir.y, 0):in, scamp5DigitalConfig);
            this.moved = upper?in:null;
            this.dir = dir;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            StringBuilder sb = new StringBuilder(String.format("/*Dmovx(%s, %s, %s)*/", upper, lowers.get(0), dir.toString()));
            List<String> outputs = config.registerMapping.get(upper);
            List<String> inputs = config.registerMapping.get(lowers.get(0));
            sb.append(String.format("CLR(%s, %s, %s, %s); ", Dir.values()[0].code, Dir.values()[1].code, Dir.values()[2].code, Dir.values()[3].code));
            sb.append(String.format("SET(%s); ", dir.code));
            for(int i = 0; i<config.bits; i++){
                sb.append(String.format("DNEWS0(%s, %s); ", outputs.get(i), inputs.get(i)));
            }
            return sb.toString();
        }

        @Override
        public AtomGoal applyForwards() {
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
