package uk.co.edstow.cain.scamp5.superPixel;

import uk.co.edstow.cain.transformations.BankedTransformation;
import uk.co.edstow.cain.goals.BankedKernel3DGoal;
import uk.co.edstow.cain.regAlloc.BRegister;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.atomGoal.Atom;
import uk.co.edstow.cain.goals.atomGoal.pairGen.SimpleTransformation;
import uk.co.edstow.cain.structures.Bounds;
import uk.co.edstow.cain.util.Bits;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;

public abstract class Scamp5SuperPixelTransformation<G extends BankedKernel3DGoal<G>> implements BankedTransformation {
    protected final Scamp5SuperPixelConfig<G> config;

    protected Scamp5SuperPixelTransformation(Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig) {
        this.config = scamp5SuperPixelConfig;
    }


    void selectBank(StringBuilder sb, int bank, String reg) {
        sb.append("scamp5_kernel_end(); ");
        if(reg.equals(config.selectReg)){
            sb.append(String.format("scamp5_select_pattern(); "));
        }else{
            boolean[][] mask = new boolean[config.width][config.height];
            for (int x = 0; x < config.width; x++) {
                for (int y = 0; y < config.height; y++) {
                    mask[x][y] = config.bitOrder[bank][x][y] > 0;
                }
            }

            char[] xBitsMask = new char[8];
            char[] xBitsValue = new char[8];
            {
                int spBits = Bits.log2nlz(mask.length);
                for (int i = 0; i < 8; i++) {
                    if (i < 8 - spBits) {
                        xBitsMask[i] = '1';
                        xBitsValue[i] = '0';
                    } else {
                        int currentBit = (8 - i) - 1;
                        boolean existsWhen0 = false;
                        boolean existsWhen1 = false;
                        for (int j = 0; j < mask.length; j++) {
                            if (((j >> currentBit) & 0x1) == 0) {
                                for (int k = 0; k < mask[j].length; k++) {
                                    existsWhen0 |= mask[j][k];
                                }
                            } else {
                                for (int k = 0; k < mask[j].length; k++) {
                                    existsWhen1 |= mask[j][k];
                                }
                            }
                        }
                        if (existsWhen0 && existsWhen1) {
                            xBitsMask[i] = '1';
                            xBitsValue[i] = '0';
                        } else {
                            xBitsMask[i] = '0';
                            if (existsWhen0) {
                                xBitsValue[i] = '0';
                            } else if (existsWhen1) {
                                xBitsValue[i] = '1';
                            } else {
                                throw new IllegalArgumentException("Bank " + bank + " does have any PEs assigned to it!");
                            }
                        }
                    }
                }
            }
            char[] yBitsMask = new char[8];
            char[] yBitsValue = new char[8];
            {
                int spBits = Bits.log2nlz(mask[0].length);
                for (int i = 0; i < 8; i++) {
                    if (i < 8 - spBits) {
                        yBitsMask[i] = '1';
                        yBitsValue[i] = '0';
                    } else {
                        int currentBit = (8 - i) - 1;
                        boolean existsWhen0 = false;
                        boolean existsWhen1 = false;
                        for (int j = 0; j < mask[0].length; j++) {
                            if (((j >> currentBit) & 0x1) == 0) {
                                for (int k = 0; k < mask.length; k++) {
                                    existsWhen0 |= mask[k][j];
                                }
                            } else {
                                for (int k = 0; k < mask.length; k++) {
                                    existsWhen1 |= mask[k][j];
                                }
                            }
                        }
                        if (existsWhen0 && existsWhen1) {
                            yBitsMask[i] = '1';
                            yBitsValue[i] = '0';
                        } else {
                            yBitsMask[i] = '0';
                            if (existsWhen0) {
                                yBitsValue[i] = '0';
                            } else if (existsWhen1) {
                                yBitsValue[i] = '1';
                            } else {
                                throw new IllegalArgumentException("Bank " + bank + " does have any PEs assigned to it!");
                            }
                        }
                    }
                }
            }

            String xMask = new String(xBitsMask);
            String xValue = new String(xBitsValue);
            String yMask = new String(yBitsMask);
            String yValue = new String(yBitsValue);



            sb.append(String.format("scamp5_load_pattern(%s, %s, %s, %s, %s); ", reg, xValue, yValue, xMask, yMask));
        }
        sb.append("scamp5_kernel_begin(); ");
    }

