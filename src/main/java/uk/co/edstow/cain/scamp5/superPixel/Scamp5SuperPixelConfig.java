package uk.co.edstow.cain.scamp5.superPixel;

import uk.co.edstow.cain.scamp5.Scamp5Config;
import uk.co.edstow.cain.scamp5.output.Scamp5OutputFormatter;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.util.Bits;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Scamp5SuperPixelConfig extends Scamp5Config<Scamp5SuperPixelConfig> {

    public final boolean useMovbx;
    public final boolean useAdd;
    public final boolean useAddSelf;
    public final boolean useSub;
    public final boolean useDiv;
    public final boolean useRes;
    private final boolean onlyMov;

    final List<String> scratchRegisters;
    final String selectReg;
    public final String maskReg;
    public final String maskedReg;
    public final String northReg;
    public final String eastReg;
    public final String southReg;
    public final String westReg;

    public final int width;
    public final int height;
    public final int banks;

    public final int[][][] bitOrder;
    public final int[] bits;
    public final int[] xBankStart;
    public final int[] yBankStart;
    public final boolean[][] sameShapeLookup;
    
    public Scamp5SuperPixelConfig(boolean useMovbx, boolean useAdd, boolean useAddSelf, boolean useSub, boolean useDiv, boolean useRes, List<String> scratchRegisters, String selectReg, String maskReg, String maskedReg, String northReg, String eastReg, String southReg, String westReg, int width, int height, int banks, int[][][] bitOrder, Scamp5OutputFormatter outputFormatter) {
        super(outputFormatter);
        this.useMovbx = useMovbx;
        this.useAdd = useAdd;
        this.useAddSelf = useAddSelf;
        this.useSub = useSub;
        this.useDiv = useDiv;
        this.useRes = useRes;

        this.onlyMov = !(this.useAdd || this.useDiv || this.useRes);

        this.width = width;
        this.height = height;
        this.banks = banks;

        this.scratchRegisters = scratchRegisters;
        this.selectReg = selectReg;
        this.maskReg = maskReg;
        this.maskedReg = maskedReg;
        this.northReg = northReg;
        this.eastReg = eastReg;
        this.southReg = southReg;
        this.westReg = westReg;


        this.bitOrder = bitOrder;
        this.bits = new int[banks];
        this.xBankStart = new int[banks];
        this.yBankStart = new int[banks];
        this.sameShapeLookup = new boolean[banks][banks];
        generateBankInfo();
    }

    public Scamp5SuperPixelConfig(Scamp5SuperPixelConfig proto, List<String> scratchRegisters, String selectReg, String maskReg, String maskedReg, String northReg, String eastReg, String southReg, String westReg, int width, int height, int banks, int[][][] bitOrder) {
        super(proto.outputFormatter);
        this.useMovbx = proto.useMovbx;
        this.useAdd = proto.useAdd;
        this.useAddSelf = proto.useAddSelf;
        this.useSub = proto.useSub;
        this.useDiv = proto.useDiv;
        this.useRes = proto.useRes;

        this.onlyMov = !(this.useAdd || this.useAddSelf || this.useDiv || this.useRes);


        this.width = width;
        this.height = height;
        this.banks = banks;

        this.scratchRegisters = scratchRegisters;
        this.selectReg = selectReg;
        this.maskReg = maskReg;
        this.maskedReg = maskedReg;
        this.northReg = northReg;
        this.eastReg = eastReg;
        this.southReg = southReg;
        this.westReg = westReg;

        this.bitOrder = bitOrder;
        this.bits = new int[banks];
        this.xBankStart = new int[banks];
        this.yBankStart = new int[banks];
        this.sameShapeLookup = new boolean[banks][banks];
        generateBankInfo();

    }

    public Scamp5SuperPixelConfig(Scamp5SuperPixelConfig proto, boolean onlyMov) {
        super(proto.outputFormatter);
        this.useMovbx = proto.useMovbx;
        this.useAdd = proto.useAdd && (!onlyMov);
        this.useAddSelf = proto.useAddSelf && (!onlyMov);
        this.useSub = proto.useSub && (!onlyMov);
        this.useDiv = proto.useDiv && (!onlyMov);
        this.useRes = proto.useRes && (!onlyMov);

        this.onlyMov = !(this.useAdd || this.useDiv || this.useRes);

        this.width = proto.width;
        this.height = proto.height;
        this.banks = proto.banks;

        this.scratchRegisters = proto.scratchRegisters;
        this.selectReg = proto.selectReg;
        this.maskReg = proto.maskReg;
        this.maskedReg = proto.maskedReg;
        this.northReg = proto.northReg;
        this.eastReg = proto.eastReg;
        this.southReg = proto.southReg;
        this.westReg = proto.westReg;

        this.bitOrder = proto.bitOrder;
        this.bits = proto.bits;
        this.xBankStart = proto.xBankStart;
        this.yBankStart = proto.yBankStart;
        this.sameShapeLookup = proto.sameShapeLookup;
    }


    private void generateBankInfo() {
        for (int i = 0; i < this.banks; i++) {
            this.bits[i] = Arrays.stream(this.bitOrder[i]).mapToInt(is -> Arrays.stream(is).max().orElse(0)).max().orElse(0);

            loop:
            for (int x = 0; x < this.width; x++) {
                for (int y = 0; y < this.height; y++) {
                    if (this.bitOrder[i][x][y] == 1) {
                        this.xBankStart[i] = x;
                        this.yBankStart[i] = y;
                        break loop;
                    }
                }
            }
        }
        for (int i = 0; i < this.banks; i++) {
            for (int j = 0; j < this.banks; j++) {
                boolean same = true;
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        if(this.bitOrder[i][x][y]>0){
                            int jx = x-this.xBankStart[i]+this.xBankStart[j];
                            int jy = y-this.yBankStart[i]+this.yBankStart[j];
                            same &= 0 <= jx && jx < this.width &&
                                    0 <= jy && jy < this.height &&
                                    this.bitOrder[i][x][y] == this.bitOrder[j][jx][jy];
                        }
                    }
                }
                this.sameShapeLookup[i][j] = same;
            }
        }
    }

    public boolean onlyMov() {
        return onlyMov;
    }

    public int getBits(int bank) {
        return this.bits[bank];
    }
    public boolean isBankSameShape(int bank1, int bank2){
        return this.sameShapeLookup[bank1][bank2];
    }

    public Tuple<Integer, Integer> getBankTranslation(int fromBank, int toBank) {
        return new Tuple<>(xBankStart[toBank] - xBankStart[fromBank], yBankStart[toBank] - yBankStart[fromBank]);
    }

    @Override
    public Builder builder() {
        return new Builder(this);
    }

    private enum Dir{
        N(0,'N','n', 0,1, c->c.northReg),
        E(1,'E','e',1,0, c->c.eastReg),
        S(2,'S','s',0,-1, c->c.southReg),
        W(3,'W','w',-1,0, c->c.westReg);
        final char u;
        final char l;
        final int idx;
        final int x;
        final int y;
        private final Function<Scamp5SuperPixelConfig, String> getReg;
        Dir(int idx, char u, char l, int x, int y, Function<Scamp5SuperPixelConfig, String> getReg) {
            this.idx = idx;
            this.u = u;
            this.l = l;
            this.x = x;
            this.y = y;
            this.getReg = getReg;
        }
        String getReg(Scamp5SuperPixelConfig config){
            return this.getReg.apply(config);
        }
        int get(int[] arr){
            return arr[idx];
        }
        int inc(int[] arr){
            arr[idx] = arr[idx]+1;
            return arr[idx];
        }
        int dec(int[] arr){
            arr[idx] = arr[idx]-1;
            return arr[idx];
        }
        int add(int[] arr, int val){
            arr[idx] = arr[idx]+val;
            return arr[idx];
        }
        int set(int[] arr, int val){
            int t = arr[idx];
            arr[idx] = val;
            return t;
        }
        static int[] makeArray(int d){
            int[] ints = new int[Dir.values().length];
            Arrays.fill(ints, d);
            return ints;
        }
        boolean get(boolean[] arr){
            return arr[idx];
        }
        boolean or(boolean[] arr, boolean v){
            arr[idx] = arr[idx] || v;
            return arr[idx];
        }
        boolean and(boolean[] arr, boolean v){
            arr[idx] = arr[idx] && v;
            return arr[idx];
        }
        boolean set(boolean[] arr, boolean v){
            boolean t = arr[idx];
            arr[idx] =  v;
            return t;
        }
        static boolean[] makeArray(boolean d){
            boolean[] booleans = new boolean[Dir.values().length];
            Arrays.fill(booleans, d);
            return booleans;
        }
        static <V> Map<Dir, V> makeMap(Supplier<V> defaultVal){
            Map<Dir, V> map = new HashMap<>();
            for (Dir value : Dir.values()) {
                map.put(value, defaultVal.get());
            }
            return map;
        }
        static Dir getDir(char c){
            for (int i = 0; i < Dir.values().length; i++) {
                if(Dir.values()[i].u == c || Dir.values()[i].l == c) return Dir.values()[i];
            }
            throw new IllegalArgumentException("char isn't a Dir");
        }

    }

    private static class Pattern {
        public final byte xMask;
        public final byte yMask;
        public final byte xVal;
        public final byte yVal;
        public final int count;
        public final boolean[][] set;


        Pattern(int xMask, int yMask, int xVal, int yVal, int count, boolean[][] set) {
            this.xMask = (byte) xMask;
            this.yMask = (byte) yMask;
            this.xVal = (byte) xVal;
            this.yVal = (byte) yVal;
            this.count = count;
            this.set = set;
        }
    }
//    void printBitCharMask(char[][] dirs, int w, int h){
//        for (int y = h-1; y >= 0 ; y--) {
//            for (int x = 0; x < w; x++) {
//                System.out.print(dirs[x][y]=='\0'?'_':dirs[x][y]);
//                if(x<w-1) {
//                    System.out.print(',');
//                }
//            }
//            System.out.println();
//        }
//    }
//    void printBitSet(boolean[][] set, int w, int h){
//        for (int y = h-1; y >= 0 ; y--) {
//            for (int x = 0; x < w; x++) {
//                System.out.print(set[x][y]?'1':'0');
//                if(x<w-1) {
//                    System.out.print(',');
//                }
//            }
//            System.out.println();
//        }
//    }

    void selectBank(StringBuilder sb, int bank, String reg, List<String> scratchRegisters) {
        char[][] mask = new char[this.width][this.height];
        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.height; y++) {
                mask[x][y] = this.bitOrder[bank][x][y] > 0? 'O':'#';
            }
        }
//        printBitCharMask(mask, this.width, this.height);
        List<Pattern> patternList = generateBitPatterns(mask, this.getBits(bank));
        List<String> toClear = new ArrayList<>();
        sb.append(String.format("/*SetSelectBank %s -> %s*/", bank, reg));
        List<String> availableRegs = new ArrayList<>(scratchRegisters);
        if(!availableRegs.contains(reg)) availableRegs.add(reg);
        generateCodeForPatternsReg(sb, availableRegs, toClear, reg, patternList);
        sb.append("/*done - SetSelectBank*/");
    }



    void setDirLessSignificant(StringBuilder sb, int bank) {
        char[][] bitDir = new char[this.width][this.height];
        int[] count = generateBitDirectionArray(bank, bitDir, true);
        Map<Dir, List<Pattern>> patterns = generateBitDirectionPatterns(bitDir, count);
        sb.append("/*SetDirLessSignificant*/");
        generateCodeForDirectionPatterns(sb, patterns);
        sb.append("/*done - SetDirLessSignificant*/");

    }

    void setDirMoreSignificant(StringBuilder sb, int bank) {
        char[][] bitDir = new char[this.width][this.height];
        int[] count = generateBitDirectionArray(bank, bitDir, false);
        Map<Dir, List<Pattern>> patterns = generateBitDirectionPatterns(bitDir, count);
        sb.append("/*SetDirMoreSignificant*/");
        generateCodeForDirectionPatterns(sb, patterns);
        sb.append("/*done - SetDirMoreSignificant*/");
    }

    public void setDirLessSignificantAndSelectBit(StringBuilder sb, int bank, int place, String reg, List<String> availableRegs) {
        sb.append("/*SetDirLessSignificantAndSelect*/");
        setDirSignificantAndSelectBit(sb, bank, place, reg, true, availableRegs);
        sb.append("/*done - SetDirLessSignificantAndSelect*/");
    }

    public void setDirMoreSignificantAndSelectBit(StringBuilder sb, int bank, int place, String reg, List<String> availableRegs) {
        sb.append("/*SetDirMoreSignificantAndSelect*/");
        setDirSignificantAndSelectBit(sb, bank, place, reg, false, availableRegs);
        sb.append("/*done - SetDirMoreSignificantAndSelect*/");
    }

    private void setDirSignificantAndSelectBit(StringBuilder sb, int bank, int place, String reg, boolean forwards, List<String> availableRegs) {
        int x = -1;
        int y = -1;
        Dir direction = null;
        {
            bitSearch:
            for (int xPos = 0; xPos < this.width; xPos++) {
                for (int yPos = 0; yPos < this.height; yPos++) {
                    if (this.bitOrder[bank][xPos][yPos] == place) {
                        x = xPos;
                        y = yPos;
                        for (int d = 0; d < Dir.values().length; d++) {
                            Dir dir = Dir.values()[d];
                            if (0 <= xPos - dir.x && xPos - dir.x < this.width && 0 <= yPos - dir.y && yPos - dir.y < this.height && this.bitOrder[bank][xPos - dir.x][yPos - dir.y] == place + (forwards?1:-1) && place + (forwards?1:-1)>0) {
                                direction = dir;
                                break;
                            }
                        }

                        break bitSearch;
                    }
                }
            }
            if(x<0) throw new IllegalArgumentException("Bit " + place + " Not found in Bank "+ bank);
        }
        byte xMask = (byte) (256-this.width); // mask set to like this since only 1 PE in the SuperPixel can be set at
        byte yMask = (byte) (256-this.height);// a time using this method.
        byte xVal = (byte) x;
        byte yVal = (byte) y;

        sb.append(outputFormatter.CLR(this.northReg, this.southReg, this.eastReg, this.westReg));
        if(reg.equals(this.selectReg)) {
            sb.append(outputFormatter.select_pattern(xVal, yVal, xMask, yMask));
        } else {
            sb.append(outputFormatter.load_pattern(reg, xVal, yVal, xMask, yMask));
        }
        if(direction != null){
                sb.append(outputFormatter.MOV(direction.getReg.apply(this), reg));
        }

    }


    private void generateCodeForDirectionPatterns(StringBuilder sb, Map<Dir, List<Pattern>> patterns) {
        List<Map.Entry<Dir, List<Pattern>>> patternList = patterns.entrySet().stream().sorted(Comparator.comparingInt(e -> -e.getValue().size())).collect(Collectors.toList());
        List<String> availableRegs = new ArrayList<>(Arrays.asList(this.northReg, this.southReg, this.eastReg, this.westReg, this.maskReg));
        List<String> toClear = new ArrayList<>();
        for (Map.Entry<Dir, List<Pattern>> entry : patternList) {
            Dir dir = entry.getKey();
            List<Pattern> list = entry.getValue();
            String reg = dir.getReg(this);
            sb.append(String.format("/* loading Dir: %s into %s */", dir.u, reg));
            generateCodeForPatternsReg(sb, availableRegs, toClear, reg, list);
        }
        if(!toClear.isEmpty()){
            sb.append(outputFormatter.CLR(toClear.toArray(new String[0])));
        }
    }

    private void generateCodeForPatternsReg(StringBuilder sb, List<String> availableRegs, List<String> toClear, String reg, List<Pattern> list) {
        availableRegs.remove(reg);
        if (list.isEmpty()){
            toClear.add(reg);
        } else if (list.size()==1){
            Pattern pattern = list.get(0);
            if(reg.equals(this.selectReg)) {
                // x and y may need to be swapped in these pattern calls
                sb.append(outputFormatter.select_pattern(pattern.xVal, pattern.yVal, pattern.xMask, pattern.yMask));
            } else {
                sb.append(outputFormatter.load_pattern(reg, pattern.xVal, pattern.yVal, pattern.xMask, pattern.yMask));
            }
        } else if (list.size() <= 4 && list.size() <= availableRegs.size()+1) {
            if(reg.equals(this.selectReg)) {
                throw new IllegalArgumentException("Scamp5 cannot have multiple patterns together in select register: "+this.selectReg);
            }
            Pattern pattern;
            for (int i = 0; i < list.size() - 1; i++) {
                pattern = list.get(i);
                sb.append(outputFormatter.load_pattern(availableRegs.get(i), pattern.xVal, pattern.yVal, pattern.xMask, pattern.yMask));
            }
            pattern = list.get(list.size()-1);
            sb.append(outputFormatter.select_pattern(pattern.xVal, pattern.yVal, pattern.xMask, pattern.yMask));

            List<String> regs = new ArrayList<>();
            for (int i = 0; i < list.size() - 1; i++) {
                regs.add(availableRegs.get(i));
            }
            sb.append(outputFormatter.OR(reg, regs.toArray(new String[0])));

        } else {
            throw new IllegalArgumentException("More complex Direction-Pattern loading not supported. Ask a developer for support");
        }
    }


    private Map<Dir, List<Pattern>> generateBitDirectionPatterns(char[][] bitDir, int[] count) {


        Map<Dir, List<Pattern>> patterns = Dir.makeMap(ArrayList::new);


        List<Tuple<Integer, Integer>> masks = new ArrayList<>();
        {
            for (int i = 0; i < this.width; i++) {
                for (int j = 0; j < this.height; j++) {
                    masks.add(new Tuple<>(256-this.width+i, 256-this.height+j));
                }
            }
            masks.sort(Comparator.comparingInt(a -> -Bits.countOnes(a.getA()) - Bits.countOnes(a.getB())));
        }

        while(Arrays.stream(count).sum()>0){

            Map<Dir, List<Pattern>> nextPatterns = Dir.makeMap(ArrayList::new);

            for (Tuple<Integer, Integer> mask : masks) {
                int xMask = mask.getA();
                int yMask = mask.getB();
                for (int xVal = 0; xVal < this.width; xVal++) {
                    for (int yVal = 0; yVal < this.height; yVal++) {
                        if ((xMask & xVal) > 0 || (yMask & yVal) > 0) continue;

                        boolean[][] set = getPatternSetArray(xMask, yMask, xVal, yVal);
//                        System.out.println("xMask: " + Bits.Str(xMask, 2) + " xVal: " + Bits.Str(xVal, 2) + " yMask: " + Bits.Str(yMask, 2) + " yVal: " + Bits.Str(yVal, 2));
//                        printBitSet(set, this.width, this.height);

                        boolean[] match = Dir.makeArray(true);
                        int[] newCount = Dir.makeArray(0);
                        for (int x = 0; x < this.width; x++) {
                            for (int y = 0; y < this.height; y++) {
                                if (set[x][y]) {
                                    for (Dir dir : Dir.values()) {
                                        dir.and(match, bitDir[x][y] == dir.u || bitDir[x][y] == dir.l || bitDir[x][y] == 0);
                                        dir.add(newCount, bitDir[x][y] == dir.u ? 1 : 0);
                                    }
                                }
                            }
                        }
//                        for (Dir dir : Dir.values()) {
//                            System.out.print(dir.u+":" + (dir.get(match) ? "T " : "F ") + dir.get(newCount) + ",  ");
//                        } System.out.println();


                        for (Dir dir : Dir.values()) {
                            if(dir.get(match) && dir.get(newCount)>0){
                                nextPatterns.get(dir).add(new Pattern(xMask, yMask, xVal, yVal, dir.get(newCount), set));
                                break;
                            }
                        }

                    }
                }
            }
            nextPatterns.forEach((dir, ps) -> ps.sort(Comparator.comparingInt(pattern -> -pattern.count)));
            for (Dir dir : Dir.values()) {
                List<Pattern> patternList = nextPatterns.get(dir);
                if(!patternList.isEmpty()){
                    Pattern pattern = patternList.get(0);
                    patterns.get(dir).add(pattern);
                    dir.add(count, -pattern.count);
                    for (int x = 0; x < this.width; x++) {
                        for (int y = 0; y < this.height; y++) {
                            if (pattern.set[x][y] && bitDir[x][y] != 0) {
                                bitDir[x][y] = dir.l;
                            }
                        }
                    }
                }
            }
//            printBitCharMask(bitDir, this.width, this.height);

        }
        if (Arrays.stream(count).sum()!=0) throw new IllegalArgumentException("Cannot find patterns to match bank");
        return patterns;
    }

    private int[] generateBitDirectionArray(int bank, char[][] bitDir, boolean forwards) {
        int bits = this.getBits(bank);
        int xPos = -1;
        int yPos = -1;
        if(forwards) {
            xPos = this.xBankStart[bank];
            yPos = this.yBankStart[bank];
        } else {
            boolean found = false;
            // start at the highest number (most significant bit)
            for (int x = 0; x < this.width; x++) {
                for (int y = 0; y < this.height; y++) {
                    if(this.bitOrder[bank][x][y] == bits){
                        xPos = x;
                        yPos = y;
                        found = true;
                        break;
                    }
                }
            }
            if (!found) throw new IllegalArgumentException("getBits("+bank+") returned returned "+bits+" but this value isn't in BitOrder");
        }
        int place;

        int[] count = Dir.makeArray(0);
        bitDir[xPos][yPos] = 'O';// origin (least or most significant bit depending on 'forwards')
        do {
            place = this.bitOrder[bank][xPos][yPos];
            for (int i = 0; i < Dir.values().length; i++) {
                Dir dir = Dir.values()[i];
                if (0 <= xPos - dir.x && xPos - dir.x < this.width && 0 <= yPos - dir.y && yPos - dir.y < this.height && this.bitOrder[bank][xPos - dir.x][yPos - dir.y] == place + (forwards?1:-1) && place + (forwards?1:-1)>0) {
                    bitDir[xPos - dir.x][yPos - dir.y] = dir.u;
                    dir.inc(count);
                    xPos = xPos - dir.x;
                    yPos = yPos - dir.y;
                    break;
                }
            }
        } while (place != this.bitOrder[bank][xPos][yPos]);
//        printBitCharMask(bitDir, this.width, this.height);
        if (place != (forwards?bits:1)) throw new IllegalArgumentException("Cannot find bits!");
        return count;
    }



    private List<Pattern> generateBitPatterns(char[][] bitMask, int count) {

        List<Pattern> patterns = new ArrayList<>();


        List<Tuple<Integer, Integer>> masks = new ArrayList<>();
        {
            for (int i = 0; i < this.width; i++) {
                for (int j = 0; j < this.height; j++) {
                    masks.add(new Tuple<>(i, j));
                }
            }
            masks.sort(Comparator.comparingInt(a -> -Bits.countOnes(a.getA()) - Bits.countOnes(a.getB())));
        }

        while(count>0){

            List<Pattern> nextPattern = new ArrayList<>();

            for (Tuple<Integer, Integer> mask : masks) {
                int xMask = mask.getA();
                int yMask = mask.getB();
                for (int xVal = 0; xVal < this.width; xVal++) {
                    for (int yVal = 0; yVal < this.height; yVal++) {
                        if ((xMask & xVal) > 0 || (yMask & yVal) > 0) continue;

                        boolean[][] set = getPatternSetArray(xMask, yMask, xVal, yVal);
//                        System.out.println("xMask: " + Bits.Str(xMask, 2) + " xVal: " + Bits.Str(xVal, 2) + " yMask: " + Bits.Str(yMask, 2) + " yVal: " + Bits.Str(yVal, 2));
//                        printBitSet(set, this.width, this.height);

                        boolean match = true;
                        int newCount = 0;
                        for (int x = 0; x < this.width; x++) {
                            for (int y = 0; y < this.height; y++) {
                                if (set[x][y]) {
                                    match &= bitMask[x][y] == 'O' || bitMask[x][y] == 'o' || bitMask[x][y] == 0;
                                    newCount += bitMask[x][y] == 'O'?1:0;
                                }
                            }
                        }
//                        System.out.println("O:" + (match ? "T " : "F ") + newCount);

                        if(match && newCount>0){
                            nextPattern.add(new Pattern(xMask, yMask, xVal, yVal, newCount, set));
                        }

                    }
                }
            }
            nextPattern.sort(Comparator.comparingInt(pattern -> -pattern.count));

            if(!nextPattern.isEmpty()){
                Pattern pattern = nextPattern.get(0);
                patterns.add(pattern);
                count -= pattern.count;
                for (int x = 0; x < this.width; x++) {
                    for (int y = 0; y < this.height; y++) {
                        if (pattern.set[x][y] && bitMask[x][y] != 0) {
                            bitMask[x][y] = 'o';
                        }
                    }
                }
            }

//            printBitCharMask(bitMask, this.width, this.height);

        }
        if (count!=0) throw new IllegalArgumentException("Cannot find patterns to match bank");
        return patterns;
    }

    private boolean[][] getPatternSetArray(int xMask, int yMask, int xVal, int yVal) {
        boolean[][] set = new boolean[this.width][this.height];
        for (int x = 0; x < this.width; x++) {
            boolean xSet = true;
            for (int i = 0; i < Bits.log2nlz(this.width); i++) {
                if (!Bits.isOne(xMask, i) && Bits.isOne(xVal, i) != Bits.isOne(x, i)) {
                    xSet = false;
                    break;
                }
            }
            if (xSet) {
                for (int y = 0; y < this.height; y++) {
                    boolean ySet = true;
                    for (int i = 0; i < Bits.log2nlz(this.height); i++) {
                        if (!Bits.isOne(yMask, i) && Bits.isOne(yVal, i) != Bits.isOne(y, i)) {
                            ySet = false;
                            break;
                        }
                    }
                    set[x][y] = ySet;
                }
            }
        }
        return set;
    }


    public static final class Builder extends Scamp5ConfigBuilder<Scamp5SuperPixelConfig> {
        public boolean useMovbx;
        public boolean useAdd;
        public boolean useAddSelf;
        public boolean useSub;
        public boolean useDiv;
        public boolean useRes;
        public String maskReg;
        public String maskedReg;
        public String northReg;
        public String eastReg;
        public String southReg;
        public String westReg;
        public int width;
        public int height;
        public int banks;
        public int[][][] bitOrder;
        List<String> scratchRegisters;
        String selectReg;

        public Builder() {
        }

        public Builder(Scamp5SuperPixelConfig src) {
            this.useMovbx = src.useMovbx;
            this.useAdd = src.useAdd;
            this.useAddSelf = src.useAddSelf;
            this.useSub = src.useSub;
            this.useDiv = src.useDiv;
            this.useRes = src.useRes;
            this.maskReg = src.maskReg;
            this.maskedReg = src.maskedReg;
            this.northReg = src.northReg;
            this.eastReg = src.eastReg;
            this.southReg = src.southReg;
            this.westReg = src.westReg;
            this.width = src.width;
            this.height = src.height;
            this.banks = src.banks;
            this.bitOrder = src.bitOrder;
            this.scratchRegisters = src.scratchRegisters;
            this.outputFormatter = src.outputFormatter;
        }

        public Builder useMovbx(boolean useMovbx) {
            this.useMovbx = useMovbx;
            return this;
        }

        public Builder useAdd(boolean useAdd) {
            this.useAdd = useAdd;
            return this;
        }

        public Builder useAddSelf(boolean useAddSelf) {
            this.useAddSelf = useAddSelf;
            return this;
        }

        public Builder useSub(boolean useSub) {
            this.useSub = useSub;
            return this;
        }

        public Builder useDiv(boolean useDiv) {
            this.useDiv = useDiv;
            return this;
        }

        public Builder useRes(boolean useRes) {
            this.useRes = useRes;
            return this;
        }

        public Builder scratchRegisters(List<String> scratchRegisters) {
            this.scratchRegisters = scratchRegisters;
            return this;
        }

        public Builder selectReg(String selectReg) {
            this.selectReg = selectReg;
            return this;
        }

        public Builder maskReg(String maskReg) {
            this.maskReg = maskReg;
            return this;
        }

        public Builder maskedReg(String maskedReg) {
            this.maskedReg = maskedReg;
            return this;
        }

        public Builder northReg(String northReg) {
            this.northReg = northReg;
            return this;
        }

        public Builder eastReg(String eastReg) {
            this.eastReg = eastReg;
            return this;
        }

        public Builder southReg(String southReg) {
            this.southReg = southReg;
            return this;
        }

        public Builder westReg(String westReg) {
            this.westReg = westReg;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder banks(int banks) {
            this.banks = banks;
            return this;
        }

        public Builder bitOrder(int[][][] bitOrder) {
            this.bitOrder = bitOrder;
            return this;
        }

        public Scamp5SuperPixelConfig build() {
            return new Scamp5SuperPixelConfig(useMovbx, useAdd, useAddSelf, useSub, useDiv, useRes, scratchRegisters, selectReg, maskReg, maskedReg, northReg, eastReg, southReg, westReg, width, height, banks, bitOrder, outputFormatter);
        }
    }
}