    private static class Pattern {
        public final int xMask;
        public final int yMask;
        public final int xVal;
        public final int yVal;

        Pattern(int xMask, int yMask, int xVal, int yVal) {
            this.xMask = xMask;
            this.yMask = yMask;
            this.xVal = xVal;
            this.yVal = yVal;
        }
    }
    void setDirLessSignificant(StringBuilder sb, int bank) {

        sb.append(String.format("CLR(%s, %s, %s, %s); ", config.northReg, config.eastReg, config.southReg, config.westReg));
        int[][] bitOrder = config.bitOrder[bank];
        char[][] bitDir = new char[config.width][config.height];
        int bits = config.getBits(bank);
        int xPos = config.xBankStart[bank];
        int yPos = config.yBankStart[bank];
        int place = 0;
        int countN = 0;
        int countE = 0;
        int countS = 0;
        int countW = 0;
        bitDir[xPos][yPos] = 'O';// origin (least significant bit)
        while (0 <= xPos && xPos < config.width && 0 <= yPos && yPos < config.height) {
            place = bitOrder[xPos][yPos];
            if(yPos+1 < config.height && bitOrder[xPos][yPos+1] == place+1) {
                bitDir[xPos][yPos+1] = 'S';
                countS++;
                yPos = yPos+1;
            } else if(xPos+1 < config.width && bitOrder[xPos+1][yPos] == place+1) {
                bitDir[xPos+1][yPos] = 'W';
                countW++;
                xPos = xPos+1;
            } else if(0 <= yPos-1 && bitOrder[xPos][yPos-1] == place+1) {
                bitDir[xPos][yPos-1] = 'N';
                countN++;
                yPos = yPos-1;
            } else if(0 <= xPos-1 && bitOrder[xPos-1][yPos] == place+1) {
                bitDir[xPos-1][yPos] = 'E';
                countE++;
                xPos = xPos-1;
            } else {
                // Next significant bit not found - exit
                xPos = -1;
                yPos = -1;
            }
        }
        if (place != bits) throw new IllegalArgumentException("Cannot find bits!");

        List<Pattern> northPatterns = new ArrayList<>();
        List<Pattern> eastPatterns = new ArrayList<>();
        List<Pattern> southPatterns = new ArrayList<>();
        List<Pattern> westPatterns = new ArrayList<>();


        List<Tuple<Integer, Integer>> masks = new ArrayList<>();
        for (int i = 0; i < config.width; i++) {
            for (int j = 0; j < config.height; j++) {
                masks.add(new Tuple<>(i, j));
            }
        }
        boolean[][] set = new boolean[config.width][config.height];
        masks.sort(Comparator.comparingInt(a -> Bits.countOnes(a.getA()) + Bits.countOnes(a.getB())));
        Iterator<Tuple<Integer, Integer>> iterator = masks.iterator();
        while (countN+countE+countS+countW>0 && iterator.hasNext()){
            Tuple<Integer, Integer> mask = iterator.next();
            int xMask = mask.getA();
            int yMask = mask.getB();
            for (int xVal = 1; xVal < config.width; xVal++) {
                for (int yVal = 1; yVal < config.height; yVal++) {

                    for (int x = 0; x < config.width; x++) {
                        boolean xSet = true;
                        for (int i = 0; i < Bits.log2nlz(config.width); i++) {
                            if (!Bits.isOne(xMask, i) && Bits.isOne(xVal, i) != Bits.isOne(x, i)) {
                                xSet = false;
                                break;
                            }
                        }
                        if (xSet) {
                            for (int y = 0; y < config.height; y++) {
                                boolean ySet = true;
                                for (int i = 0; i < Bits.log2nlz(config.height); i++) {
                                    if (!Bits.isOne(yMask, i) && Bits.isOne(yVal, i) != Bits.isOne(y, i)) {
                                        ySet = false;
                                        break;
                                    }
                                }
                                set[x][y] = ySet;
                            }
                        }
                    }

                    boolean matchN = true;
                    boolean matchS = true;
                    boolean matchE = true;
                    boolean matchW = true;
                    int setCount = 0;
                    for (int x = 0; x < config.width; x++) {
                        for (int y = 0; y < config.height; y++) {
                            if(set[x][y]){
                                setCount++;
                                matchN &= bitDir[x][y] == 'N';
                                matchE &= bitDir[x][y] == 'E';
                                matchS &= bitDir[x][y] == 'S';
                                matchW &= bitDir[x][y] == 'W';
                            }
                        }
                    }
                    if(matchN){
                        northPatterns.add(new Pattern(xMask, yMask, xVal, yVal));
                        countN -= setCount;
                    } else if (matchE) {
                        eastPatterns.add(new Pattern(xMask, yMask, xVal, yVal));
                        countE -= setCount;
                    } else if (matchS) {
                        southPatterns.add(new Pattern(xMask, yMask, xVal, yVal));
                        countS -= setCount;
                    } else if (matchW) {
                        westPatterns.add(new Pattern(xMask, yMask, xVal, yVal));
                        countW -= setCount;
                    }

                    if(matchN || matchE || matchS || matchW) {
                        for (int x = 0; x < config.width; x++) {
                            for (int y = 0; y < config.height; y++) {
                                if (set[x][y]) {
                                    bitDir[x][y] = 0;
                                }
                            }
                        }
                    }

                }
            }
        }
        if (countN+countE+countS+countW!=0) throw new IllegalArgumentException("Cannot find patterns to match bank");


    }

    void setDirMoreSignificant(StringBuilder sb, int bank) {

    }


    public static class Null<G extends BankedKernel3DGoal<G>> extends Scamp5SuperPixelTransformation<G> {
        private final int inputCount;
        private final int outputCount;

        public Null(int inputCount, int outputCount, Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig) {
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
            return String.format("//Null Instruction: %s <- %s", uppers, lowers);
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

        SimpleScamp5SuperPixelTransformation(Scamp5SuperPixelConfig<G> config) {
            super(config);
        }

        @Override
        public String code(List<BRegister> uppers, List<BRegister> lowers, List<BRegister> trash) {
            if (uppers.size() == 1) {
                return code(uppers.get(0), lowers);
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

        public <G extends BankedKernel3DGoal<G>> String code(Scamp5SuperPixelConfig<G> config) {
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

        public Res(List<G> results, Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);
            this.results = results;
        }

        @Override
        public String code(List<BRegister> uppers, List<BRegister> lowers, List<BRegister> trash) {
//            assert lowers.size() == inputCount();
            StringBuilder sb = new StringBuilder(String.format("/*SP res(%s)*/", uppers));
            for (int bank = 0; bank < config.banks; bank++) {
                List<BRegister> regs = new ArrayList<>();
                for (BRegister reg: uppers)if(reg.bank==bank) regs.add(reg);
                if (!regs.isEmpty()){
                    this.selectBank(sb, bank, config.maskReg);
                    for (BRegister reg: regs){
                        String sreg = reg.name;
                        sb.append(String.format("NOT(%s, %s); ", config.scratchRegisters.get(0), sreg));
                        sb.append(String.format("NOR(%s, %s, %s); ", sreg, config.scratchRegisters.get(0), config.maskReg));
                    }
                }
            }
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

    public static class Add_2<G extends BankedKernel3DGoal<G>> extends Scamp5SuperPixelTransformation<G> {
        // u := a + b

        final G a;
        final G b;
        G sum;


        public Add_2(G a, G b, Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);
            assert a.getBank() == b.getBank();
            this.a = a;
            this.b = b;
            this.sum = null;
        }

        @Override
        public String code(List<BRegister> uppers, List<BRegister> lowers, List<BRegister> trash) {
            assert uppers.size() == 1;
            assert lowers.size() == 2;
            assert uppers.get(0).bank == lowers.get(0).bank && uppers.get(0).bank == lowers.get(1).bank;
            int bank = uppers.get(0).bank;
            StringBuilder sb = new StringBuilder(String.format("/*SP add(%s, %s, %s)*/\n", uppers.get(0), lowers.get(0), lowers.get(1)));
            String outputReg = uppers.get(0).name;
            String inputAReg = lowers.get(0).name;
            String inputBReg = lowers.get(1).name;
            List<String> scratch = config.scratchRegisters;

            for(int i = 0; i<config.getBits(bank); i++){
                sb.append(String.format("/* Bit %d */\n", i));
                // Copy in Carry in from correct PE
                setDirLessSignificant(sb, bank);
                // the least significant bit will have zeros for n,e,s,w so this should clear maskReg to 0 for that PE
                sb.append(String.format("DNEWS0(%s, %s); ", config.maskReg, config.scratchRegisters.get(0)));
                // maskReg := Carry in bit

                // Use dir registers as scratch registers - we'll have to use setDirLessSignificant(sb, bank) on every
                // Iteration but this save 4 scratch registers!
                sb.append(String.format("NOR(%s, %s, %s); ", config.northReg, inputAReg, inputBReg));           //vn(1) = !(a+b)
                sb.append(String.format("NOR(%s, %s, %s); ", config.eastReg, inputAReg, config.northReg));      //ve(2) = !(a+vn(1))
                sb.append(String.format("NOR(%s, %s, %s); ", config.southReg, inputBReg, config.northReg));     //vs(3) = !(b+vn(1))
                sb.append(String.format("NOR(%s, %s, %s); ", config.westReg, config.eastReg, config.southReg)); //vw(4) = !(ve(2)+vs(3))
                sb.append(String.format("NOR(%s, %s, %s); ", config.eastReg, config.maskReg, config.westReg)); //ve(5) = !(vm(C)+vw(4))
                sb.append(String.format("NOR(%s, %s, %s); ", config.southReg, config.westReg, config.eastReg)); //vs(6) = !(vw(4)+ve(5))
                sb.append(String.format("NOR(%s, %s, %s); ", config.westReg, config.maskReg, config.eastReg)); //vw(7) = !(vm(C)+ve(5))
                sb.append(String.format("NOR(%s, %s, %s); ", scratch.get(0), config.eastReg, config.northReg)); //v0(C) = !(ve(5)+vn(1))
                // scratch[0] := Carry out bit

                // Multiplex to only store sum if we're in the correct bank.
                sb.append(String.format("SET(%s); ", config.maskReg));
                sb.append(String.format("MOV(%s, %s); ", config.maskedReg, outputReg));
                selectBank(sb, uppers.get(0).bank, config.maskReg);
                sb.append(String.format("NOR(%s, %s, %s); ", config.maskedReg, config.southReg, config.westReg)); //vm(S) = !(vs(6)+vw(7))
                sb.append(String.format("MOV(%s, %s); ", outputReg, config.maskedReg));

            }
            return sb.toString();
        }

        @Override
        public double cost() {
            return (15 * this.config.getBits(a.getBank()));
        }

        @Override
        public String toString() {
            return String.format("SP Add2(%s, %s)", a, b);
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
            return 0;
        }
    }

    public static class AddSelf<G extends BankedKernel3DGoal<G>> extends Scamp5SuperPixelTransformation<G> {
        // u := a + a

        final G a;
        G sum;


        public AddSelf(G a, Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);

            this.a = a;
            this.sum = null;
        }
        public AddSelf(G a, G sum, Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);

            this.a = a;
            this.sum = sum;
        }

        public AddSelf(G in, boolean upper, Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);

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
        public String code(List<BRegister> uppers, List<BRegister> lowers, List<BRegister> trash) {
            assert uppers.size() == 1;
            assert lowers.size() == 1;
            assert uppers.get(0).bank == lowers.get(0).bank;
            int bank = uppers.get(0).bank;
            StringBuilder sb = new StringBuilder(String.format("/*SP addSelf(%s, %s)*/\n", uppers.get(0), lowers.get(0)));
            String outputReg = uppers.get(0).name;
            String inputReg = lowers.get(0).name;
            List<String> scratch = config.scratchRegisters;

            setDirLessSignificant(sb, bank);
            // the least significant bit will have zeros for n,e,s,w

            // Multiplex to only store sum if we're in the correct bank.
            sb.append(String.format("SET(%s); ", config.maskReg));
            sb.append(String.format("MOV(%s, %s); ", config.maskedReg, outputReg));
            selectBank(sb, uppers.get(0).bank, config.maskReg);
            sb.append(String.format("DNEWS0(%s, %s); ", config.maskedReg, inputReg));
            sb.append(String.format("MOV(%s, %s); ", outputReg, config.maskedReg));


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

        public Div(G a, Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig) {
            super(scamp5SuperPixelConfig);

            this.a = a;
            this.div = null;
        }

        public Div(G in, boolean upper, Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig) {
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
            int bank = uppers.get(0).bank;
            StringBuilder sb = new StringBuilder(String.format("/*SP addSelf(%s, %s)*/\n", uppers.get(0), lowers.get(0)));
            String outputReg = uppers.get(0).name;
            String inputReg = lowers.get(0).name;
            List<String> scratch = config.scratchRegisters;

            // Copy in Carry in from correct PE
            setDirMoreSignificant(sb, bank);
            // the most significant bit will have zeros for n,e,s,w

            // Multiplex to only store sum if we're in the correct bank.
            //      always write (save) outputReg soo all PEs outside the bank aren't corrupted
            sb.append(String.format("SET(%s); ", config.maskReg));
            sb.append(String.format("MOV(%s, %s); ", config.maskedReg, outputReg));
            //      write shifted result if this is in the selected Bank
            selectBank(sb, uppers.get(0).bank, config.maskReg);
            sb.append(String.format("DNEWS0(%s, %s); ", config.maskedReg, inputReg));
            //      write un-shifted result if all dir bits are 0 (if this is Most significant bit)
            sb.append(String.format("OR(%s, %s, %s, %s, %s); ", scratch.get(0), config.northReg, config.eastReg, config.southReg, config.westReg));
            sb.append(String.format("NOT(%s, %s); ", config.northReg, config.maskReg));
            sb.append(String.format("NOR(%s, %s, %s); ", config.maskReg, scratch.get(0), config.northReg));
            sb.append(String.format("MOV(%s, %s); ", config.maskedReg, inputReg));
            //      write answer into outputReg
            sb.append(String.format("MOV(%s, %s); ", outputReg, config.maskedReg));


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
            return String.format("DDiv(%s)", this.a);
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

        public Movxb(G upper, G lower, Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig) {
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
        public Movxb(G upper, int x, int y, int bank, Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig) {
            this(upper ,upper.translated(x, y, 0).inBank(bank), scamp5SuperPixelConfig);
        }

        @Override
        public String code(List<BRegister> uppers, List<BRegister> lowers, List<BRegister> trash) {
            assert uppers.size() == 1;
            assert lowers.size() == 1;
            StringBuilder sb = new StringBuilder(String.format("/*SP movb(%s, %s)*/", uppers.get(0), lowers.get(0)));
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
                sb.append(String.format("CLR(%s, %s, %s, %s); ", config.northReg, config.eastReg, config.southReg, config.westReg));
                sb.append(String.format("SET(%s, %s); ", dirReg, config.maskReg));//also set maskReg so we can use maskedReg
                for (int i = 0; i < Math.abs(xPETranslation); i++) {
                    if(i%2 == 0) { // ensure we never do a Mov where dst and src are the same!
                        sb.append(String.format("DNEWS0(%s, %s); ", scratchReg, currentReg));
                        currentReg = scratchReg;
                    }else{
                        sb.append(String.format("DNEWS0(%s, %s); ", config.maskedReg, currentReg));
                        currentReg = config.maskedReg;
                    }
                }
            }
            if(yPETranslation != 0) {
                String dirReg = yPETranslation<0? config.northReg : config.southReg;
                // set move direction
                sb.append(String.format("CLR(%s, %s, %s, %s); ", config.northReg, config.eastReg, config.southReg, config.westReg));
                sb.append(String.format("SET(%s, %s); ", dirReg, config.maskReg));//also set maskReg so we can use maskedReg
                for (int i = 0; i < Math.abs(yPETranslation); i++) {
                    if(i%2 == 0) { // ensure we never do a Mov where dst and src are the same!
                        sb.append(String.format("DNEWS0(%s, %s); ", scratchReg, currentReg));
                        currentReg = scratchReg;
                    }else{
                        sb.append(String.format("DNEWS0(%s, %s); ", config.maskedReg, currentReg));
                        currentReg = config.maskedReg;
                    }
                }
            }
            if (xPETranslation == 0 && yPETranslation == 0){ // catch case when translation is (0,0)
                sb.append(String.format("SET(%s); ", config.maskReg));// set maskReg so we can use maskedReg
            }
            if(currentReg == config.maskedReg){
                // the moved result is in maskedReg
                this.selectBank(sb, ubank, config.selectReg); // invert selectBank into maskReg
                sb.append(String.format("NOT(%s, %s); ", config.maskReg, config.selectReg));
                sb.append(String.format("MOV(%s, %s); ", config.maskedReg, output)); // copy original output value if this PE is Not in bank
            } else {
                // the moved result is in currentReg (and currentReg isn't maskedReg)
                sb.append(String.format("MOV(%s, %s); ", config.maskedReg, output)); // copy original output value
                this.selectBank(sb, ubank, config.maskReg); // selectBank into maskReg
                sb.append(String.format("MOV(%s, %s); ", config.maskedReg, currentReg)); // copy moved value if PE is in Bank
            }

            sb.append(String.format("MOV(%s, %s); ", output, config.maskedReg)); // copy result into outputReg

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
